'use strict';
function validateRequest(request, now = Math.floor(Date.now() / 1000)) {
  if (!request.auth?.uid) throw Object.assign(Error('Sign in first.'), {code:'unauthenticated'});
  if (request.data?.expectedUid !== request.auth.uid)
    throw Object.assign(Error('Account changed. Sign in again.'), {code:'failed-precondition'});
  const time = request.auth.token.auth_time;
  if (!Number.isFinite(time) || now - time > 300 || time > now + 30)
    throw Object.assign(Error('Sign in again before deleting your account.'), {code:'failed-precondition'});
  if (request.data?.confirm !== true)
    throw Object.assign(Error('Confirm permanent deletion.'), {code:'invalid-argument'});
  return request.auth.uid;
}
async function eraseAccount(uid, adapter) {
  await adapter.media(uid);
  await adapter.entries(uid);
  await adapter.profile(uid);
  await adapter.identity(uid);
}
module.exports = {validateRequest, eraseAccount};

async function sweepPending(adapter) {
  let page = await adapter.page(await adapter.cursor());
  if (!page.length) page = await adapter.page(null);
  for (const uid of page) {
    // Persist before processing: even a timeout/failure moves the next sweep forward.
    await adapter.saveCursor(uid);
    try { await adapter.process(uid); }
    catch (error) { adapter.failure(error); }
  }
}
module.exports.sweepPending = sweepPending;
