package com.mulkora.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mulkora.feature.billing.presentation.PurchaseConfirmationScreen
import com.mulkora.feature.billing.presentation.RemoveAdsScreen
import com.mulkora.feature.auth.presentation.emailverification.EmailVerificationScreen
import com.mulkora.feature.onboarding.presentation.OnboardingScreen
import com.mulkora.feature.auth.presentation.forgotpassword.ForgotPasswordScreen
import com.mulkora.feature.auth.presentation.login.LoginScreen
import com.mulkora.feature.auth.presentation.register.RegisterScreen
import com.mulkora.feature.auth.presentation.roleselection.RoleSelectionScreen
import com.mulkora.feature.auth.presentation.splash.SplashScreen
import com.mulkora.feature.auth.presentation.termsacceptance.TermsAcceptanceScreen
import com.mulkora.feature.auth.presentation.usernameselection.UsernameSelectionScreen
import com.mulkora.feature.comments.presentation.CommentsScreen
import com.mulkora.feature.feed.presentation.feed.FeedScreen
import com.mulkora.feature.postdetail.presentation.PostDetailScreen
import com.mulkora.feature.reports.presentation.ReportScreen
import com.mulkora.feature.feed.presentation.postedit.PostEditScreen
import com.mulkora.feature.reports.domain.model.ReportTargetType
import com.mulkora.feature.notifications.presentation.NotificationsScreen
import com.mulkora.feature.notifications.presentation.NotificationSettingsScreen
import com.mulkora.feature.postcreate.presentation.CreatePostScreen
import com.mulkora.feature.profile.presentation.edit.ProfileEditScreen
import com.mulkora.feature.profile.presentation.profile.ProfileScreen
import com.mulkora.feature.reference.presentation.create.ReferenceCreateScreen
import com.mulkora.feature.reference.presentation.detail.ReferenceDetailScreen
import com.mulkora.core.common.model.PostCategory
import com.mulkora.feature.contracts.presentation.create.ContractCreateScreen
import com.mulkora.feature.contracts.presentation.detail.ContractDetailScreen
import com.mulkora.feature.contracts.presentation.list.ContractListScreen
import com.mulkora.feature.explore.presentation.CategoryPostsScreen
import com.mulkora.feature.explore.presentation.CityPostsScreen
import com.mulkora.feature.explore.presentation.ExploreScreen
import com.mulkora.feature.reference.presentation.list.ReferenceListScreen
import com.mulkora.feature.settings.presentation.SettingsScreen
import com.mulkora.feature.settings.presentation.changepassword.ChangePasswordScreen
import com.mulkora.feature.settings.presentation.deleteaccount.DeleteAccountScreen
import com.mulkora.feature.settings.presentation.legal.LegalTextScreen
import com.mulkora.feature.settings.presentation.legal.TERMS_OF_SERVICE_TEXT
import com.mulkora.feature.settings.presentation.legal.PRIVACY_POLICY_TEXT
import com.mulkora.feature.settings.presentation.legal.KVKK_TEXT
import com.mulkora.feature.settings.presentation.legal.COMMUNITY_GUIDELINES_TEXT
import com.mulkora.feature.feed.presentation.savedposts.SavedPostsScreen
import com.mulkora.feature.profile.presentation.blockedusers.BlockedUsersScreen
import com.mulkora.feature.reference.presentation.recognition.ReferenceRecognitionScreen
import com.mulkora.feature.reference.presentation.write.ReferenceWriteScreen
import com.mulkora.feature.reference.presentation.dispute.ReferenceDisputeScreen
import com.mulkora.feature.reference.presentation.response.ReferenceResponseScreen
import com.mulkora.feature.settings.presentation.about.AboutScreen
import com.mulkora.feature.settings.presentation.datadownload.DataDownloadScreen
import com.mulkora.feature.settings.presentation.faq.FaqScreen
import com.mulkora.feature.settings.presentation.helpsupport.HelpSupportScreen

/**
 * Bu dosya, her feature modülünden gelen Composable ekranları
 * ana NavHost'a bağlar. Her graf kendi route'ları için composable
 * tanımları içerir.
 *
 * docs/09-navigation-map.md: Navigasyon haritasına uygun rota yapısı.
 */

