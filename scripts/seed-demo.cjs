// This script always targets the local Firebase emulators and a demo project.
const admin = require('../functions/node_modules/firebase-admin');
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
const projectId = 'demo-mulkora';
admin.initializeApp({ projectId });
const auth = admin.auth();
const db = admin.firestore();
const timestamp = admin.firestore.Timestamp.now();

async function createIfMissing(ref, data) {
  try { await ref.create(data); } catch (error) { if (error.code !== 6) throw error; }
}
async function createUser(uid, email, displayName, role) {
  try {
    await auth.createUser({ uid, email, password: 'MulkoraDemo2026!', displayName, emailVerified: true });
    if (role === 'admin') await auth.setCustomUserClaims(uid, { role: 'admin' });
  } catch (error) {
    if (error.code !== 'auth/uid-already-exists') throw error;
  }
  await createIfMissing(db.collection('publicProfiles').doc(uid), { uid, displayName, username: uid.replace(/-/g, '_'), role, city: 'İzmir', createdAt: timestamp, updatedAt: timestamp });
  await createIfMissing(db.collection('users').doc(uid), {
    uid, email, displayName, username: uid.replace(/-/g, '_'), role,
    city: 'İzmir', isBanned: false, isBlocked: false, createdAt: timestamp, updatedAt: timestamp,
  });
}
async function main() {
  await createUser('demo-admin', 'admin@example.com', 'Demo Yönetici', 'admin');
  await createUser('demo-ayse', 'ayse@example.com', 'Ayşe Deniz', 'user');
  await createUser('demo-emre', 'emre@example.com', 'Emre Kaya', 'user');
  for (const [id, name] of [['kira_artisi', 'Kira artışı'], ['depozito', 'Depozito'], ['tasinma', 'Taşınma']]) {
    await createIfMissing(db.collection('categories').doc(id), { name, slug: id, isActive: true, postCount: 0, createdAt: timestamp });
  }
  const posts = [
    { id: 'demo-post-1', title: 'Taşınırken kontrol listesi', content: 'Yeni eve taşınmadan önce sayaçları ve teslim tutanağını birlikte kontrol etmek işimizi kolaylaştırdı.', category: 'tasinma', postType: 'experience', authorUid: 'demo-ayse', authorUsername: 'demo_ayse', status: 'approved' },
    { id: 'demo-post-2', title: 'Depozito iadesi için belgeler', content: 'Ev tesliminde hangi belgeleri saklamak faydalı olur? Deneyimlerinizi paylaşabilirsiniz.', category: 'depozito', postType: 'question', authorUid: 'demo-emre', authorUsername: 'demo_emre', status: 'approved' },
    { id: 'demo-post-3', title: 'Ortak alan bakımı hakkında', content: 'Apartman ortak alanlarının bakımı için örnek bir toplantı gündemi hazırlıyoruz.', category: 'tasinma', postType: 'advice', authorUid: 'demo-ayse', authorUsername: 'demo_ayse', status: 'pending' },
  ];
  for (const post of posts) {
    const { id, ...fields } = post;
    const data = { ...fields, postId: id, isAnonymous: false, imageUrls: [], likeCount: 0, commentCount: 0, createdAt: timestamp, updatedAt: timestamp };
    await createIfMissing(db.collection('posts').doc(id), data);
    if (post.status === 'pending') await createIfMissing(db.collection('moderationQueue').doc(id), { ...data, isFlagged: false });
  }
  console.log('Demo hazır: admin@example.com / MulkoraDemo2026! (yalnızca emülatör)');
}
main().catch((error) => { console.error(error); process.exitCode = 1; });
