package com.example.ui.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.TigerStrings
import com.example.data.firestore.StoreOrder
import com.example.data.firestore.StoreProduct
import com.example.data.firestore.UserStore
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private data class BankingAppTarget(
    val name: String,
    val packageName: String,
    val webFallbackUrl: String
)

private val REGIONAL_BANKING_APPS = listOf(
    BankingAppTarget("Al Rajhi Bank", "com.alrajhibank.mobile", "https://www.alrajhibank.com.sa"),
    BankingAppTarget("SNB AlAhli", "com.alahli.mobile.android", "https://www.alahli.com"),
    BankingAppTarget("STC Pay", "sa.com.stcpay", "https://stcpay.com.sa"),
    BankingAppTarget("InstaPay Egypt", "com.egyptianbanks.instapay", "https://www.instapay.eg"),
    BankingAppTarget("NBE Mobile", "com.nbe.alahlynet", "https://www.nbe.com.eg"),
    BankingAppTarget("CIB Mobile", "com.cib.mobile", "https://www.cibeg.com"),
    BankingAppTarget("Revolut", "com.revolut.revolut", "https://www.revolut.com"),
    BankingAppTarget("Wise", "com.transferwise.android", "https://wise.com")
)

@Composable
fun StoreScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings
) {
    val storesState by viewModel.storesState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val buyerOrdersState by viewModel.buyerOrdersState.collectAsStateWithLifecycle()
    val sellerOrdersState by viewModel.sellerOrdersState.collectAsStateWithLifecycle()

    val myUid = Firebase.auth.currentUser?.uid.orEmpty()
    val stores = (storesState as? DataState.Success)?.data.orEmpty()
    val products = (productsState as? DataState.Success)?.data.orEmpty()
    val buyerOrders = (buyerOrdersState as? DataState.Success)?.data.orEmpty()
    val sellerOrders = (sellerOrdersState as? DataState.Success)?.data.orEmpty()
    val myStore = stores.firstOrNull { it.ownerId == myUid }

    var selectedTab by remember { mutableIntStateOf(0) }
    var checkoutProduct by remember { mutableStateOf<StoreProduct?>(null) }
    var refundingOrder by remember { mutableStateOf<StoreOrder?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.storeMarketplace) }, modifier = Modifier.testTag("store_tab_market"))
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.storeMyStore) }, modifier = Modifier.testTag("store_tab_mystore"))
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.storeOrders) }, modifier = Modifier.testTag("store_tab_orders"))
        }

        when (selectedTab) {
            0 -> MarketplaceTab(
                products = products,
                stores = stores,
                strings = strings,
                onBuyClick = { checkoutProduct = it }
            )
            1 -> MyStoreCreatorTab(
                myStore = myStore,
                myProducts = products.filter { it.ownerId == myUid },
                strings = strings,
                onCreateStore = { name, desc, bank, holder, iban, acct, notes ->
                    viewModel.createStore(name, desc, bank, holder, iban, acct, notes)
                },
                onAddProduct = { store, title, desc, price, delivery, stock ->
                    viewModel.addProduct(store.id, store.storeName, title, desc, price, delivery, stock)
                }
            )
            2 -> OrdersReviewTab(
                buyerOrders = buyerOrders,
                sellerOrders = sellerOrders,
                strings = strings,
                onApproveOrder = { viewModel.sellerReviewOrder(it.id, approve = true) },
                onStartPartialRefund = { refundingOrder = it }
            )
        }
    }

    checkoutProduct?.let { product ->
        val store = stores.firstOrNull { it.id == product.storeId }
        CheckoutDialog(
            product = product,
            store = store,
            strings = strings,
            onDismiss = { checkoutProduct = null },
            onPlaceOrder = { qty, method, receiptName, bytes ->
                viewModel.placeOrder(product, qty, method, receiptName, bytes)
                checkoutProduct = null
            },
            onCopied = { viewModel.showMessage(it) }
        )
    }

    refundingOrder?.let { order ->
        PartialRefundDialog(
            order = order,
            strings = strings,
            onDismiss = { refundingOrder = null },
            onConfirmRefund = { partialAmt, note ->
                viewModel.sellerReviewOrder(
                    orderId = order.id,
                    approve = false,
                    refundPartialAmount = partialAmt,
                    refundNote = note
                )
                refundingOrder = null
            }
        )
    }
}

