# Ace download site

Static HTML/CSS/JS. Node 22+. Run `npm run build` from `site`; output is `site/dist`. Preview with `python3 -m http.server 4173 --directory dist`.

Set Vercel root directory to `site`, enable **Include source files outside of the Root Directory in the Build Step**, and use the committed `vercel.json` build/output settings. The build copies current screenshots, film, poster and SRT (converted to WebVTT) from `../docs`. No Node packages or runtime API are needed.

`download.json` pins a GitHub release asset and its SHA-256. Update version, URL, SHA-256 and size together when publishing a new APK. The build downloads the APK and fails on hash/size mismatch, then hosts the verified bytes at `/downloads/AceTimeTracker-demo-debug.apk`. An alternate manifest can be supplied with `ACE_DOWNLOAD_MANIFEST`. Nothing fetches an unpinned latest release. SHA verified against the GitHub asset digest for the initial manifest.

Place policy pages in `public/privacy.html` and `public/delete-account.html`; Vercel clean URLs serve `/privacy` and `/delete-account`. Those pages are owned by the release lead and must exist before deployment. No invented contact email is included.

Design: supplied “Colour chapters” direction, seed 1179578057; approved red/slate/white palette and system type. Ace's Today/Timer/History/Reports journey becomes a sequence of full-colour chapters. The live interaction switches between real app screenshots; without JS all screenshots remain visible. Hero adapts the type-poster blueprint to include the required real product screenshot and immediate download CTA. No third-party imagery, fonts, analytics or cookies.

Verification: Node syntax checks, production build with checksum verification and skill source lint. Browser screenshots, responsive/keyboard review, video playback, policy routes and final APK download remain the release lead's integration checks.
