# Photobook Organizer

A native Android app to track your photobook workflow: Candidate -> Placed -> Needs Edit -> Edited -> Final,
sorted by date taken, with real folders on your phone for the Lightroom round-trip. Multiple projects supported.

## Setup - no computer needed (Tab or phone browser is enough)

### 1. Create a GitHub repo
On github.com, create a new **empty** repository (e.g. `photobook-organizer`). Don't add a README there - this
project brings its own.

### 2. Open it in GitHub Codespaces
On the repo page: **Code -> Codespaces tab -> Create codespace on main**.
This opens a full VS Code-like editor with a terminal, in your browser. Works fine on the Tab.

### 3. Upload this project
In the Codespace, drag `photobook-organizer.zip` into the file explorer sidebar, then in the terminal:
```
unzip photobook-organizer.zip -d .
rm photobook-organizer.zip
```
This places `settings.gradle.kts`, `app/`, `.github/`, etc. directly at the repo root.

### 4. Push it
```
git add -A
git commit -m "Initial app"
git push
```

### 5. Let it build
The push triggers GitHub Actions automatically. Open the **Actions** tab on the repo page, click the running
workflow, and wait for the green check (a few minutes).

### 6. Download and install
In that workflow run, scroll to **Artifacts**, download `photobook-organizer-debug` (a zip with the APK inside).
On your phone, unzip it (the Files app can do this) and tap the `.apk` to install. The first time, you may need
to allow "install unknown apps" for whichever app you used to open it.

## Using the app
- Tap **+** to create a project: name it, then pick or create a folder on your phone as its home. The app creates
  `Inbox`, `ToLightroom`, `EditedReturn`, and `Export` subfolders inside it automatically.
- Add photos by dropping them into `Inbox` with any file manager, or by selecting photos in Gallery -> Share ->
  Photobook Organizer. Then tap **Scan Inbox**.
- Photos are always sorted by the date they were taken, whatever the source.
- Tap a photo to change its status. Marking it **Needs Edit** copies it into `ToLightroom` - point Lightroom's
  import there.
- After editing, export/save the result into `EditedReturn`, then tap **Scan Returns** - matches flip to Edited
  automatically (matched by filename).
- Mark a photo **Final** when it's done. Tap **Export Finals** any time to copy every Final photo into `Export`,
  ready to upload for ordering.
- Files are always copied, never re-encoded - original quality is preserved throughout.

## If the build fails
Open the Actions tab -> the failed run -> read the red step's log, and send me the error text. This is a
sizeable project I wrote without being able to compile it myself (no Android build tools in my environment), so
some back-and-forth on the first build is realistic - I'll fix it fast once I see a real error.
