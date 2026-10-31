package com.mulkora.app.navigation

/**
 * Uygulamadaki tüm navigasyon rotalarını tek yerde tanımlar.
 * Her rota bir sealed class veya object ile ifade edilir.
 *
 * Parametre içeren rotalar için:
 *   route = "post_detail/{postId}"
 *   fun createRoute(postId: String) = "post_detail/$postId"
 */
sealed class AppRoute(val route: String) {

    // --- Sistem ---
    data object Splash : AppRoute("splash")

    // --- Auth ---
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object UsernameSelection : AppRoute("username_selection")
    data object RoleSelection : AppRoute("role_selection")
    data object TermsAcceptance : AppRoute("terms_acceptance")
    data object EmailVerification : AppRoute("email_verification")
    data object ForgotPassword : AppRoute("forgot_password")

    // --- Onboarding ---
    data object Onboarding : AppRoute("onboarding")

    // --- Ana Sekmeler ---
    data object Feed : AppRoute("feed")
    data object Explore : AppRoute("explore")

    // --- Keşfet alt sayfaları ---
    data object ExploreCategoryPosts : AppRoute("explore_category/{category}") {
        fun createRoute(category: String) = "explore_category/$category"
        const val ARG_CATEGORY = "category"
    }
    data object ExploreCityPosts : AppRoute("explore_city/{city}") {
        fun createRoute(city: String) = "explore_city/$city"
        const val ARG_CITY = "city"
    }
    data object PostCreate : AppRoute("post_create")
    data object Notifications : AppRoute("notifications")
    data object Profile : AppRoute("profile")

    // --- Gönderi ---
    data object PostDetail : AppRoute("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
        const val ARG_POST_ID = "postId"
    }

    data object PostEdit : AppRoute("post_edit/{postId}") {
        fun createRoute(postId: String) = "post_edit/$postId"
        const val ARG_POST_ID = "postId"
    }

    data object ImagePicker : AppRoute("image_picker")
    data object ImageCrop : AppRoute("image_crop")

    // --- Yorumlar ---
    data object Comments : AppRoute("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
        const val ARG_POST_ID = "postId"
    }

    // --- Şikâyet ---
    data object ReportCreate : AppRoute("report_create/{contentType}/{contentId}") {
        fun createRoute(contentType: String, contentId: String) =
            "report_create/$contentType/$contentId"
        const val ARG_CONTENT_TYPE = "contentType"
        const val ARG_CONTENT_ID = "contentId"
    }

    // --- Profil ---
    data object UserProfile : AppRoute("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
        const val ARG_USER_ID = "userId"
    }

    data object ProfileEdit : AppRoute("profile_edit")
    data object SavedPosts : AppRoute("saved_posts")
    data object BlockedUsers : AppRoute("blocked_users")

    // --- Referanslar ---
    data object References : AppRoute("references")
    data object ReferenceCreate : AppRoute("reference_create/{targetUserId}") {
        fun createRoute(targetUserId: String) = "reference_create/$targetUserId"
        const val ARG_TARGET_USER_ID = "targetUserId"
    }
    data object ReferenceRecognition : AppRoute("reference_recognition/{referenceId}") {
        fun createRoute(referenceId: String) = "reference_recognition/$referenceId"
        const val ARG_REFERENCE_ID = "referenceId"
    }
    data object ReferenceWrite : AppRoute("reference_write/{referenceId}") {
        fun createRoute(referenceId: String) = "reference_write/$referenceId"
        const val ARG_REFERENCE_ID = "referenceId"
    }
    data object ReferenceDetail : AppRoute("reference_detail/{referenceId}") {
        fun createRoute(referenceId: String) = "reference_detail/$referenceId"
        const val ARG_REFERENCE_ID = "referenceId"
    }
    data object ReferenceResponse : AppRoute("reference_response/{referenceId}") {
        fun createRoute(referenceId: String) = "reference_response/$referenceId"
        const val ARG_REFERENCE_ID = "referenceId"
    }
    data object ReferenceAppeal : AppRoute("reference_appeal/{referenceId}") {
        fun createRoute(referenceId: String) = "reference_appeal/$referenceId"
        const val ARG_REFERENCE_ID = "referenceId"
    }

    // --- Sözleşmeler ---
    data object Contracts : AppRoute("contracts")
    data object ContractTemplates : AppRoute("contract_templates")
    data object ContractCreate : AppRoute("contract_create/{templateId}") {
        fun createRoute(templateId: String) = "contract_create/$templateId"
        const val ARG_TEMPLATE_ID = "templateId"
    }
    data object ContractDetail : AppRoute("contract_detail/{contractId}") {
        fun createRoute(contractId: String) = "contract_detail/$contractId"
        const val ARG_CONTRACT_ID = "contractId"
    }
    data object ContractPdf : AppRoute("contract_pdf/{contractId}") {
        fun createRoute(contractId: String) = "contract_pdf/$contractId"
        const val ARG_CONTRACT_ID = "contractId"
    }

    // --- Bildirimler ---
    data object NotificationDetail : AppRoute("notification_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "notification_detail/$notificationId"
        const val ARG_NOTIFICATION_ID = "notificationId"
    }

    // --- Ayarlar ---
    data object Settings : AppRoute("settings")
    data object AccountSettings : AppRoute("account_settings")
    data object ChangePassword : AppRoute("change_password")
    data object PrivacySettings : AppRoute("privacy_settings")
    data object NotificationSettings : AppRoute("notification_settings")
    data object ThemeSettings : AppRoute("theme_settings")
    data object DataDownload : AppRoute("data_download")
    data object DeleteAccount : AppRoute("delete_account")

    // --- Yasal ---
    data object TermsOfService : AppRoute("terms_of_service")
    data object PrivacyPolicy : AppRoute("privacy_policy")
    data object KvkkText : AppRoute("kvkk_text")
    data object CommunityGuidelines : AppRoute("community_guidelines")
    data object About : AppRoute("about")
    data object HelpSupport : AppRoute("help_support")
    data object Faq : AppRoute("faq")

    // --- Satın Alma ---
    data object RemoveAds : AppRoute("remove_ads")
    data object PurchaseConfirmation : AppRoute("purchase_confirmation")
}