// --- AUTH ---
fun NavGraphBuilder.authGraph(navController: NavController) {
    composable(AppRoute.Splash.route) {
        SplashScreen(
            onNavigateToFeed = {
                navController.navigate(AppRoute.Feed.route) {
                    popUpTo(AppRoute.Splash.route) { inclusive = true }
                }
            },
            onNavigateToLogin = {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(AppRoute.Splash.route) { inclusive = true }
                }
            },
            onNavigateToOnboarding = {
                navController.navigate(AppRoute.Onboarding.route) {
                    popUpTo(AppRoute.Splash.route) { inclusive = true }
                }
            },
        )
    }
    composable(AppRoute.Login.route) {
        LoginScreen(
            onNavigateToHome = {
                navController.navigate(AppRoute.Feed.route) {
                    popUpTo(AppRoute.Login.route) { inclusive = true }
                }
            },
            onNavigateToRegister = {
                navController.navigate(AppRoute.Register.route)
            },
            onNavigateToForgotPassword = {
                navController.navigate(AppRoute.ForgotPassword.route)
            },
            onNavigateToEmailVerification = {
                navController.navigate(AppRoute.EmailVerification.route)
            },
        )
    }
    composable(AppRoute.Register.route) {
        RegisterScreen(
            onNavigateUp = { navController.navigateUp() },
            onRegistered = {
                navController.navigate(AppRoute.UsernameSelection.route) {
                    popUpTo(AppRoute.Register.route) { inclusive = true }
                }
            },
        )
    }
    composable(AppRoute.UsernameSelection.route) {
        UsernameSelectionScreen(
            onContinue = {
                navController.navigate(AppRoute.RoleSelection.route)
            }
        )
    }
    composable(AppRoute.RoleSelection.route) {
        RoleSelectionScreen(
            onContinue = {
                navController.navigate(AppRoute.TermsAcceptance.route)
            }
        )
    }
    composable(AppRoute.TermsAcceptance.route) {
        TermsAcceptanceScreen(
            onComplete = {
                navController.navigate(AppRoute.Feed.route) {
                    popUpTo(AppRoute.Login.route) { inclusive = true }
                }
            },
            onNavigateToTerms = { navController.navigate(AppRoute.TermsOfService.route) },
            onNavigateToPrivacy = { navController.navigate(AppRoute.PrivacyPolicy.route) },
            onNavigateToKvkk = { navController.navigate(AppRoute.KvkkText.route) },
            onNavigateToCommunityRules = { navController.navigate(AppRoute.CommunityGuidelines.route) }
        )
    }
    composable(AppRoute.EmailVerification.route) {
        EmailVerificationScreen(
            onVerified = {
                navController.navigate(AppRoute.UsernameSelection.route) {
                    popUpTo(AppRoute.EmailVerification.route) { inclusive = true }
                }
            },
            onBackToLogin = {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
    composable(AppRoute.ForgotPassword.route) {
        ForgotPasswordScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
}

// --- ONBOARDING ---
fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    composable(AppRoute.Onboarding.route) {
        OnboardingScreen(
            onFinish = {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(AppRoute.Onboarding.route) { inclusive = true }
                }
            }
        )
    }
}

// --- FEED ---
fun NavGraphBuilder.feedGraph(navController: NavController) {
    composable(AppRoute.Feed.route) {
        FeedScreen(
            onNavigateToPostDetail = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId))
            },
            onNavigateToCreatePost = {
                navController.navigate(AppRoute.PostCreate.route)
            },
            onNavigateToUserProfile = { uid ->
                navController.navigate(AppRoute.UserProfile.createRoute(uid))
            },
            onNavigateToReport = { postId ->
                navController.navigate(AppRoute.ReportCreate.createRoute("post", postId))
            },
        )
    }

    composable(
        route = AppRoute.PostDetail.route,
        arguments = listOf(
            navArgument(AppRoute.PostDetail.ARG_POST_ID) { type = NavType.StringType },
        ),
    ) {
        PostDetailScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToComments = { postId ->
                navController.navigate(AppRoute.Comments.createRoute(postId))
            },
            onNavigateToReport = { postId ->
                navController.navigate(AppRoute.ReportCreate.createRoute("post", postId))
            },
        )
    }

    composable(
        route = AppRoute.PostEdit.route,
        arguments = listOf(
            navArgument(AppRoute.PostEdit.ARG_POST_ID) { type = NavType.StringType },
        ),
    ) {
        PostEditScreen(
            onNavigateUp = { navController.navigateUp() },
            onEditSubmitted = { navController.navigateUp() },
        )
    }

    composable(
        route = AppRoute.Comments.route,
        arguments = listOf(
            navArgument(AppRoute.Comments.ARG_POST_ID) { type = NavType.StringType },
        ),
    ) {
        CommentsScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
}

