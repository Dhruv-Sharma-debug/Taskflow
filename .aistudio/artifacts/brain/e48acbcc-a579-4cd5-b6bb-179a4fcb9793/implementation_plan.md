# TaskFlow — Home Screen Launcher Visibility & Reliable Task Reminders

This update resolves two issues: (1) ensuring TaskFlow appears prominently on the Android launcher/home screen by refreshing all adaptive & legacy raster launcher icons, manifest attributes, and adding an in-app **Pin to Home Screen** action; and (2) replacing the stubbed reminder helper with a real `AlarmManager` + `BroadcastReceiver` notification system that triggers a standard alert with sound and vibration at the exact scheduled `AM/PM` time.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during clarification:
> - **Task Reminder Alert Style**: Standard notification alert with sound and vibration when the task's scheduled reminder time (`hh:mm AM/PM`) arrives, tapping which opens TaskFlow directly.
> - **Home Screen Visibility & Shortcut Pinning**: Refresh the adaptive and density-specific PNG launcher icons, update the launcher manifest configuration, and provide a one-tap **Pin to Home Screen** button in the top bar using Android's `ShortcutManager` pin shortcut dialog.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Launcher & Home Screen Presence**: Replaces the legacy `.webp` launcher icons across all density buckets (`mdpi` through `xxxhdpi`) with high-contrast custom PNG (`ic_launcher.png` and circular `ic_launcher_round.png`) and adaptive vector icons, ensures standard `MAIN` + `LAUNCHER` + `DEFAULT` intent filter categories, and adds a top-bar **Pin to Home Screen** icon button that triggers Android's native "Add to Home Screen" dialog (`ShortcutManager.requestPinShortcut`).
  2. **Exact-Time Task Reminders**: Implements real `AlarmManager` scheduling (`setExactAndAllowWhileIdle` with graceful fallback to `setWindow` / `setAndAllowWhileIdle`) and a dedicated `BroadcastReceiver` (`TaskReminderReceiver`) that wakes the device and posts a high-importance notification with sound and vibration at the task's scheduled date and `hh:mm AM/PM` time, plus automatic reminder rescheduling across app launches and device reboots.