@Composable
private fun MarketplaceTab(
    products: List<StoreProduct>,
    stores: List<UserStore>,
    strings: TigerStrings,
    onBuyClick: (StoreProduct) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth().height(148.dp), shape = RoundedCornerShape(22.dp)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_store_banner_1795365390787),
                        contentDescription = "Tiger Store Showcase",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(Color(0xD9090D16), Color(0x66090D16)))
                        )
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(
                            text = strings.pick("متجر تايجر متعدد التجار", "TIGER Multi-Vendor Store"),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.pick(
                                "دفع عند الاستلام (COD) أو تحويل بنكي مباشر مع رمز QR وحماية الاسترداد الجزئي",
                                "COD & Direct Bank Transfer with QR, Banking App Intents & Partial Refund Protection"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TigerGold
                        )
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                        Text(strings.pick("لا توجد منتجات بعد", "No products listed yet"), fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.pick("انتقل إلى تبويب «متجري» لإنشاء متجرك ببيانات التحويل البنكي وإضافة منتجاتك.", "Open 'My Store' tab to register your bank details and publish your first product."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(products, key = { it.id }) { product ->
                val store = stores.firstOrNull { it.id == product.storeId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${product.storeName} • ${store?.bankName ?: "Bank Transfer"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = strings.formatCurrency(product.price),
                                style = MaterialTheme.typography.titleLarge,
                                color = TigerGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!product.description.isNullOrBlank()) {
                            Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${strings.deliveryFeeLabel}: ${strings.formatCurrency(product.deliveryFee)} • ${strings.stockLabel}: ${strings.formatNumber(product.stock)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onBuyClick(product) },
                                modifier = Modifier.testTag("buy_product_btn_${product.id}")
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.pick("شراء الآن", "Checkout"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyStoreCreatorTab(
    myStore: UserStore?,
    myProducts: List<StoreProduct>,
    strings: TigerStrings,
    onCreateStore: (String, String, String, String, String, String, String) -> Unit,
    onAddProduct: (UserStore, String, String, Double, Double, Long) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var storeDesc by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("Al Rajhi Bank") }
    var accountHolder by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("SA4420000001234567891234") }
    var accountNumber by remember { mutableStateOf("1234567891234") }
    var bankNotes by remember { mutableStateOf("Mention order ID in transfer remarks") }

    var showAddProduct by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (myStore == null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.pick("إنشاء متجرك وبيانات التحويل البنكي", "Create Your Store & Bank Transfer Details"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text(strings.pick("اسم المتجر", "Store Name")) }, modifier = Modifier.fillMaxWidth().testTag("store_name_input"))
                        OutlinedTextField(value = storeDesc, onValueChange = { storeDesc = it }, label = { Text(strings.pick("وصف المتجر", "Store Description")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text(strings.pick("اسم البنك", "Bank Name")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = accountHolder, onValueChange = { accountHolder = it }, label = { Text(strings.pick("اسم صاحب الحساب", "Account Holder Name")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = iban, onValueChange = { iban = it }, label = { Text("IBAN") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text(strings.pick("رقم الحساب البنكي", "Account Number")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bankNotes, onValueChange = { bankNotes = it }, label = { Text(strings.pick("ملاحظات التحويل البنكي", "Bank Transfer Notes")) }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (storeName.trim().length >= 2 && iban.trim().length >= 8) {
                                    onCreateStore(
                                        storeName,
                                        storeDesc,
                                        bankName,
                                        accountHolder.ifBlank { storeName },
                                        iban,
                                        accountNumber,
                                        bankNotes
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_store_submit_btn")
                        ) {
                            Text(strings.pick("تفعيل متجري الآن", "Launch My Store"))
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(myStore.storeName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${myStore.bankName} • ${myStore.accountHolder}", style = MaterialTheme.typography.bodyMedium)
                        Text("IBAN: ${myStore.iban}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily))
                        Text("Acct: ${myStore.accountNumber}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily))
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { showAddProduct = true },
                            modifier = Modifier.fillMaxWidth().testTag("open_add_product_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.pick("إضافة منتج (السعر + المخزون + رسوم التوصيل)", "Add Product (Price, Stock, Delivery Fee)"))
                        }
                    }
                }
            }
            items(myProducts, key = { it.id }) { prod ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(prod.title, fontWeight = FontWeight.Bold)
                            Text("${strings.deliveryFeeLabel}: ${strings.formatCurrency(prod.deliveryFee)} • ${strings.stockLabel}: ${prod.stock}", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(strings.formatCurrency(prod.price), color = TigerGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddProduct && myStore != null) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var priceStr by remember { mutableStateOf("199") }
        var deliveryStr by remember { mutableStateOf("20") }
        var stockStr by remember { mutableStateOf("15") }

        AlertDialog(
            onDismissRequest = { showAddProduct = false },
            title = { Text(strings.pick("إضافة منتج جديد", "Add Store Product")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(strings.pick("اسم المنتج", "Product Title")) })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(strings.pick("الوصف", "Description")) })
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text(strings.pick("السعر", "Price")) })
                    OutlinedTextField(value = deliveryStr, onValueChange = { deliveryStr = it }, label = { Text(strings.deliveryFeeLabel) })
                    OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text(strings.stockLabel) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val delivery = deliveryStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toLongOrNull() ?: 1L
                    if (title.trim().length >= 2) {
                        onAddProduct(myStore, title, desc, price, delivery, stock)
                        showAddProduct = false
                    }
                }) { Text(strings.pick("حفظ المنتج", "Save Product")) }
            },
            dismissButton = {
                TextButton(onClick = { showAddProduct = false }) { Text(strings.pick("إلغاء", "Cancel")) }
            }
        )
    }
}

@Composable
private fun OrdersReviewTab(
    buyerOrders: List<StoreOrder>,
    sellerOrders: List<StoreOrder>,
    strings: TigerStrings,
    onApproveOrder: (StoreOrder) -> Unit,
    onStartPartialRefund: (StoreOrder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.pick("طلبات متجري للمراجعة (حماية التحويل الجزئي)", "Incoming Seller Orders (Receipt & Refund Review)"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (sellerOrders.isEmpty()) {
            item {
                Text(strings.pick("لا توجد طلبات واردة لمتجرك بعد.", "No incoming seller orders yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(sellerOrders, key = { "s_${it.id}" }) { order ->
                OrderCard(
                    order = order,
                    isSellerView = true,
                    strings = strings,
                    onApprove = { onApproveOrder(order) },
                    onRefund = { onStartPartialRefund(order) }
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.pick("مشترياتي وطلباتي", "My Buyer Orders"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (buyerOrders.isEmpty()) {
            item {
                Text(strings.pick("لم تقم بأي عمليات شراء بعد.", "You have not placed any orders yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(buyerOrders, key = { "b_${it.id}" }) { order ->
                OrderCard(
                    order = order,
                    isSellerView = false,
                    strings = strings,
                    onApprove = {},
                    onRefund = {}
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: StoreOrder,
    isSellerView: Boolean,
    strings: TigerStrings,
    onApprove: () -> Unit,
    onRefund: () -> Unit
) {
    val badgeColor = when (order.status) {
        "approved" -> EmeraldSuccess
        "cancelled_refunded" -> CrimsonDanger
        "receipt_uploaded" -> TigerGold
        else -> TigerOrange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${order.productTitle} x${order.quantity}", fontWeight = FontWeight.Bold)
                Surface(color = badgeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = order.status,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "${order.paymentMethod} • Total: ${strings.formatCurrency(order.totalAmount)} (Delivery: ${strings.formatCurrency(order.deliveryFee)})",
                style = MaterialTheme.typography.bodySmall
            )
            if (!order.receiptFileName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TigerGold, modifier = Modifier.size(16.dp))
                    Text("Receipt: ${order.receiptFileName}", style = MaterialTheme.typography.labelSmall, color = TigerGold)
                }
            }
            if (order.status == "cancelled_refunded") {
                Surface(color = CrimsonDanger.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Refunded Partial Amount: ${strings.formatCurrency(order.refundedPartialAmount)} • ${order.refundNote ?: ""}",
                        color = CrimsonDanger,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            if (isSellerView && order.status !in listOf("approved", "cancelled_refunded")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.approveOrder, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onRefund,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancelRefundPartial, color = CrimsonDanger, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutDialog(
    product: StoreProduct,
    store: UserStore?,
    strings: TigerStrings,
    onDismiss: () -> Unit,
    onPlaceOrder: (Long, String, String?, ByteArray?) -> Unit,
    onCopied: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var paymentMethod by remember { mutableStateOf("BANK_TRANSFER") }
    var quantity by remember { mutableIntStateOf(1) }
    var receiptName by remember { mutableStateOf<String?>(null) }
    var receiptBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showBankSelector by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptName = uri.lastPathSegment ?: "transfer_receipt.jpg"
            receiptBytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
        }
    }

    val subtotal = product.price * quantity
    val delivery = product.deliveryFee * quantity
    val total = subtotal + delivery
    val ibanValue = store?.iban ?: "SA4420000001234567891234"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.pick("إتمام الطلب والدفع", "Store Checkout")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${product.title} • ${strings.formatCurrency(product.price)}", fontWeight = FontWeight.Bold)
                Text(
                    text = "${strings.deliveryFeeLabel}: ${strings.formatCurrency(delivery)} • Total: ${strings.formatCurrency(total)}",
                    color = TigerGold,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paymentMethod == "BANK_TRANSFER",
                        onClick = { paymentMethod = "BANK_TRANSFER" },
                        label = { Text(strings.bankTransferPayment) }
                    )
                    FilterChip(
                        selected = paymentMethod == "COD",
                        onClick = { paymentMethod = "COD" },
                        label = { Text(strings.codPayment) }
                    )
                }

                if (paymentMethod == "BANK_TRANSFER") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(store?.bankName ?: "Tiger Escrow Bank", fontWeight = FontWeight.Bold)
                            Text(store?.accountHolder ?: product.storeName, style = MaterialTheme.typography.bodySmall)
                            Text(ibanValue, style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily))

                            // Deterministic QR Code Canvas for IBAN Transfer
                            DeterministicQrCanvas(
                                payload = "IBAN:$ibanValue|AMT:$total|STORE:${product.storeName}",
                                modifier = Modifier.size(136.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("$ibanValue (${store?.accountNumber ?: ""})"))
                                        onCopied(strings.pick("تم نسخ IBAN ورقم الحساب إلى الحافظة", "IBAN & account copied to clipboard"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.copyIban, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { showBankSelector = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.openBankApp, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Button(
                                onClick = {
                                    receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.fillMaxWidth().testTag("upload_receipt_btn")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(receiptName ?: strings.uploadReceipt)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPlaceOrder(quantity.toLong(), paymentMethod, receiptName, receiptBytes) },
                modifier = Modifier.testTag("confirm_checkout_btn")
            ) {
                Text(strings.pick("تأكيد الطلب", "Place Order"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.pick("إلغاء", "Cancel")) }
        }
    )

    if (showBankSelector) {
        AlertDialog(
            onDismissRequest = { showBankSelector = false },
            title = { Text(strings.pick("اختر تطبيق البنك للتحويل", "Launch Banking App")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    REGIONAL_BANKING_APPS.forEach { bank ->
                        val installed = remember(bank.packageName) { isPackageInstalled(context, bank.packageName) }
                        OutlinedButton(
                            onClick = {
                                launchBankingAppOrWeb(context, bank)
                                showBankSelector = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${bank.name} ${if (installed) "• Installed" else "• Web Portal"}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBankSelector = false }) { Text(strings.pick("إغلاق", "Close")) }
            }
        )
    }
}

@Composable
private fun PartialRefundDialog(
    order: StoreOrder,
    strings: TigerStrings,
    onDismiss: () -> Unit,
    onConfirmRefund: (Double, String) -> Unit
) {
    var partialAmount by remember { mutableStateOf((order.totalAmount / 2.0).toString()) }
    var refundNote by remember { mutableStateOf("Partial transfer received; refunded back to buyer IBAN") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.cancelRefundPartial) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.pick(
                        "حماية التحويل غير المكتمل: أدخل المبلغ الجزئي المسترد للمشتري لتوثيق حالة (cancelled_refunded).",
                        "Partial Transfer Protection: Enter the partial amount returned to the buyer to record `cancelled_refunded`."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = partialAmount,
                    onValueChange = { partialAmount = it },
                    label = { Text(strings.pick("المبلغ الجزئي المسترد", "Partial Amount Refunded")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = refundNote,
                    onValueChange = { refundNote = it },
                    label = { Text(strings.pick("ملاحظة الاسترداد", "Refund Reference Note")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = partialAmount.toDoubleOrNull() ?: 0.0
                    onConfirmRefund(amt, refundNote)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
            ) {
                Text(strings.pick("تأكيد الإلغاء والاسترداد", "Record Refund"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.pick("رجوع", "Back")) }
        }
    )
}

@Composable
private fun DeterministicQrCanvas(payload: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val grid = 21
        val cell = size.minDimension / grid
        fun drawFinder(startX: Int, startY: Int) {
            for (y in 0..6) {
                for (x in 0..6) {
                    val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                    val isCore = x in 2..4 && y in 2..4
                    if (isBorder || isCore) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset((startX + x) * cell, (startY + y) * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(grid - 7, 0)
        drawFinder(0, grid - 7)

        var hash = payload.hashCode()
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                val inTopLeft = x < 8 && y < 8
                val inTopRight = x >= grid - 8 && y < 8
                val inBottomLeft = x < 8 && y >= grid - 8
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    hash = hash * 31 + (x * 17 + y * 23)
                    if ((hash and 1) == 0) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
        }
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return runCatching {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    }.getOrDefault(false)
}

private fun launchBankingAppOrWeb(context: Context, bank: BankingAppTarget) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(bank.packageName)
    if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(launchIntent) }
    } else {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bank.webFallbackUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(webIntent) }
    }
}