// --- EXPLORE ---
fun NavGraphBuilder.exploreGraph(navController: NavController) {
    composable(AppRoute.Explore.route) {
        ExploreScreen(
            onNavigateToPostDetail = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId))
            },
            onNavigateToCategoryPosts = { category ->
                navController.navigate(
                    AppRoute.ExploreCategoryPosts.createRoute(category.name)
                )
            },
            onNavigateToCityPosts = { city ->
                navController.navigate(AppRoute.ExploreCityPosts.createRoute(city))
            },
        )
    }

    composable(
        route = AppRoute.ExploreCategoryPosts.route,
        arguments = listOf(
            navArgument(AppRoute.ExploreCategoryPosts.ARG_CATEGORY) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val categoryName = backStackEntry.arguments
            ?.getString(AppRoute.ExploreCategoryPosts.ARG_CATEGORY)
            ?: PostCategory.GENERAL.name
        val category = runCatching { PostCategory.valueOf(categoryName) }
            .getOrDefault(PostCategory.GENERAL)
        CategoryPostsScreen(
            category = category,
            onNavigateUp = { navController.navigateUp() },
            onNavigateToPostDetail = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId))
            },
        )
    }

    composable(
        route = AppRoute.ExploreCityPosts.route,
        arguments = listOf(
            navArgument(AppRoute.ExploreCityPosts.ARG_CITY) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        CityPostsScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToPostDetail = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId))
            },
        )
    }
}

// --- POST CREATE ---
fun NavGraphBuilder.postCreateGraph(navController: NavController) {
    composable(AppRoute.PostCreate.route) {
        CreatePostScreen(
            onNavigateUp = { navController.navigateUp() },
            onPostCreated = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId)) {
                    popUpTo(AppRoute.PostCreate.route) { inclusive = true }
                }
            },
        )
    }
    composable(AppRoute.ImagePicker.route) { }
    composable(AppRoute.ImageCrop.route) { }
}

// --- NOTIFICATIONS ---
fun NavGraphBuilder.notificationsGraph(navController: NavController) {
    composable(AppRoute.Notifications.route) {
        NotificationsScreen(
            onNavigateToDeepLink = { deepLink ->
                navController.navigate(deepLink)
            },
        )
    }

    composable(
        route = AppRoute.NotificationDetail.route,
        arguments = listOf(
            navArgument(AppRoute.NotificationDetail.ARG_NOTIFICATION_ID) {
                type = NavType.StringType
            },
        ),
    ) { }
}

// --- PROFILE ---
fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable(AppRoute.Profile.route) {
        ProfileScreen(
            onNavigateToSettings = { navController.navigate(AppRoute.Settings.route) },
            onNavigateToEditProfile = { navController.navigate(AppRoute.ProfileEdit.route) },
            onNavigateToReferences = { navController.navigate(AppRoute.References.route) },
        )
    }
    composable(AppRoute.ProfileEdit.route) {
        ProfileEditScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.SavedPosts.route) {
        SavedPostsScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToPostDetail = { postId ->
                navController.navigate(AppRoute.PostDetail.createRoute(postId))
            },
        )
    }
    composable(AppRoute.BlockedUsers.route) {
        BlockedUsersScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }

    composable(
        route = AppRoute.UserProfile.route,
        arguments = listOf(
            navArgument(AppRoute.UserProfile.ARG_USER_ID) { type = NavType.StringType },
        ),
    ) {
        // CLAUDE.md §8: Başkasının profilinde Ayarlar ve Düzenle butonları gizlenir.
        ProfileScreen(
            onNavigateToSettings = { /* no-op: başkasının profili */ },
            onNavigateToEditProfile = { /* no-op: başkasının profili */ },
            onNavigateToReferences = { navController.navigate(AppRoute.References.route) },
        )
    }

    composable(
        route = AppRoute.ReportCreate.route,
        arguments = listOf(
            navArgument(AppRoute.ReportCreate.ARG_CONTENT_TYPE) { type = NavType.StringType },
            navArgument(AppRoute.ReportCreate.ARG_CONTENT_ID) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val contentType = backStackEntry.arguments
            ?.getString(AppRoute.ReportCreate.ARG_CONTENT_TYPE) ?: "post"
        val contentId = backStackEntry.arguments
            ?.getString(AppRoute.ReportCreate.ARG_CONTENT_ID) ?: return@composable
        val targetType = when (contentType) {
            "comment" -> ReportTargetType.COMMENT
            "user" -> ReportTargetType.USER
            "reference" -> ReportTargetType.REFERENCE
            "reference_response" -> ReportTargetType.REFERENCE_RESPONSE
            else -> ReportTargetType.POST
        }
        ReportScreen(
            targetId = contentId,
            targetType = targetType,
            onBack = { navController.navigateUp() },
            onSuccess = { navController.navigateUp() },
        )
    }
}

