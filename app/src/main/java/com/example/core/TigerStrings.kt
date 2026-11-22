package com.example.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TigerStrings(
    val isArabic: Boolean
) {
    fun pick(ar: String, en: String): String = if (isArabic) ar else en

    val appTitle get() = pick("تايجر كونكت", "TIGER CONNECT")
    val tagline get() = pick("عالمك الاجتماعي والذكي المشفّر بالكامل", "Your Unified Social, Encrypted & AI Super-App")

    // Bottom / Rail Navigation
    val navFeed get() = pick("الرئيسية", "Social")
    val navChat get() = pick("المحادثات", "Messenger")
    val navAi get() = pick("تايجر AI", "TIGER AI")
    val navStore get() = pick("المتجر", "Store")
    val navHub get() = pick("المركز", "Hub")

    // Auth
    val signInWithGoogle get() = pick("تسجيل الدخول عبر Google", "Sign in with Google")
    val emailSignIn get() = pick("دخول بالبريد", "Email Sign In")
    val createAccount get() = pick("إنشاء حساب", "Create Account")
    val emailLabel get() = pick("البريد الإلكتروني", "Email Address")
    val passwordLabel get() = pick("كلمة المرور", "Password")
    val displayNameLabel get() = pick("الاسم الكامل", "Full Name")
    val signOut get() = pick("تسجيل الخروج", "Sign Out")
    val backendSyncLabel get() = pick("متصل بـ Firebase السحابي + واجهة Node.js API", "Synced with Cloud Firebase + Node.js API")

    // Social
    val tabFeed get() = pick("المنشورات", "Feed")
    val tabReels get() = pick("ريلز", "Reels")
    val tabLive get() = pick("بث مباشر", "Live")
    val storiesTitle get() = pick("قصص 24 ساعة", "24h Stories")
    val addStory get() = pick("قصتك", "Add Story")
    val newPost get() = pick("منشور جديد", "New Post")
    val shareToFeed get() = pick("مشاركة في المنشورات", "Share to Feed")
    val shareToStory get() = pick("مشاركة في القصة", "Share to Story")
    val shareToChat get() = pick("إرسال في محادثة مشفّرة", "Share to Encrypted Chat")
    val shareToAi get() = pick("تحليل عبر تايجر AI", "Analyze with TIGER AI")

    // Messenger & Calls
    val e2eeBadge get() = pick("تشفير طرف لطرف ECDH P-256 + AES-GCM", "E2EE Active • ECDH P-256 + AES-GCM")
    val testIncomingCall get() = pick("محاكاة مكالمة على شاشة القفل", "Test Lock-Screen Call")
    val startVoiceCall get() = pick("مكالمة صوتية", "Voice Call")
    val startVideoCall get() = pick("مكالمة فيديو", "Video Call")
    val addPersonToCall get() = pick("إضافة شخص للمكالمة", "Add Person to Call")
    val newGroupChat get() = pick("مجموعة مشفّرة جديدة", "New Encrypted Group")

    // TIGER AI
    val aiStudioTitle get() = pick("استوديو تايجر AI", "TIGER AI Studio")
    val aiQuotaTitle get() = pick("حصة الرسائل الشهرية", "Monthly AI Quota")
    val aiAnalyzeImage get() = pick("تحليل صورة / ملف", "Analyze Photo / File")
    val aiDriveSummary get() = pick("ذكاء درايف السحابي", "Cloud Drive AI")
    val aiSettleCycle get() = pick("تسوية دورة 30 يوم والاحتفال", "Settle 30-Day Cycle")

    // Store
    val storeMarketplace get() = pick("السوق", "Marketplace")
    val storeMyStore get() = pick("متجري", "My Store")
    val storeOrders get() = pick("الطلبات", "Orders")
    val deliveryFeeLabel get() = pick("رسوم التوصيل", "Delivery Fee")
    val stockLabel get() = pick("المخزون", "Stock")
    val codPayment get() = pick("الدفع عند الاستلام (COD)", "Cash on Delivery (COD)")
    val bankTransferPayment get() = pick("تحويل بنكي أونلاين", "Online Bank Transfer")
    val copyIban get() = pick("نسخ الآيبان والحساب", "Copy IBAN & Account")
    val openBankApp get() = pick("فتح تطبيق البنك", "Open Banking App")
    val uploadReceipt get() = pick("رفع إيصال التحويل", "Upload Transfer Receipt")
    val approveOrder get() = pick("اعتماد الطلب", "Approve Order")
    val cancelRefundPartial get() = pick("إلغاء وتسجيل استرداد جزئي", "Cancel & Issue Partial Refund")

    // Hub
    val hubAvatarStudio get() = pick("استوديو الأفاتار", "Avatar Studio")
    val hubGamesArena get() = pick("ساحة 9 ألعاب مصغرة", "9 Mini-Games Arena")
    val hubDriveVault get() = pick("درايف والخزينة الخاصة", "Drive & Private Vault")
    val hubTigerMap get() = pick("خريطة تايجر المباشرة", "TIGER Live Map")
    val hubPlans get() = pick("الباقات المميزة", "Premium Plans")
    val hubAdmin get() = pick("لوحة الإدارة", "Admin Panel")
    val hubSettings get() = pick("الإعدادات والمظهر", "Theme & Security")

    fun formatNumber(value: Number): String {
        val locale = if (isArabic) Locale("ar", "EG") else Locale.US
        return NumberFormat.getNumberInstance(locale).format(value)
    }

    fun formatCurrency(amount: Double): String {
        val locale = if (isArabic) Locale("ar", "SA") else Locale.US
        val formatted = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(amount)
        return if (isArabic) "$formatted ر.س" else "$$formatted"
    }

    fun formatDate(epochMs: Long): String {
        val locale = if (isArabic) Locale("ar", "SA") else Locale.US
        val pattern = if (isArabic) "d MMM yyyy • h:mm a" else "MMM d, yyyy • h:mm a"
        return SimpleDateFormat(pattern, locale).format(Date(epochMs))
    }

    fun categoryLabel(category: String): String = when (category) {
        "General" -> pick("عام", "General")
        "Tech" -> pick("تقنية", "Tech")
        "Gaming" -> pick("ألعاب", "Gaming")
        "Education" -> pick("تعليم", "Education")
        "Commerce" -> pick("تجارة", "Commerce")
        "Sports" -> pick("رياضة", "Sports")
        "Entertainment" -> pick("ترفيه", "Entertainment")
        else -> category
    }
}
