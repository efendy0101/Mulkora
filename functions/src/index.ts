import * as admin from "./firebaseAdmin";

// Firebase Admin başlat
admin.initializeApp();

// ============================================================
// AUTH TRIGGER FONKSİYONLARI
// docs/14-cloud-functions-architecture.md
// ============================================================

// Yeni kullanıcı kayıt olduğunda Firestore profil belgesi oluşturur
export { onUserCreated } from "./auth/onUserCreated";

// Kullanıcı silindiğinde ilgili verileri temizler
export { onUserDeleted } from "./auth/onUserDeleted";

// ============================================================
// GÖNDERI (POST) FONKSİYONLARI
// docs/17-post-moderation-state-machine.md
// ============================================================

// Gönderi incelemeye gönderilir (draft → pending)
export { submitPost } from "./posts/submitPost";
export { incrementViewCount } from "./posts/incrementViewCount";

// Moderatör gönderiyi onaylar (pending → approved)
export { approvePost } from "./posts/approvePost";

// Moderatör gönderiyi reddeder (pending → rejected)
export { rejectPost } from "./posts/rejectPost";

// Moderatör düzenleme ister (pending → edit_requested)
export { requestPostEdit } from "./posts/requestPostEdit";

// Kullanıcı edit_requested gönderisini günceller (edit_requested → pending_approval)
export { updatePost } from "./posts/updatePost";

// edit_requested durumundaki gönderiyi düzenleme için getirir
export { getPostForEdit } from "./posts/getPostForEdit";

// ============================================================
// REFERANS (REFERENCE) FONKSİYONLARI
// docs/16-reference-system-state-machine.md
// ============================================================

// Referans daveti başlatır (yok → invite_sent)
export { initiateReference } from "./references/initiateReference";

// Tanıma onayı: awaiting_confirmation → preparing
export { acceptReference } from "./references/acceptReference";

// Tanımıyorum: awaiting_confirmation → not_recognized
export { rejectReference } from "./references/rejectReference";

// Referans içeriği gönderilir (preparing → pending_approval)
export { submitReference } from "./references/submitReference";

// Moderatör referansı onaylar veya reddeder (pending_approval → approved/rejected)
export { moderateReference } from "./references/moderateReference";

// Onaylı referansa yanıt yazar (approved → response_pending_review)
export { respondToReference } from "./references/respondToReference";

// Moderatör referans cevabını onaylar/reddeder
export { moderateReferenceResponse } from "./references/moderateReferenceResponse";

// Onaylı referansa itiraz eder (approved → disputed)
export { disputeReference } from "./references/disputeReference";

// Yönetici referans itirazını çözümler (disputed → approved | rejected)
export { resolveReferenceDispute } from "./references/resolveReferenceDispute";

// ============================================================
// TEPKİ (REACTION) FONKSİYONLARI
// Sayaçları atomic olarak günceller
// ============================================================

// Gönderi tepkisi ekler/kaldırır ve sayaçları günceller
export { togglePostReaction } from "./reactions/togglePostReaction";

// Gönderiyi puanlar (1-5 yıldız) — transaction ile atomik ortalama güncellenir
export { ratePost } from "./reactions/ratePost";

// ============================================================
// KULLANICI YÖNETİM FONKSİYONLARI
// ============================================================

// Kullanıcıyı engeller
export { blockUser } from "./users/blockUser";

// Kullanıcı engelini kaldırır
export { unblockUser } from "./users/unblockUser";

// Kullanıcıya moderatör veya admin rolü atar — sadece adminler çağırabilir
export { setUserRole } from "./users/setUserRole";

// Hesap siler (kullanıcı verisini temizler + Auth'dan kaldırır)
export { deleteUserAccount } from "./users/deleteUserAccount";

// FCM token ekler
export { updateFcmToken } from "./users/updateFcmToken";

// FCM token kaldırır
export { removeFcmToken } from "./users/removeFcmToken";

// Kullanıcı adı müsaitlik kontrolü
export { checkUsername } from "./users/checkUsername";

// Kullanıcı adını atomik olarak rezerve eder ve profile yazar
export { reserveUsername } from "./users/reserveUsername";

// Profil bilgilerini günceller
export { updateProfile } from "./users/updateProfile";