// --- REFERENCES ---
fun NavGraphBuilder.referencesGraph(navController: NavController) {
    composable(AppRoute.References.route) {
        ReferenceListScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToReferenceDetail = { referenceId ->
                navController.navigate(AppRoute.ReferenceDetail.createRoute(referenceId))
            },
            onNavigateToRecognition = { referenceId ->
                navController.navigate(AppRoute.ReferenceRecognition.createRoute(referenceId))
            },
        )
    }

    composable(
        route = AppRoute.ReferenceCreate.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceCreate.ARG_TARGET_USER_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceCreateScreen(
            onNavigateUp = { navController.navigateUp() },
            onReferenceCreated = { referenceId ->
                navController.navigate(AppRoute.ReferenceDetail.createRoute(referenceId)) {
                    popUpTo(AppRoute.ReferenceCreate.route) { inclusive = true }
                }
            },
        )
    }

    // CLAUDE.md §13: hedef kullanıcı "Evet/Hayır" cevabı verir
    composable(
        route = AppRoute.ReferenceRecognition.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceRecognition.ARG_REFERENCE_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceRecognitionScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToWrite = { referenceId ->
                navController.navigate(AppRoute.ReferenceWrite.createRoute(referenceId)) {
                    popUpTo(AppRoute.ReferenceRecognition.route) { inclusive = true }
                }
            },
        )
    }

    // CLAUDE.md §13: referansı yaz ve yönetici onayına gönder
    composable(
        route = AppRoute.ReferenceWrite.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceWrite.ARG_REFERENCE_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceWriteScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToList = {
                navController.navigate(AppRoute.References.route) {
                    popUpTo(AppRoute.ReferenceWrite.route) { inclusive = true }
                }
            },
        )
    }

    composable(
        route = AppRoute.ReferenceDetail.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceDetail.ARG_REFERENCE_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceDetailScreen(
            onNavigateUp = { navController.navigateUp() },
            onNavigateToResponse = { referenceId ->
                navController.navigate(AppRoute.ReferenceResponse.createRoute(referenceId))
            },
            onNavigateToAppeal = { referenceId ->
                navController.navigate(AppRoute.ReferenceAppeal.createRoute(referenceId))
            },
        )
    }

    // CLAUDE.md §13: alıcı onaylanan referansa cevap yazar
    composable(
        route = AppRoute.ReferenceResponse.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceResponse.ARG_REFERENCE_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceResponseScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }

    // CLAUDE.md §13: hedef kullanıcı onaylanmış referansa itiraz edebilir
    composable(
        route = AppRoute.ReferenceAppeal.route,
        arguments = listOf(
            navArgument(AppRoute.ReferenceAppeal.ARG_REFERENCE_ID) { type = NavType.StringType },
        ),
    ) {
        ReferenceDisputeScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
}

// --- CONTRACTS ---
fun NavGraphBuilder.contractsGraph(navController: NavController) {
    composable(AppRoute.Contracts.route) {
        ContractListScreen(
            onCreateContract = {
                navController.navigate(AppRoute.ContractCreate.createRoute("default"))
            },
            onContractClick = { contractId ->
                navController.navigate(AppRoute.ContractDetail.createRoute(contractId))
            },
        )
    }

    composable(AppRoute.ContractTemplates.route) { }

    composable(
        route = AppRoute.ContractCreate.route,
        arguments = listOf(
            navArgument(AppRoute.ContractCreate.ARG_TEMPLATE_ID) { type = NavType.StringType },
        ),
    ) {
        ContractCreateScreen(
            onNavigateUp = { navController.navigateUp() },
            onContractCreated = { contractId ->
                navController.navigate(AppRoute.ContractDetail.createRoute(contractId)) {
                    popUpTo(AppRoute.ContractCreate.route) { inclusive = true }
                }
            },
        )
    }

    composable(
        route = AppRoute.ContractDetail.route,
        arguments = listOf(
            navArgument(AppRoute.ContractDetail.ARG_CONTRACT_ID) { type = NavType.StringType },
        ),
    ) {
        ContractDetailScreen(
            onNavigateBack = { navController.navigateUp() },
        )
    }

    composable(
        route = AppRoute.ContractPdf.route,
        arguments = listOf(
            navArgument(AppRoute.ContractPdf.ARG_CONTRACT_ID) { type = NavType.StringType },
        ),
    ) { }
}

