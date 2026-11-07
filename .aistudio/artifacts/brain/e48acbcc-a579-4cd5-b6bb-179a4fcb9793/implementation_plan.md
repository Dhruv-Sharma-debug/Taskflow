# Plan: Revert Calendar Features, Fix App Stability, & Keep Scheduled Date for Non-Repeating Tasks

## 1. Remove Calendar / Events / Google Calendar Sync & Fix Crash / Home Screen Stability
- **Clean 4-Tab Navigation**: Ensure only the 4 core tabs (**Today**, **Tasks**, **Projects**, **Courses**) exist with no Calendar tab, Events/Appointments models, or `CalendarContract` sync code.
- **Database & APK Stability**:
  - Update `AppDatabase` with both `.fallbackToDestructiveMigration()` and `.fallbackToDestructiveMigrationOnDowngrade()` and increment the schema version so upgrading or reinstalling never triggers a Room migration crash on startup.
  - Increment `versionCode` in `app/build.gradle.kts` so Android installs the APK cleanly on the home screen without version downgrade conflicts.
  - Wrap reminder/notification and sound helpers in safe `try/catch` blocks so device-specific OEM restrictions never crash the app.

## 2. Scheduled Date for Non-Repeating Tasks
- **Task Model & Persistence (`TaskEntity.kt`, `BackupHelper.kt`)**:
  - Add `scheduledDate: String? = null` (`YYYY-MM-DD`, defaulting to creation day `Today`) to `TaskEntity` and include it in JSON backup/restore.
- **Scheduled Date Card in Create / Edit Task Dialog (`Dialogs.kt`)**:
  - When **Repeat** is toggled **OFF** (`!isRecurring`), display a **Scheduled Date** card defaulting to **Today**.
  - Include quick-select chips (**Today**, **Tomorrow**, **Next Week**) and a **Pick Date** button that opens a Material 3 `DatePickerDialog`.
- **Today & Upcoming Filter Integration (`DateUtils.kt`, `TasksScreen.kt`, `TaskCard.kt`, `TaskDetailDialog.kt`)**:
  - Non-repeating tasks with a future `scheduledDate` (`scheduledDate > todayIso`) automatically appear under the **Upcoming** filter on the **Tasks** screen (and do not clutter Today's active list until their scheduled date arrives).
  - Display a scheduled date badge (e.g., `Tomorrow`, `Mon, Nov 10`) on `TaskCard` and `TaskDetailDialog` for scheduled tasks.