- **Target Audience / Persona**: Users managing daily and upcoming tasks who rely on home screen quick access and punctual time-based notifications.
- **Key Value**: TaskFlow is immediately accessible on the device home screen and reliably delivers scheduled task reminders on time.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home Screen Icon & Pinning**:
     - After installing/updating the app, TaskFlow displays a vibrant Indigo/Cyan/Emerald adaptive launcher icon in the app drawer and home screen.
     - On devices where sideloaded APK installs do not automatically place a shortcut on the main home screen page (such as Samsung One UI), tapping the **Pin to Home Screen** button in the top bar opens the system confirmation sheet to place the TaskFlow icon directly onto the home screen.
  2. **Scheduling & Receiving Task Reminders**:
     - When creating or editing a task with **Reminder Notification** toggled ON and a time selected (e.g., `09:30 AM` or `06:15 PM`), TaskFlow calculates the exact target timestamp:
       - For **non-repeating tasks**: uses the task's `scheduledDate` (`YYYY-MM-DD`) and selected `hh:mm AM/PM` (if `scheduledDate` is today and the chosen time is in the future, it schedules for today; if the time has already passed today, it schedules for the next occurrence so it never silently drops).
       - For **repeating tasks**: finds the next matching day of the week from `repeatDays` (starting today if the time hasn't passed yet) and schedules the alarm, then automatically schedules the next recurring day when triggered.
     - When the scheduled minute arrives, Android fires `TaskReminderReceiver`, playing the default notification sound and vibration pattern and showing the task title, priority (`P1`–`P4`), and duration in the notification shade.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Deep dark slate surfaces (`#0F172A` / `#1E293B`) with vibrant Indigo (`#6366F1`), Cyan (`#38BDF8`), and Emerald (`#10B981`) accents.
  - *Launcher Icon Palette*: Bold Indigo-to-Violet gradient background (`#4F46E5` to `#312E81`) with a crisp white/emerald task checkmark badge so the icon stands out against dark and light wallpapers alike.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: `AlarmManager` (`setExactAndAllowWhileIdle`) + `BroadcastReceiver` for Task Reminders**
  - *Chosen Approach*: Schedule exact alarms via `AlarmManager` (`setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` is true or on API < 31, with automatic fallback to `setAndAllowWhileIdle` so it never throws `SecurityException`), paired with a manifest-registered `TaskReminderReceiver` and `BOOT_COMPLETED` receiver.
  - *Why*: Unlike `WorkManager` (which defers background jobs by 15+ minutes under Doze mode), `AlarmManager` wakes the device at the exact `hh:mm AM/PM` minute the user picked.
  - *Alternatives Considered*: `WorkManager` periodic/one-time requests (too imprecise for minute-accurate reminders).
- **Decision 2: Native `ShortcutManager.requestPinShortcut` + Complete Raster & Adaptive Launcher Icons**
  - *Chosen Approach*: Generate all 5 density `ic_launcher.png` and circular `ic_launcher_round.png` assets, update `ic_launcher_background.xml` / `ic_launcher_foreground.xml`, and provide a top-bar **Pin to Home Screen** button backed by `ShortcutManager`.
  - *Why*: When sideloading APKs on Android 8+ (especially Samsung One UI), the OS places the app in the App Drawer rather than auto-adding a shortcut to the Home Screen unless requested via `ShortcutManager.requestPinShortcut`. Providing both a high-contrast launcher icon and a one-tap Pin to Home Screen action guarantees the icon is visible and placeable on any launcher.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌──────────────────────────────────────────────────────────────────────┐
│                         MainActivity (UI)                            │
│  ┌────────────────────────────┐   ┌───────────────────────────────┐  │
│  │ Top Bar Pin Shortcut Btn   │   │ AddEditTaskDialog (AM/PM)     │  │
│  │ ShortcutManager.requestPin │   │ Schedules / Updates Task      │  │
│  └─────────────┬──────────────┘   └───────────────┬───────────────┘  │
└────────────────┼──────────────────────────────────┼──────────────────┘
                 │                                  ▼
                 │                  ┌───────────────────────────────┐
                 │                  │      TaskFlowRepository       │
                 │                  │  saveTask / complete / delete │
                 │                  └───────────────┬───────────────┘
                 │                                  ▼
                 │                  ┌───────────────────────────────┐
                 │                  │      NotificationHelper       │
                 │                  │  • Creates High-Prio Channel  │
                 │                  │  • Computes triggerAtMillis   │
                 │                  │  • AlarmManager.setExact...   │
                 │                  └───────────────┬───────────────┘
                 ▼                                  ▼
┌──────────────────────────────┐    ┌───────────────────────────────┐
│   Android System Launcher    │    │     TaskReminderReceiver      │
│ • Adaptive & PNG Mipmap Icon │    │ • Fires Notification + Sound  │
│ • Pinned Home Screen Icon    │    │ • Reschedules on Boot / Next  │
└──────────────────────────────┘    └───────────────────────────────┘
```

- **Data Model & State**:
  - Uses existing `TaskEntity` fields (`id`, `title`, `description`, `priority`, `durationMins`, `time`, `isRecurring`, `repeatDays`, `scheduledDate`, `isCompleted`).
  - On app startup (`TaskFlowViewModel.init`) and device boot (`ACTION_BOOT_COMPLETED`), iterates all active uncompleted tasks with a non-blank `time` and ensures their alarms are scheduled.
- **Interactive Component & State Mapping**:
  - **Creating / Editing a Task**: Calls `NotificationHelper.scheduleTaskReminder(context, savedTask)` when `time != null && !isCompleted`, or `NotificationHelper.cancelTaskReminder(context, savedTask.id)` if the reminder toggle was turned off.
  - **Completing / Deleting a Task**: Calls `NotificationHelper.cancelTaskReminder(context, task.id)` so completed or deleted tasks do not trigger stale notifications.
  - **Pin to Home Screen Button**: Checks `ShortcutManager.isRequestPinShortcutSupported`, builds a `ShortcutInfo` pointing to `MainActivity` with the `TaskFlow` icon, and invokes `requestPinShortcut` (with a Toast fallback if already pinned or unsupported).