// --- SETTINGS ---
fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable(AppRoute.Settings.route) {
        SettingsScreen(
            onNavigateToLogin = {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToProfile = {
                navController.navigate(AppRoute.ProfileEdit.route)
            },
            onNavigateToChangePassword = {
                navController.navigate(AppRoute.ChangePassword.route)
            },
            onNavigateToDeleteAccount = {
                navController.navigate(AppRoute.DeleteAccount.route)
            },
            onNavigateToTermsOfService = {
                navController.navigate(AppRoute.TermsOfService.route)
            },
            onNavigateToPrivacyPolicy = {
                navController.navigate(AppRoute.PrivacyPolicy.route)
            },
            onNavigateToKvkk = {
                navController.navigate(AppRoute.KvkkText.route)
            },
            onNavigateToCommunityGuidelines = {
                navController.navigate(AppRoute.CommunityGuidelines.route)
            },
            onNavigateToRemoveAds = {
                navController.navigate(AppRoute.RemoveAds.route)
            },
            onNavigateToAbout = {
                navController.navigate(AppRoute.About.route)
            },
            onNavigateToHelpSupport = {
                navController.navigate(AppRoute.HelpSupport.route)
            },
            onNavigateToFaq = {
                navController.navigate(AppRoute.Faq.route)
            },
            onNavigateToBlockedUsers = {
                navController.navigate(AppRoute.BlockedUsers.route)
            },
            onNavigateToNotificationSettings = {
                navController.navigate(AppRoute.NotificationSettings.route)
            },
        )
    }
    composable(AppRoute.AccountSettings.route) { }
    composable(AppRoute.ChangePassword.route) {
        ChangePasswordScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.PrivacySettings.route) { }
    composable(AppRoute.NotificationSettings.route) {
        NotificationSettingsScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.ThemeSettings.route) { }
    composable(AppRoute.DataDownload.route) {
        DataDownloadScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.DeleteAccount.route) {
        DeleteAccountScreen(
            onNavigateUp = { navController.navigateUp() },
            onAccountDeleted = {
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
    composable(AppRoute.TermsOfService.route) {
        LegalTextScreen(
            title = "Kullanıcı Sözleşmesi",
            body = TERMS_OF_SERVICE_TEXT,
            onBack = { navController.navigateUp() },
        )
    }
    composable(AppRoute.PrivacyPolicy.route) {
        LegalTextScreen(
            title = "Gizlilik Politikası",
            body = PRIVACY_POLICY_TEXT,
            onBack = { navController.navigateUp() },
        )
    }
    composable(AppRoute.KvkkText.route) {
        LegalTextScreen(
            title = "KVKK Aydınlatma Metni",
            body = KVKK_TEXT,
            onBack = { navController.navigateUp() },
        )
    }
    composable(AppRoute.CommunityGuidelines.route) {
        LegalTextScreen(
            title = "Topluluk Kuralları",
            body = COMMUNITY_GUIDELINES_TEXT,
            onBack = { navController.navigateUp() },
        )
    }
    composable(AppRoute.About.route) {
        AboutScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.HelpSupport.route) {
        HelpSupportScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.Faq.route) {
        FaqScreen(
            onNavigateUp = { navController.navigateUp() },
        )
    }
    composable(AppRoute.RemoveAds.route) {
        RemoveAdsScreen(
            onNavigateUp = { navController.navigateUp() },
            onPurchaseSuccess = {
                navController.navigate(AppRoute.PurchaseConfirmation.route) {
                    popUpTo(AppRoute.RemoveAds.route) { inclusive = true }
                }
            }
        )
    }
    composable(AppRoute.PurchaseConfirmation.route) {
        PurchaseConfirmationScreen(
            onNavigateToFeed = {
                navController.navigate(AppRoute.Feed.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
}