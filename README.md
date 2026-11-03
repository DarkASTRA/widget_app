# My Text Widget 📱

A native Android notebook app that displays your notes directly on real, scrollable, customizable home-screen widgets.

## ✨ Key Features
- **Simple, Distraction-Free Notebook:** Write anything (study timetable, to-do list, reminders, quotes, Hindi/English, emojis). No mandatory dates, tags, or accounts required.
- **Real Native Android Home-Screen Widget:** Built using native `AppWidgetProvider`, `RemoteViews`, and `RemoteViewsService` collection (smooth vertical scrolling).
- **No Header / Direct Content:** Clean widget layout starting directly with your text without wasted title space.
- **Customization per Widget:**
  - Independent note binding for each widget instance
  - Font size adjustment and optional "Fit text" mode
  - Colors, background opacity / transparency
  - 6 instant color presets (Dark, Light, OLED Black, Warm Paper, Navy, Translucent)
  - Left / Center text alignment
  - Quick tap-to-edit action directly from the widget
- **Persistence & Offline:** Fully offline using AndroidX Room Database and Kotlin Coroutines. State survives reboots and app updates.
- **Auto-Save Drafts:** Automatically preserves your text while typing.

## 🛠️ Tech Stack & Architecture
- **Language:** Kotlin 1.9.x
- **UI:** Android Material Components 3 (Material Design), ViewBinding
- **Database:** AndroidX Room Database + SQLite
- **Architecture:** MVVM (Model-View-ViewModel) + LiveData + Kotlin Coroutines & Flow
- **Widget:** Android `AppWidgetProvider`, `RemoteViewsService.RemoteViewsFactory`

## 🚀 Building & Running
1. Open this directory in **Android Studio**.
2. Let Gradle sync and download dependencies.
3. Select your target device or emulator (Android 8.0 / API 26+).
4. Run `app` or build APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
