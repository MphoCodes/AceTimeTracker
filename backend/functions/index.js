'use strict';
const {initializeApp} = require('firebase-admin/app');
const {getAuth} = require('firebase-admin/auth');
const {getFirestore, FieldValue, FieldPath} = require('firebase-admin/firestore');
const {getStorage} = require('firebase-admin/storage');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {onSchedule} = require('firebase-functions/v2/scheduler');
const {defineBoolean} = require('firebase-functions/params');
const deletionEnabled = defineBoolean('ACCOUNT_DELETION_ENABLED', {default:false});
const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {validateRequest, eraseAccount, sweepPending} = require('./deletion');
initializeApp();
const db = getFirestore();
exports.deleteAccount = onCall({region:'us-central1'}, async request => {
  if (!deletionEnabled.value()) throw new HttpsError("unavailable", "Account deletion service is not enabled yet. Contact support.");
  let uid;
  try { uid = validateRequest(request); }
  catch (error) { throw new HttpsError(error.code, error.message); }
  // The tombstone is written before cleanup and is retained to prevent stale clients
  // or pending SDK writes from recreating data. No client-selected UID/path is used.
  await db.runTransaction(async transaction => {
    const ref = db.doc(`deletionRequests/${uid}`);
    if (!(await transaction.get(ref)).exists)
      transaction.create(ref, {status:'pending', requestedAt:FieldValue.serverTimestamp()});
  });
  return {status:'accepted'};
});
exports.processAccountDeletion = onDocumentCreated({
  document:'deletionRequests/{uid}', region:'africa-south1', retry:true,
  timeoutSeconds:540, memory:'512MiB', maxInstances:5
}, async event => {
  await processDeletion(event.params.uid);
});

async function processDeletion(uid) {
  const ref = db.doc(`deletionRequests/${uid}`);
  if ((await ref.get()).get("status") === "complete") return;
  try {
  await eraseAccount(uid, {
    media: async () => {
      await getStorage().bucket().deleteFiles({prefix:`users/${uid}/`});
    },
    entries: async () => {
      while (true) {
        const page = await db.collection('timesheetEntries').where('userId','==',uid).limit(400).get();
        if (page.empty) break;
        const batch = db.batch();
        page.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
      }
    },
    profile: async () => db.recursiveDelete(db.doc(`users/${uid}`)),
    identity: async () => {
      try { await getAuth().deleteUser(uid); }
      catch (error) { if (error.code !== 'auth/user-not-found') throw error; }
    }
  });
  await ref.update({status:'complete', completedAt:FieldValue.serverTimestamp()});
  } catch (error) {
    await ref.update({lastFailureAt:FieldValue.serverTimestamp(), attempts:FieldValue.increment(1)});
    throw error;
  }
}
// Eventarc retries are bounded; sweep pending requests so failures are not abandoned.
exports.retryAccountDeletions = onSchedule({schedule:'every 60 minutes', region:'africa-south1',
  timeoutSeconds:540, memory:'512MiB', maxInstances:1}, async () => {
  const cursorRef = db.doc('operations/accountDeletionSweep');
  await sweepPending({
    cursor: async () => (await cursorRef.get()).get('afterUid') || null,
    saveCursor: async uid => cursorRef.set({afterUid:uid}),
    page: async after => {
      let query = db.collection('deletionRequests').where('status','==','pending')
        .orderBy(FieldPath.documentId()).limit(100);
      if (after) query = query.startAfter(after);
      return (await query.get()).docs.map(doc => doc.id);
    },
    process: processDeletion,
    failure: error => console.error('Account cleanup failed; pending request retained', {code:error.code || 'unknown'})
  });
});
