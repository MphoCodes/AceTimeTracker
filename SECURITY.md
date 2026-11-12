# Security

Please report vulnerabilities privately to **mpho.c.ndlela@gmail.com**. Include the affected commit/version, reproduction steps, and impact. Do not include passwords, tokens, private account data, or service-account credentials.

Do not file a public issue containing an exploitable vulnerability or test against another person's Firebase project without authorization. There is no promised response SLA or currently supported production release.

## Configuration boundaries

Firebase Android client configuration identifies a project; it is not an administrative credential. Security depends on correctly deployed Firebase rules, authentication, API restrictions, and backend ownership. None of those should be inferred from a successful APK build.

Debug sample sign-in is local and gated by Android's debuggable flag. It must remain unavailable in release builds. Keep signing keys and service-account credentials outside the repository.

If a credential is accidentally exposed, revoke or rotate it immediately. Removing it from the latest commit does not remove it from Git history.
