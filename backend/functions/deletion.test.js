const test = require('node:test');
const assert = require('node:assert/strict');
const { validateRequest, eraseAccount } = require('./deletion');
test('rejects missing and stale authentication and unconfirmed deletion', () => {
  assert.throws(() => validateRequest({}, 1000));
  assert.throws(() => validateRequest({auth:{uid:'a',token:{auth_time:1}},data:{expectedUid:"a",confirm:true}},1000));
  assert.throws(() => validateRequest({auth:{uid:'a',token:{auth_time:999}},data:{}},1000));
  assert.throws(() => validateRequest({auth:{uid:'b',token:{auth_time:999}},data:{confirm:true,expectedUid:'a'}},1000));
  assert.equal(validateRequest({auth:{uid:'a',token:{auth_time:999}},data:{expectedUid:"a",confirm:true,uid:'victim'}},1000),'a');
});
test('failure preserves identity for retry; successful retry removes identity last', async () => {
  const calls=[]; let fail=true;
  const adapter={ media:async()=>{calls.push('media'); if(fail) throw Error('offline');}, entries:async()=>calls.push('entries'), profile:async()=>calls.push('profile'), identity:async()=>calls.push('identity') };
  await assert.rejects(eraseAccount('a',adapter));
  assert.deepEqual(calls,['media']);
  fail=false; await eraseAccount('a',adapter);
  assert.deepEqual(calls,['media','media','entries','profile','identity']);
});
test('persistent sweep cursor reaches later accounts after a full failing page', async()=>{
  const {sweepPending}=require('./deletion');
  let cursor=null; const attempted=[];
  const ids=Array.from({length:101},(_,i)=>String(i).padStart(3,'0'));
  const adapter={cursor:async()=>cursor,saveCursor:async uid=>{cursor=uid;},
    page:async after=>ids.filter(id=>after===null||id>after).slice(0,100),
    process:async uid=>{attempted.push(uid);if(uid!=='100')throw Error('transient');},failure:()=>{}};
  await sweepPending(adapter); await sweepPending(adapter);
  assert.equal(attempted.length,101);assert.equal(attempted.at(-1),'100');
});