// ============================================================
// İÇERİK FİLTRELEME
// docs/18-content-filter-design.md
// ============================================================

// Gönderi oluşturulduğunda içerik filtresi çalıştırır
// onPostCreated → onPostStatusChanged olarak yeniden yazıldı (draft→pending geçiş trigger)
export { onPostStatusChanged } from "./moderation/onPostCreated";

// ============================================================
// BİLDİRİM FONKSİYONLARI
// docs/19-notification-system.md
// ============================================================

// Firestore bildirim belgesi oluştuğunda FCM gönderir
export { onNotificationCreated } from "./notifications/onNotificationCreated";

// Yönetici duyurusu push bildirimi gönderir (admin only)
export { sendSystemAnnouncement } from "./notifications/sendSystemAnnouncement";

// ============================================================
// ZAMANLANMIŞ FONKSİYONLAR (SCHEDULED)
// ============================================================

// 30 günden eski okunmuş bildirimleri temizler
export { cleanupOldNotifications } from "./scheduled/cleanupOldNotifications";

// Askıda kalan (stale) referansları kontrol eder
export { checkStaleReferences } from "./scheduled/checkStaleReferences";

// ============================================================
// YORUM (COMMENT) FONKSİYONLARI
// CLAUDE.md Bölüm 9: Yorum ve Cevap Sistemi
// ============================================================

// Yorum ekler (içerik filtresi + sayaç güncelleme)
export { addComment } from "./comments/addComment";

// Yorum siler (yazar veya moderatör)
export { deleteComment } from "./comments/deleteComment";

// Yorum beğenisini toggle eder
export { toggleCommentLike } from "./comments/toggleCommentLike";

// Yorum içeriğini düzenler (yazar tarafından)
export { editComment } from "./comments/editComment";

// ============================================================
// ŞİKAYET (REPORT) FONKSİYONLARI
// CLAUDE.md Bölüm 11: Şikâyet Sistemi
// ============================================================

// Şikayet oluşturur (mükerrer kontrolü yapılır)
export { createReport } from "./reports/createReport";

// Şikayeti çözüme kavuşturur (moderatör)
export { resolveReport } from "./reports/moderateReport";

// Şikayeti reddeder (moderatör)
export { dismissReport } from "./reports/moderateReport";

// ============================================================
// SÖZLEŞME (CONTRACT) FONKSİYONLARI
// docs/22-development-phases.md Faz 9: Kira Sözleşmesi
// ============================================================

// Sözleşme taslağı oluşturur (draft)
export { createContract } from "./contracts/createContract";

// İkinci tarafı sözleşmeye davet eder (draft → awaiting_party2)
export { inviteContractParty } from "./contracts/inviteContractParty";

// Taraf düzenleme talebinde bulunur (onayları sıfırlar → edit_requested)
export { requestContractEdit } from "./contracts/requestContractEdit";

// Taraf sözleşmeyi onaylar (awaiting_approval → both_approved)
export { approveContract } from "./contracts/approveContract";

// Her iki taraf onayladıktan sonra sözleşmeyi kilitler (both_approved → locked)
export { lockContractVersion } from "./contracts/lockContractVersion";

// Kilitli sözleşme için PDF üretir ve Storage'a yükler (CF-CONTRACT-003)
export { generateContractPdf } from "./contracts/generateContractPdf";

// Taslak veya davet bekleyen sözleşmeyi iptal eder (CF-CONTRACT-007)
export { cancelContract } from "./contracts/cancelContract";

// ============================================================
// KVKK — Veri Dışa Aktarma
// docs/22-development-phases.md — Faz 10
// ============================================================

// Kullanıcı kişisel veri dışa aktarma talebi oluşturur
export { requestDataExport } from "./users/requestDataExport";

// ============================================================
// FATURALANDIRMA (BILLING) FONKSİYONLARI
// docs/20-ads-billing-design.md
// ============================================================

// Google Play satın alma tokenını doğrular ve Firestore'a kaydeder
export { validatePurchaseToken } from "./billing/validatePurchaseToken";

// Google Play iade bildirimlerini işler (Firestore trigger)
export { onPurchaseRefunded } from "./billing/onPurchaseRefunded";
export { syncPublicProfile } from "./users/syncPublicProfile";
