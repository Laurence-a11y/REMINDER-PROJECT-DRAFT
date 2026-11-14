# Reminders (offline Android app)

A personal reminder app for Android. You type something like "pay rent every 5th at 9am" and it sets the reminder up for you. It works fully offline, with no account and no internet.

- **Quick type bar** with a live preview, plus a full **form** (date, time, repeat, heads-up, category, notes)
- Repeats: once, daily, weekly (pick days), monthly (day X, "2nd Tuesday", last day), yearly, and "every N" days/weeks/months/years
- **Notifications** at the exact time with **Done / Snooze 10 min / Snooze 1 hr** buttons. Reminders come back after the phone restarts
- **Dashboard**: month calendar with colored dots, and a task list underneath (Overdue, Today, This week, Later). Swipe right = done, swipe left = delete, both with Undo
- **Your own categories** with colors, plus a filter on the dashboard
- **How it works** page (also shown the first time you open the app)
- Backup / restore to a file, light/dark mode, default reminder time

Built with Kotlin + Jetpack Compose. minSdk 26 (Android 8), targetSdk 35. Everything here is free.

---

## Get the APK (free, about 10 minutes the first time)

### 1. Put the project on GitHub
1. Make a free account at github.com if you don't have one.
2. Click **New repository**, name it `reminders`, and choose **Private**. Click **Create**.
3. Upload the project. Use one of these:
   - **Git (recommended)**: open a terminal in this folder and run:
     ```
     git init
     git add .
     git commit -m "Reminders app"
     git branch -M main
     git remote add origin https://github.com/YOUR-USERNAME/reminders.git
     git push -u origin main
     ```
   - **GitHub Desktop**: File → Add local repository → pick this folder → Publish.
   - **Browser upload**: on the repo page, click "uploading an existing file" and drag in everything in this folder. **Check that the `.github` folder was uploaded too.** On macOS press Cmd+Shift+. to see hidden folders. Without it, no build will run.

### 2. Let GitHub build it
1. Open the **Actions** tab of your repo. A run called **Build APK** starts by itself. It takes about 5–8 minutes.
2. When it shows a green check, open the repo's **Releases** (on the right side of the repo page). Download `Reminders-build-N.apk`.
   It's easiest to open the Releases page right on your Nothing Phone.

Private repos get 2,000 free build minutes per month, which is about 250 builds.

### 3. Install on your phone
1. Tap the downloaded `.apk`. If Android asks, allow **Install unknown apps** for your browser or Files app.
2. Open **Reminders**. Read the welcome guide and tap **Allow notifications & get started**.
3. Go to **Settings** (gear icon) and tap **Allow** next to *Battery (no restrictions)*. This keeps alarms on time on Nothing OS.
4. Tap **Send test notification** to check that notifications work.

### Updating later
Change the code, push, and a new build appears in Releases. Install it over the old one. Your reminders stay, because every build is signed with the same key in `keystore/`.

---

## Notes

- **Signing key**: `keystore/reminders.jks` (password `reminders123`) is your own self-made key, which costs nothing. Keep the repo **private**, and don't lose this file, because Android only installs updates signed with the same key. Want a different password? Make a new key with `keytool` and update `app/build.gradle.kts`.
- **Open in Android Studio** (free): File → Open → this folder. It works there too, with Build → Build APK.
- **Data**: everything is stored in one file in the app's private storage. Use Settings → Export backup before you switch phones.
- **Typing parser**: English, rule-based, no AI and no internet. The logic is in `app/src/main/java/com/rence/reminders/logic/Parser.kt`, and the tests are in `app/src/test/`.

## Project layout
```
app/src/main/java/com/rence/reminders/
  MainActivity.kt          app start + simple navigation
  model/Models.kt          Reminder, RepeatRule, Category, Settings
  logic/Parser.kt          "type it" → reminder
  logic/Recurrence.kt      repeat math, next occurrence, next alarm
  logic/Format.kt          human-readable text
  data/Store.kt            saving/loading (JSON file)
  alarm/                   exact alarms, notifications, boot/Done/Snooze receivers
  ui/                      Dashboard, Edit form, Categories, Help, Settings
.github/workflows/build.yml  free cloud build → APK in Releases
```
