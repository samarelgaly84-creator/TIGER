const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("stores").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("orders").where("buyerId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get());
});

test("User profile: owner can create/update own profile but cannot escalate role or access other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Tiger",
      username: "alice_tiger",
      email: "alice@example.com",
      role: "user",
      planTier: "FREE",
      xp: 0,
      coins: 100,
      monthlyAiUsed: 0,
      monthlyAiLimit: 10000,
      cycleNumber: 1,
      celebrationSeenCycle: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigned admin role on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Escalator",
      username: "bob_esc",
      role: "admin",
      planTier: "PRO",
      xp: 0,
      coins: 0,
      monthlyAiUsed: 0,
      monthlyAiLimit: 10000000,
      cycleNumber: 1,
      celebrationSeenCycle: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject role escalation on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Allow valid plan & quota update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "PLUS",
      monthlyAiLimit: 100000,
      monthlyAiUsed: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // PII Isolation: Bob cannot read Alice's user doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Vault, AI Messages, and Game Scores subcollections isolate per owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vaultItems").doc("doc_1").set({
      userId: ALICE_UID,
      name: "Passport_Scan.pdf",
      folder: "Private Vault",
      mimeType: "application/pdf",
      sizeBytes: 245000,
      vaultLocked: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("vaultItems").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vaultItems").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("aiMessages").doc("msg_1").set({
      userId: ALICE_UID,
      senderRole: "user",
      text: "Summarize my Q4 roadmap",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gameScores").doc("score_1").set({
      userId: ALICE_UID,
      playerName: "Alice Tiger",
      gameId: "tiger_tapper",
      score: 145,
      xpEarned: 75,
      coinsEarned: 30,
      createdAt: now,
    })
  );
});

test("Social Posts: public query aligned with rules; ghost field & future timestamp rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      userId: ALICE_UID,
      authorName: "Alice Tiger",
      category: "Tech",
      content: "Launching Tiger Connect Android!",
      mediaType: "none",
      likesCount: 0,
      commentsCount: 0,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Aligned public query succeeds
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  // Unfiltered list query fails
  await assertFails(bobDb.collection("posts").get());

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("posts").doc("post_ghost").set({
      userId: ALICE_UID,
      authorName: "Alice",
      category: "Tech",
      content: "Ghost",
      mediaType: "none",
      likesCount: 0,
      commentsCount: 0,
      visibility: "public",
      isVerified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set({
      userId: ALICE_UID,
      authorName: "Alice",
      category: "Tech",
      content: "Future",
      mediaType: "none",
      likesCount: 0,
      commentsCount: 0,
      visibility: "public",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Tiger Store & Orders: Bank Transfer receipt upload and Seller partial refund terminal lock", async () => {
  const sellerDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const buyerDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    sellerDb.collection("stores").doc("store_1").set({
      ownerId: ALICE_UID,
      ownerName: "Alice Electronics",
      storeName: "Tiger Tech Boutique",
      bankName: "Al Rajhi Bank",
      accountHolder: "Alice Trading LLC",
      iban: "SA4420000001234567891234",
      accountNumber: "1234567891234",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    sellerDb.collection("products").doc("prod_1").set({
      storeId: "store_1",
      ownerId: ALICE_UID,
      storeName: "Tiger Tech Boutique",
      title: "Tiger Pro Earbuds",
      price: 299.0,
      deliveryFee: 20.0,
      stock: 15,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    buyerDb.collection("orders").doc("order_1").set({
      buyerId: BOB_UID,
      buyerName: "Bob Buyer",
      sellerId: ALICE_UID,
      storeId: "store_1",
      storeName: "Tiger Tech Boutique",
      productId: "prod_1",
      productTitle: "Tiger Pro Earbuds",
      quantity: 1,
      subtotal: 299.0,
      deliveryFee: 20.0,
      totalAmount: 319.0,
      paymentMethod: "BANK_TRANSFER",
      status: "pending",
      refundedPartialAmount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Buyer uploads transfer receipt
  await assertSucceeds(
    buyerDb.collection("orders").doc("order_1").update({
      receiptFileName: "bank_transfer_receipt_01.jpg",
      status: "receipt_uploaded",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Seller issues partial refund record (cancelled_refunded)
  await assertSucceeds(
    sellerDb.collection("orders").doc("order_1").update({
      status: "cancelled_refunded",
      refundedPartialAmount: 150.0,
      refundNote: "Incomplete transfer returned to buyer IBAN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: cannot modify after cancelled_refunded
  await assertFails(
    sellerDb.collection("orders").doc("order_1").update({
      status: "approved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Encrypted Chats: strictly restricted to memberUids", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("charlie_999").firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").set({
      title: "Alice & Bob E2EE",
      groupChat: false,
      memberUids: [ALICE_UID, BOB_UID],
      lastMessagePreview: "Encrypted message",
      unreadCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").collection("messages").doc("m_1").set({
      chatId: "chat_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      cipherText: "U2FsdGVkX19==",
      ivBase64: "MTIzNDU2Nzg5MDEy",
      plainPreview: "Hello encrypted!",
      messageType: "text",
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get());
  await assertFails(outsiderDb.collection("chats").doc("chat_1").get());
  await assertFails(outsiderDb.collection("chats").doc("chat_1").collection("messages").doc("m_1").get());
});

test("Admin email spoofing rejected when email_verified is false", async () => {
  const spoofDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "samarelgaly84@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());

  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "samarelgaly84@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(verifiedAdminDb.collection("users").get());
});
