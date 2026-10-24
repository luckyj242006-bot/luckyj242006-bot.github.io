# Kharcha: from GitHub to the Play Store

Kharcha is a daily spend book with budgets, limit alerts, a loans tracker and automatic recording of payments. This folder holds everything you need to publish it on Google Play. You can publish it in two ways. Pick one.

| | Route A: PWABuilder | Route B: Native Android |
| --- | --- | --- |
| How it is made | PWABuilder wraps the website | GitHub builds a real Android app |
| Daily spend book, budget, limits, loans | Yes | Yes |
| Works offline | Yes | Yes |
| Loan and limit reminders as phone notifications | No, only a banner inside the app | Yes |
| Automatic recording from payment notifications | No | Yes |
| Extra work | Needs a website address | Needs a signing key |
| Play review risk | Low | Higher, because the app reads notifications |

Route A is simpler and safer for a first release. Route B has every feature. Decide before you create the app in the Play Console, because the package name cannot be changed after that.

None of the GitHub workflows or the Android build has been run yet, because the tools they need could not be installed where this project was made. Expect to fix small things on the first run. If a step turns red, open it and send the red error text to Claude.

## Before you start

1. A GitHub account.
2. A Google Play developer account. Google charges a one time fee and checks your identity.
3. If your developer account is personal and was created after 13 November 2023, Google requires a closed test first: at least 12 testers, all opted in for 14 days, before you can apply for production access. Plan for this, because it takes about two weeks.
4. Open `www/privacy.html` and replace `YOUR NAME` and `YOUR_EMAIL_ADDRESS` at the bottom. Delete the yellow box at the top. Google requires a privacy policy, and this one is written to match what the app does.
5. Read `store/store-listing.md`. It has the store text, the answers for the Play Console forms and the pictures (in `store/`).

## Route A: PWABuilder

1. Create a **public** repository named exactly `YOUR-GITHUB-USERNAME.github.io`. The name matters. Google checks a file at the root of your web address, and only a repository with this name controls the root. Any other name makes the app show a browser address bar on top.
2. Upload everything in this folder to it. The `.github` folder must go up too. If your file manager hides it, create the file `.github/workflows/pages.yml` in the GitHub website and paste in the contents.
3. In the repository open Settings, then Pages, and set Source to GitHub Actions.
4. Open the Actions tab. The workflow "Publish web app" runs by itself. When it is green, open `https://YOUR-GITHUB-USERNAME.github.io`. The app should load. On a phone in Chrome you should see an install option.
5. Go to pwabuilder.com and enter that address. It should score the app as installable.
6. Choose Package for stores, then Android. Set the package ID to something unique such as `com.yourname.kharcha`, the app name to Kharcha, and the version to 1. Choose to create a new signing key and download the zip. **Keep the zip.** It holds your signing key and its passwords, and you need them for every update.
7. In the Play Console, create the app and upload the `.aab` file from the zip to a closed test.
8. Play re-signs your app. In the Play Console open Setup, then App signing, and copy the SHA-256 fingerprint of the app signing key. Create the file `www/.well-known/assetlinks.json` in your repository using the package ID and that fingerprint (PWABuilder shows the exact format). Commit it. After the workflow finishes, open `https://YOUR-GITHUB-USERNAME.github.io/.well-known/assetlinks.json` and check it loads. Without this file, the installed app shows a browser address bar.

## Route B: native Android

1. Create a **private** repository and upload everything in this folder, including the `.github` folder. Keep it private, because the signing key in step 3 is downloadable by anyone who can see a public repository. The "Publish web app" workflow cannot run in a private repository and will show a red cross. Ignore it, or delete `.github/workflows/pages.yml` from this repository.
2. Google needs a public web address for your privacy policy. Create a second, **public** repository named exactly `YOUR-GITHUB-USERNAME.github.io` and upload only `privacy.html` into it (after filling in your name and email). In that repository open Settings, then Pages, and choose Deploy from a branch, main, root. The address is `https://YOUR-GITHUB-USERNAME.github.io/privacy.html`.
3. Open `capacitor.config.json` and change `appId` to something unique such as `com.yourname.kharcha`. Google does not allow two apps with the same ID, and you cannot change it after you publish.
4. Make your signing key. Open the Actions tab, choose "Create signing key", and press Run workflow. When it finishes, download `kharcha-signing-key` from the Artifacts list. Save the three files somewhere safe, such as a password manager. If you lose the key you must ask Google to reset it.
5. In the repository open Settings, then Secrets and variables, then Actions, and add two secrets:
   - `KEYSTORE_PASSWORD`: the password from `signing-info.txt`
   - `KEYSTORE_BASE64`: the whole text inside `KEYSTORE_BASE64.txt`
