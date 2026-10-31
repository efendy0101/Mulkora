const admin = require('../functions/lib/firebaseAdmin');
const projectId = process.env.GCLOUD_PROJECT;
if (!projectId || process.argv[2] !== '--confirm-project' || process.argv[3] !== projectId) {
  console.error('Set GCLOUD_PROJECT and pass --confirm-project <same project ID>.'); process.exit(1);
}
admin.initializeApp({projectId});
const db = admin.firestore();
const fields = ['username','displayName','photoUrl','avatarUrl','bio','city','role','links','postCount','referenceCount','averageReferenceRating','ratingCount','createdAt','updatedAt'];
(async()=>{let last;let count=0;for(;;){let q=db.collection('users').orderBy(admin.firestore.FieldPath.documentId()).limit(250);if(last)q=q.startAfter(last);const page=await q.get();if(page.empty)break;const batch=db.batch();for(const doc of page.docs){const data=doc.data();const safe={uid:doc.id};for(const key of fields)if(data[key]!==undefined)safe[key]=data[key];batch.set(db.collection('publicProfiles').doc(doc.id),safe);count++;}await batch.commit();last=page.docs[page.docs.length-1];}console.log(`Created or updated ${count} public profiles in ${projectId}.`);})().catch(e=>{console.error(e);process.exitCode=1});
