const {test, before, after, beforeEach} = require('node:test');
const fs = require('node:fs');
const {initializeTestEnvironment, assertFails, assertSucceeds} = require('@firebase/rules-unit-testing');
const {doc,setDoc,getDoc,updateDoc,collection,query,where,getDocs} = require('firebase/firestore');
const {ref,uploadBytes,getBytes} = require('firebase/storage');
let env;
before(async()=>{
  env=await initializeTestEnvironment({projectId:'demo-ace-deletion',
    firestore:{host:'127.0.0.1',port:8080,rules:fs.readFileSync('../firestore.rules','utf8')},
    storage:{host:'127.0.0.1',port:9199,rules:fs.readFileSync('../storage.rules','utf8')}});
});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{await env.clearFirestore();await env.clearStorage();});
test('profile/category ownership denies strangers and unauthenticated clients',async()=>{
  const alice=env.authenticatedContext('alice').firestore();
  const bob=env.authenticatedContext('bob').firestore();
  const anonymous=env.unauthenticatedContext().firestore();
  for(const path of ['users/alice','users/alice/categories/work']) {
    await assertSucceeds(setDoc(doc(alice,path),{name:'private'}));
    await assertFails(getDoc(doc(bob,path))); await assertFails(setDoc(doc(bob,path),{}));
    await assertFails(getDoc(doc(anonymous,path)));
  }
});
test('entries require owner-filtered queries and immutable ownership',async()=>{
  const a=env.authenticatedContext('alice').firestore(), b=env.authenticatedContext('bob').firestore();
  await assertSucceeds(setDoc(doc(a,'timesheetEntries/one'),{userId:'alice'}));
  await assertFails(setDoc(doc(b,'timesheetEntries/two'),{userId:'alice'}));
  await assertFails(updateDoc(doc(a,'timesheetEntries/one'),{userId:'bob'}));
  await assertFails(getDoc(doc(b,'timesheetEntries/one')));
  await assertFails(getDocs(collection(a,'timesheetEntries')));
  await assertSucceeds(getDocs(query(collection(a,'timesheetEntries'),where('userId','==','alice'))));
});
test('deletion tombstone fences all Firestore access and cannot be modified by client',async()=>{
  const a=env.authenticatedContext('alice').firestore();
  await assertFails(setDoc(doc(a,'deletionRequests/alice'),{status:'complete'}));
  await env.withSecurityRulesDisabled(async c=>{await setDoc(doc(c.firestore(),'deletionRequests/alice'),{status:'pending'});});
  await assertSucceeds(getDoc(doc(a,'deletionRequests/alice')));
  await assertFails(getDoc(doc(env.authenticatedContext('bob').firestore(),'deletionRequests/alice')));
  await assertFails(setDoc(doc(a,'users/alice'),{}));
  await assertFails(setDoc(doc(a,'users/alice/categories/work'),{}));
  await assertFails(setDoc(doc(a,'timesheetEntries/one'),{userId:'alice'}));
  await assertFails(getDoc(doc(a,'users/alice')));
});
test('storage enforces owner, image type and deletion fence; rejects legacy paths',async()=>{
  const a=env.authenticatedContext('alice').storage(), b=env.authenticatedContext('bob').storage();
  const bytes=new Uint8Array([1,2,3]);
  await assertSucceeds(uploadBytes(ref(a,'users/alice/images/photo'),bytes,{contentType:'image/png'}));
  await assertFails(getBytes(ref(b,'users/alice/images/photo')));
  await assertFails(uploadBytes(ref(b,'users/alice/images/other'),bytes,{contentType:'image/png'}));
  await assertFails(uploadBytes(ref(a,'users/alice/images/script'),bytes,{contentType:'text/html'}));
  await assertFails(uploadBytes(ref(a,'images/legacy'),bytes,{contentType:'image/png'}));
  await env.withSecurityRulesDisabled(async c=>{await setDoc(doc(c.firestore(),'deletionRequests/alice'),{status:'pending'});});
  await assertFails(uploadBytes(ref(a,'users/alice/images/late'),bytes,{contentType:'image/png'}));
  await assertFails(getBytes(ref(a,'users/alice/images/photo')));
});