6. Delete the `kharcha-signing-key` artifact from the run page.
7. Choose "Build Play Store bundle" in the Actions tab and press Run workflow. It takes about 10 minutes. Download `kharcha-play-bundle` and unzip it to get `kharcha-release.aab`.
8. In the Play Console, create the app and upload `kharcha-release.aab` to a closed test. Turn on Play App Signing when asked, and keep using the same key for later updates.
9. To try the app on your own phone first, run "Build Android APK" and install the `app-debug.apk` it makes.

The native app targets Android 16 (API 36), which Google requires for new apps and updates from 31 August 2026.

### Turning on automatic recording on a phone

1. Open Budget and scroll to Automatic recording. Tap Turn on, read the screen, and choose Agree and continue.
2. Switch on Kharcha in the notification access list.
3. On Android 13 and newer, an app installed from a file can be blocked from this setting. If the switch is greyed out, go back, tap Open App info, tap the three dots at the top right, choose Allow restricted settings, and try again. Apps installed from the Play Store do not have this problem.

Kharcha reads notifications from UPI apps, bank apps and message apps. It keeps a message only when it looks like money you spent, saves the amount and who you paid, and throws the rest away. New spends show up when you open the app. Some phones (Xiaomi, Oppo, Vivo, Realme) stop background apps to save battery, so allow Kharcha to run in the background if spends stop arriving.

## In the Play Console

1. Create the app and fill the store listing from `store/store-listing.md`. Upload `store/icon-512.png`, `store/feature-graphic-1024x500.png` and the screenshots in `store/screenshots/`.
2. Add the privacy policy address: `https://YOUR-GITHUB-USERNAME.github.io/privacy.html`.
3. Complete the App content forms using the answers in `store/store-listing.md`.
4. Upload the bundle to a closed test and invite at least 12 testers. Keep them opted in for 14 days.
5. Apply for production access. Then publish.

Google may ask more questions about an app that reads notifications. Route B has a higher chance of delays or a rejection than Route A. If Google refuses the notification feature, you can ship Route B without it by removing the Automatic recording panel, or switch to Route A.

## Updating the app

- Route A: change `VERSION` in `www/sw.js` so phones fetch the new files, then commit. The website updates by itself. Upload a new package from PWABuilder only if you change the icon, name or colours.
- Route B: raise `version` in `package.json` and run "Build Play Store bundle" again. The version code goes up by itself on every run. Upload the new bundle to Play.

## What is inside

- `www/` is the whole app, plus `manifest.webmanifest`, `sw.js` (offline copy), `icons/` and `privacy.html`.
- `native/` is the Android code that reads payment notifications. `scripts/patch-android.mjs` copies it into the generated `android` folder and adds the manifest entries, the Google account backup rules and the notification icon.
- `scripts/vendor-fonts.mjs` downloads the two fonts at build time so the finished app never contacts Google. The release workflows fail if the download fails, so a published build is always self contained.
- `.github/workflows/` holds the four workflows: Publish web app, Create signing key, Build Play Store bundle and Build Android APK.
- `store/` holds the Play Store text and pictures.

## Good to know

- Backup. In the native Android app (Route B), Android Auto Backup saves the app data to the user's own Google account automatically when Google backup is on in their phone settings, and restores it on a new phone with the same account. It is limited to 25 MB, which is far more than this app needs, and Android decides when it runs (about once a day while the phone is charging, idle and on wi-fi). Clearing the app's storage or uninstalling and not reinstalling can still lose data that has not been backed up yet. It is hard to test without a real phone and a Google account, so try it once: install, add a spend, run `adb shell bmgr backupnow YOUR.APP.ID`, uninstall, reinstall.
- Route A (PWABuilder) has no automatic backup, because Chrome does not back up website storage in a way an app can rely on. Users of Route A, and anyone who wants an extra copy, can use Backup, then Save backup file in the app and send the file to their own email or Drive.
- The `Backup` panel in the Budget tab works on both routes.
- Amounts in the screenshots come from the sample data the app starts with.
- Loans are not counted as spending.
