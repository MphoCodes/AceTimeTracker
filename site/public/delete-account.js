(async () => {
  const status = document.querySelector('#portal-status');
  const form = document.querySelector('#delete-form');
  let config;
  try {
    const response = await fetch('/account-config.json', {cache:'no-store'});
    if (!response.ok) return;
    config = await response.json();
    if (config.deletionEnabled !== true) return;
    if (config.firebase.projectId !== 'ace-time-tracker-3d7d3') return;
    form.hidden = false;
    status.textContent = 'Verify your account below to request permanent deletion. An accepted request is processed in the background.';
  } catch { return; }
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const button = form.querySelector('button');
    button.disabled = true;
    status.textContent = 'Verifying ownership…';
    try {
      const authResponse = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(config.firebase.apiKey)}`, {
        method:'POST', headers:{'Content-Type':'application/json'},
        body:JSON.stringify({email:document.querySelector('#email').value.trim(), password:document.querySelector('#password').value, returnSecureToken:true})
      });
      document.querySelector('#password').value = '';
      const auth = await authResponse.json();
      if (!authResponse.ok || !auth.idToken) throw new Error('We could not verify the account. Check your email and password, or reset your password in the app.');
      status.textContent = 'Requesting deletion…';
      const response = await fetch('https://us-central1-ace-time-tracker-3d7d3.cloudfunctions.net/deleteAccount', {
        method:'POST', headers:{'Content-Type':'application/json',Authorization:`Bearer ${auth.idToken}`},
        body:JSON.stringify({data:{confirm:true,expectedUid:auth.localId}})
      });
      const result = await response.json();
      if (!response.ok || result.result?.status !== 'accepted') throw new Error('Deletion was not accepted. Please try again later or use Settings → Delete account in the app.');
      form.reset(); form.hidden = true;
      status.textContent = 'Your deletion request was accepted. Account access is now blocked while the service removes your stored data in the background. Clear AceTimeTracker storage on your other devices and remove any exports you saved separately.';
    } catch (error) {
      status.textContent = error.message === 'Failed to fetch' ? 'The service could not be reached. Your deletion request is not confirmed. Please try again when connected.' : error.message;
    } finally { document.querySelector('#password').value = ''; button.disabled = false; }
  });
})();
