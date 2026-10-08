# Swift

A simple desktop media player built entirely in JavaFX

## Requirements

- JDK 17 or newer (developed on JDK 26)
- JavaFX SDK 25 for your operating system and architecture
- Set the `JAVAFX_HOME` environment variable to the JavaFX SDK installation
  directory (the directory containing `lib/`), or extract the SDK to a
  `javafx-sdk-25/` folder in the project root (used when `JAVAFX_HOME` is unset).
  The SDK is machine-specific and is not included in this repository.
- Optional: `ffmpeg` on PATH — formats JavaFX cannot open natively (e.g. MKV, AVI,
  WebM, FLAC, OGG) are converted on the fly; converted copies are cached in
  `~/.swift/transcode`. Without ffmpeg, such files show an error.

## Running

In PowerShell, set `JAVAFX_HOME` to the path where you installed the SDK:

```powershell
$env:JAVAFX_HOME = "C:\path\to\javafx-sdk-25"
```

Then run either script from the project directory:

**Easiest: double-click or run:**

```
run.bat
```

or, from PowerShell:

```
.\run.ps1
```

Both scripts compile the sources into `out/` and launch the app.

**From VS Code:** install the *Extension Pack for Java*, open this folder and
use the **Run Swift** launch configuration (`.vscode/launch.json`).

**Manually:**

```powershell
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac --module-path "$env:JAVAFX_HOME\lib" --add-modules javafx.controls,javafx.media -d out $sources
New-Item -ItemType Directory -Force out\com\swift\resources | Out-Null
Copy-Item src\com\swift\resources\* out\com\swift\resources\ -Recurse -Force
java "-Djava.library.path=$env:JAVAFX_HOME\bin" --module-path "$env:JAVAFX_HOME\lib;out" --add-modules javafx.controls,javafx.media -m swift/com.swift.SwiftApp
```

## Features

- Local audio and video playback (open files, open a folder, or drag and drop),
  with automatic ffmpeg transcoding fallback for unsupported containers/codecs
- Home page with recently played media (stored locally in `~/.swift/recent.tsv`)
- Queue page with search, remove and clear
- Shuffle, repeat (off / all / one), seek, volume, mute, playback speed, fullscreen
- Responsive layout with no clipping: full sidebar (>= 960 px), icon rail (640-959 px),
  bottom tab bar (< 640 px); the playback bar collapses its side zones by width
- Shortcuts: Space play/pause, S stop, N/P next/previous, M mute, Up/Down volume,
  Left/Right seek 5 s, F or F11 fullscreen

## Project structure

```
src/com/swift/
  SwiftApp.java      App wiring, responsive layout modes, playback logic, shortcuts
  model/             MediaItem, Playlist, RecentStore, RepeatMode
  player/            PlayerController (MediaPlayer lifecycle and state),
                     MetadataReader (tag/cover extraction), Transcoder (ffmpeg fallback)
  ui/                Sidebar, TopBar, HomeView, QueueView, MediaPane, ControlsBar,
                     ResponsiveGrid, Art, Icons, Splash, Branding
  util/              FormatUtils
  resources/         app.css (dark theme), fonts/ (Inter), images/ (logo, icons)
```
