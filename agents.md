# Projekat - Android Notes & Tasks App

## Goal

Android app (Jetpack Compose, Kotlin) for managing notes and tasks. Features:
- Creating, editing, deleting notes and tasks
- Adding images to notes from camera/gallery
- Searching notes by title
- Bookmarking notes, soft-deleting notes (kept 7 days)
- Task status tracking (in progress / completed)
- Deadlines for tasks
- Calendar view showing tasks per selected date with expandable previews
- Push notifications when deadlines expire
- Three main screens: Notes (with side drawer for filters), Tasks, Calendar
- Modern, visually appealing GUI first, backend logic next

## Instructions

- User communicates in Serbian (use Serbian for UI labels/text)
- Build GUI first - the interface should look modern and polished
- Package name: `com.example.projekat`
- Pure Jetpack Compose project (no XML layouts), Material 3
- Build system: Gradle with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- AGP 8.13.2, Kotlin 2.0.21, Gradle 8.13, compileSdk 36, minSdk 24
- No Android SDK available in the build environment - must build in Android Studio

## Discoveries

- The original project was a blank "Empty Compose Activity" template from Android Studio
- **Critical issue found and fixed:** Each screen had its own inner `Scaffold` which added status bar padding. Combined with `enableEdgeToEdge()` in MainActivity and the outer `Scaffold`'s `innerPadding`, this caused **double padding** — content pushed too far down
- The `SearchBar` composable from Material 3 adds significant vertical space by default. Replaced with a compact `OutlinedTextField` (52dp height) to save space
- Fix: Removed inner `Scaffold` from all three main screens, replaced with `Box` + `Column` layout, moved FABs into `Box` with `Alignment.BottomEnd`
- Fix: Removed inner `Scaffold` from both detail screens (`NoteDetailScreen`, `TaskDetailScreen`) which also had double padding from the outer `Scaffold`'s `innerPadding` in `MainActivity`
- Cannot run Gradle builds in this environment (no Android SDK) — user must build in Android Studio
- **Dark theme note colors:** Light pastel note colors (`NoteYellow`, `NoteBlue`, etc.) looked washed out in dark mode with light text on light backgrounds. Added dark muted variants (`NoteYellowDark`, `NoteBlueDark`, etc.) and theme-aware color selection in both `NoteCard` and `NoteDetailScreen`
- **Deadline notification timing fix:** `DatePicker` returns `selectedDateMillis` as midnight UTC (e.g. `2026-03-02T00:00:00Z`). Scheduling notifications directly at this timestamp caused them to fire at ~1:00 AM CET / ~2:00 AM CEST instead of a sensible time. Fixed by converting UTC midnight to local-time 9:00 AM via `Calendar` API in `DeadlineScheduler.toLocal9AM()`. Both reminder (day before) and deadline-day notifications now fire at 9:00 AM local time. Updated notification text from "Rok istekao" to "Danas ističe rok za..." since notifications now fire in the morning, not after expiry.

## Accomplished

### Completed:
1. **Dependencies added** to `libs.versions.toml` and `app/build.gradle.kts`: Navigation Compose (2.8.4), Lifecycle ViewModel Compose (2.8.7), Material Icons Extended (1.7.5)
2. **Custom theme**: Modern indigo/amber/teal color palette with light+dark schemes, full Typography definitions, status bar color handling
3. **Data models**: `Note` (id, title, content, imageUris, colorIndex, isBookmarked, isDeleted, deletedAt, timestamps) and `Task` (id, title, description, status enum IN_PROGRESS/COMPLETED, deadline, noteId, colorIndex, timestamps)
4. **Navigation**: `Screen` sealed class with all routes, `AppNavHost` with all composable destinations
5. **Bottom Navigation Bar**: 3 tabs (Beleske, Taskovi, Kalendar) with filled/outlined icons
6. **NotesScreen**: Staggered grid of colored note cards, OutlinedTextField search bar, side modal drawer with filters (All/Bookmarked/Deleted), FAB
7. **NoteDetailScreen**: Google Keep-style editor — full-screen note color background, borderless title/content TextFields, bottom action bar with color picker (animated slide-up panel), camera/gallery icons, auto-save text. No explicit save button — auto-saves on text change with 800ms debounce + saves on exit via DisposableEffect.
8. **TasksScreen**: Stats cards (in progress/completed counts), task list with status circles, deadline display, status badges, FAB
9. **TaskDetailScreen**: Google Keep-style editor matching NoteDetailScreen — full-screen themed background, borderless title/description TextFields, inline status chips, deadline/note-attachment cards, bottom bar with status indicator, auto-save with 800ms debounce + saveOnExit via DisposableEffect. No explicit save button.
10. **CalendarScreen**: Custom calendar grid (Monday-first), month navigation, dots on days with tasks, expandable task cards below calendar
11. **MainActivity**: NavController with bottom bar (hidden on detail screens), single outer Scaffold with innerPadding, `@AndroidEntryPoint`
12. **Fixed double-padding issue**: Removed inner Scaffolds from all 3 main screens
13. **Room Database**: Full Room setup with entities (Note, Task), DAOs (NoteDao, TaskDao), AppDatabase with TypeConverters
14. **Hilt Dependency Injection**: `@HiltAndroidApp` Application class, `@AndroidEntryPoint` on MainActivity, DatabaseModule providing DB/DAOs
15. **Repositories**: NoteRepository (CRUD, soft-delete, 7-day cleanup, bookmark toggle, search), TaskRepository (CRUD, status toggle, day filtering)
16. **ViewModels**: NotesViewModel, NoteDetailViewModel, TasksViewModel, TaskDetailViewModel, CalendarViewModel — all `@HiltViewModel` with proper state management via StateFlow
17. **Screens wired to ViewModels**: All screens now use `hiltViewModel()` instead of mock data, with reactive UI via `collectAsState()`
18. **Date Picker**: TaskDetailScreen now has a working Material 3 DatePickerDialog for setting deadlines
19. **Note attachment to tasks**: TaskDetailScreen can select from available notes to attach to a task
20. **Camera/gallery image picking**: Full implementation — camera captures via FileProvider, gallery picks via GetContent, images copied to app cache for stable URIs, Coil AsyncImage for display. Remove-image button (X) overlaid on each image. Image thumbnails shown in NoteCard on NotesScreen. Coil 2.6.0 added as dependency.
21. **Attached note inline preview in TaskDetailScreen**: `AttachedNotePreview` composable showing the note's color, first image thumbnail, title, content snippet (3 lines max), and "Otvori belešku >" link. Card is clickable to navigate to the full note. X button to detach.
22. **AppNavHost wired onNoteClick**: Both TaskDetail and TaskCreate routes now pass `onNoteClick` to `TaskDetailScreen`, enabling navigation to attached notes.
23. **Calendar swipe gesture**: Horizontal swipe on the calendar card changes month (swipe left = next month, swipe right = previous month). Uses `detectHorizontalDragGestures` with a 100px threshold. Includes `AnimatedContent` slide transition matching swipe direction.
24. **Swipe-back on detail screens**: Reusable `SwipeBackContainer` composable in `ui/components/`. Both `NoteDetailScreen` and `TaskDetailScreen` are wrapped — swipe right from anywhere to go back. Features smooth `Animatable`-driven offset, dark scrim behind sliding content, and 35% screen-width dismiss threshold.
25. **Multiple images per note (Google Keep style)**: Migrated from single `imageUri: String?` to `imageUris: List<String>`. Room database migrated v1→v2 with `MIGRATION_1_2` that adds `imageUris` column and copies existing single image data. Added `List<String>` TypeConverter using `org.json.JSONArray`. NoteDetailScreen shows single image full-width or multiple images in horizontal scrollable row with individual remove buttons. NoteCard shows first image with "+N" overlay badge when multiple images exist. Camera/gallery now append images instead of replacing. AttachedNotePreview shows first image from the list.
26. **Task coloring (matching notes)**: Added `colorIndex` field to Task entity. Room database migrated v2→v3 with `MIGRATION_2_3` that adds `colorIndex` column to tasks table (default 0). TaskDetailScreen now has a color picker in the bottom bar (identical animated slide-up panel as NoteDetailScreen) and full-screen colored background based on selected color. TaskCard in TasksScreen and CalendarTaskCard in CalendarScreen both display the task's color as card background with theme-aware light/dark variants. TaskDetailViewModel updated with `updateColorIndex()` and `toggleColorPicker()` methods.
27. **Soft-delete with 7-day auto-cleanup (WorkManager)**: Full implementation of background cleanup for soft-deleted notes. Added WorkManager 2.9.1 + Hilt WorkManager integration (`androidx.hilt:hilt-work` 1.2.0) as dependencies. Created `CleanupWorker` (`@HiltWorker` + `CoroutineWorker`) that calls `NoteRepository.cleanupOldDeletedNotes()` to permanently remove notes deleted more than 7 days ago. `ProjekatApplication` now implements `Configuration.Provider` with custom `HiltWorkerFactory`, and schedules a `PeriodicWorkRequest` (every 24 hours, battery-not-low constraint) via `WorkManager.enqueueUniquePeriodicWork`. Disabled default WorkManager initializer in `AndroidManifest.xml`. Also added: NoteDao `deleteAllDeletedNotes()` query, NoteRepository `emptyTrash()` method, NotesViewModel `emptyTrash()` function. UI enhancements: deleted note cards now show "Ostalo X dana" countdown badge (red when <= 1 day), permanent delete button (DeleteForever icon) on each deleted card, and "Isprazni" (empty trash) button next to the title in Deleted view with a confirmation AlertDialog.
28. **Push notifications for deadline expiry**: Full implementation of deadline notifications using WorkManager. Created `DeadlineWorker` (`@HiltWorker` + `CoroutineWorker`) that verifies task still exists/is in-progress, then fires a high-priority notification with task title. Created `DeadlineScheduler` singleton (Hilt `@Singleton`) that schedules `OneTimeWorkRequest` with `initialDelay` calculated from deadline timestamp, using `enqueueUniqueWork` with `REPLACE` policy per task. Notification channel "Rokovi za taskove" (`IMPORTANCE_HIGH`) created in `ProjekatApplication.onCreate()`. `POST_NOTIFICATIONS` permission added to manifest and requested at runtime in `MainActivity` (Android 13+). `TaskDetailViewModel` schedules notification on save when deadline is set (new or updated tasks), cancels on deadline removal/task completion/task deletion. `TasksViewModel` cancels notification on status toggle to COMPLETED and re-schedules on toggle back to IN_PROGRESS; also cancels on task delete.
29. **Task priority system**: Added `TaskPriority` enum (`HIGH`, `MEDIUM`, `LOW`) to `Task.kt`. New `priority` field with default `MEDIUM`. Room database migrated v3→v4 with `MIGRATION_3_4` that adds `priority TEXT NOT NULL DEFAULT 'MEDIUM'` column. TypeConverter added for `TaskPriority`. TaskDetailScreen has 3 priority chips (Visok/Srednji/Nizak) with color-coded indicators (red/orange/green). TaskCard on TasksScreen shows priority badge alongside deadline. TaskDetailViewModel updated with `updatePriority()` and priority included in save/load. Priority colors added to Color.kt (`PriorityHigh`, `PriorityMedium`, `PriorityLow`).

30. **Embedded AI scheduling (FAZA 3-5 - MIGRATED FROM KTOR SERVER)**: Full AI scheduling implementation calling Pollinations.ai directly from Android app (eliminated separate Ktor server). Created `data/ai/` package with `PollinationsAiClient` (direct OkHttp calls to `https://text.pollinations.ai`), `AiScheduleService` (business logic layer), and `AiScheduleModels` (request/response models). `PollinationsAiClient` sends prompt to Pollinations.ai (model "openai"), parses AI response with robust handling: arrays, single objects, objects with nested arrays (scheduledTasks/tasks/schedule keys), markdown code blocks. Fallback returns original deadlines if parsing fails. Missing tasks filled with deadline fallback. Uses Calendar API instead of LocalDate for minSdk 24 compatibility. Added Gson 2.10.1 and OkHttp 4.12.0 as dependencies, removed Retrofit. Created `di/AiModule.kt` Hilt module providing `PollinationsAiClient` and `AiScheduleService`. Updated `AiScheduleRepository` to use `AiScheduleService` instead of Retrofit. Removed old `data/remote/` package and `NetworkModule`. Full end-to-end AI scheduling flow in TasksScreen: "AI Raspored" button (with AutoAwesome icon) visible when eligible tasks exist (in-progress + has deadline). Selection mode with checkboxes, loading spinner, results shown in `SchedulePreviewDialog` with original deadline → new AI-scheduled date. "Primeni" overwrites task deadlines and re-schedules push notifications. Errors shown via Snackbar. App now fully self-contained, no separate server needed.

31. **Checklist in Notes/Tasks (FEATURE 1)**: Full checklist implementation with checkboxes inside notes and tasks. Created `ChecklistItem` data model (`data/model/ChecklistItem.kt`) with id, text, isChecked, and order fields. Added `List<ChecklistItem>` TypeConverter in `Converters.kt` using JSON serialization. Added `checklistItems` field to both `Note` and `Task` entities. Room database migrated v4→v5 with `MIGRATION_4_5` adding `checklistItems TEXT NOT NULL DEFAULT '[]'` column to both notes and tasks tables. Created reusable `ChecklistEditor` composable (`ui/components/ChecklistEditor.kt`) with add item (+), toggle checkbox, inline text editing, and delete (X) functionality. Also includes `ChecklistPreview` composable for compact display in cards. Updated `NoteDetailViewModel` and `TaskDetailViewModel` with checklist management methods (`addChecklistItem`, `toggleChecklistItem`, `updateChecklistItemText`, `deleteChecklistItem`). Added checklist sections to `NoteDetailScreen` and `TaskDetailScreen` with full CRUD operations. `NoteCard` and `TaskCard` now show checklist preview with progress indicator (e.g., "2/5") and first few items.

32. **Shake to Undo (FEATURE 2)**: Full shake-to-undo implementation for reverting unsaved changes in note/task editors. Created `ShakeDetector` utility class (`util/ShakeDetector.kt`) using `SensorManager` + `TYPE_ACCELEROMETER` with 12 m/s² threshold and 1000ms cooldown. Created `UndoDialog` composable (`ui/components/UndoDialog.kt`) with Serbian text ("Ponisti izmene?"). Updated `NoteDetailViewModel` with undo state (`showUndoDialog`, `hasUnsavedChanges`), `lastSavedState` tracking, and methods `showUndoDialog()`, `dismissUndoDialog()`, `revertToLastSaved()`, `hasChangesSinceLastSave()`. Updated `NoteDetailScreen` with `ShakeDetector` lifecycle integration (start on ON_RESUME, stop on ON_PAUSE) and `UndoDialog` display. Applied same changes to `TaskDetailViewModel` and `TaskDetailScreen`. When user shakes phone while editing, dialog appears; "Da, ponisti" reverts to last saved state, "Ne, zadrzi" dismisses.

33. **Location-based Task Notifications (FEATURE 3)**: Full geofencing implementation for location-based task reminders. Added location fields to `Task` entity (`locationLat`, `locationLng`, `locationName`, `locationRadius`). Room database migrated v5→v6 with `MIGRATION_5_6` adding location columns to tasks table. Added Play Services Location 21.3.0 as dependency. Created `location/` package with: `GeofenceManager` (Hilt @Singleton, uses `GeofencingClient` to add/remove/update geofences), `GeofenceBroadcastReceiver` (handles `GEOFENCE_TRANSITION_ENTER`, fetches task from DB, fires notification). Created `LocationPicker` composable (`ui/components/LocationPicker.kt`) with address search via Geocoder, "Moja lokacija" button using FusedLocationProviderClient, radius slider (50-500m), permission handling for fine/coarse/background location. Updated `TaskDetailViewModel` with location state management and geofence scheduling on save. Updated `TaskDetailScreen` with LocationPicker section. Updated `TasksViewModel` to remove geofences on task completion/deletion, re-add on status toggle back to in-progress. Added location permissions to AndroidManifest (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION) and registered GeofenceBroadcastReceiver. Created second notification channel "Lokacijska obavestenja" in `ProjekatApplication`. No Google Maps API key required — uses Geocoder for address lookup.

34. **Firebase Authentication + Cloud Sync (FEATURE 4)**: Full Firebase integration for user accounts and cloud synchronization. Added Firebase BOM 33.7.0, firebase-auth-ktx, firebase-firestore-ktx, firebase-storage-ktx dependencies. Added google-services plugin to project. Created `SyncStatus` enum (`LOCAL_ONLY`, `SYNCED`, `PENDING_UPLOAD`, `PENDING_DELETE`) in `data/model/SyncStatus.kt`. Added `syncStatus` and `cloudId` fields to both `Note` and `Task` entities. Room database migrated v6→v7 with `MIGRATION_6_7` adding sync columns. Added `SyncStatus` TypeConverter in `Converters.kt`. Created `di/FirebaseModule.kt` providing `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage`. Created `AuthRepository` with login/register/logout/password reset (email/password auth), Serbian error messages. Created `CloudNoteRepository` and `CloudTaskRepository` for Firestore CRUD with image upload to Firebase Storage. Created `data/sync/` package with `SyncManager` (orchestrates full sync: upload pending, delete pending, download and merge with conflict resolution) and `ConflictResolver` (last-write-wins based on `updatedAt`). Created `SyncWorker` (`@HiltWorker`) for periodic background sync (every 15 min when network available). Updated `NoteRepository` and `TaskRepository` to set appropriate `syncStatus` on all operations. Created `ui/screens/auth/` package with `LoginScreen`, `RegisterScreen`, and `AuthViewModel`. Login screen has email/password fields, forgot password link, register link, and "continue without account" option. Register screen has email, password, confirm password with validation. Created `ui/screens/settings/` package with `SettingsScreen` and `SettingsViewModel`. Settings shows account status (logged in email or login prompt), sync status with last sync time, manual "Sync now" button, and logout with confirmation dialog. Added settings icon to `NotesScreen` top bar. Updated `Screen.kt` with `Login`, `Register`, `Settings` routes. Updated `AppNavHost.kt` with auth and settings screen composables. Updated `ProjekatApplication` to schedule `SyncWorker`. NOTE: User must create Firebase project, enable Email/Password auth, create Firestore database, and add `google-services.json` to `app/` folder.

### Still TODO (next steps):
- FAZA 6: Testing and polish
- Verify everything builds correctly in Android Studio (user needs to rebuild with Gradle sync)
- Create Firebase project and add google-services.json for cloud sync to work
- Verify the app compiles and runs without errors

---

## Planned Features (Implementation Order)

### FEATURE 1: Checklist in Notes/Tasks
**Status:** Completed  
**Complexity:** Medium  
**Estimated Time:** 2-3 days

**Description:** Add checklist items (with checkboxes) inside notes and tasks. Each item has text, checkbox state, and order.

**Data Model Changes:**
```kotlin
// New model: ChecklistItem.kt
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
    val order: Int = 0
)

// Note.kt - add field:
val checklistItems: List<ChecklistItem> = emptyList()

// Task.kt - add field:
val checklistItems: List<ChecklistItem> = emptyList()
```

**Room Migration (v4 → v5):**
- Add `checklistItems TEXT NOT NULL DEFAULT '[]'` to notes table
- Add `checklistItems TEXT NOT NULL DEFAULT '[]'` to tasks table
- Add TypeConverter for `List<ChecklistItem>` ↔ JSON string

**New Files:**
- `data/model/ChecklistItem.kt` — Data class
- `ui/components/ChecklistEditor.kt` — Reusable checklist UI component

**Files to Modify:**
- `data/model/Note.kt` — Add checklistItems field
- `data/model/Task.kt` — Add checklistItems field
- `data/local/Converters.kt` — Add TypeConverter
- `data/local/AppDatabase.kt` — Add MIGRATION_4_5
- `ui/screens/notes/NoteDetailScreen.kt` — Add checklist section
- `ui/screens/notes/NoteDetailViewModel.kt` — Add checklist management
- `ui/screens/tasks/TaskDetailScreen.kt` — Add checklist section
- `ui/screens/tasks/TaskDetailViewModel.kt` — Add checklist management

**UI Features:**
- Add new checklist item (+ button)
- Toggle checkbox (tap)
- Edit item text inline
- Delete item (X button or swipe)
- Reorder items (drag handle) — optional

---

### FEATURE 2: Shake to Undo
**Status:** Completed  
**Complexity:** Medium  
**Estimated Time:** 1-2 days

**Description:** When user shakes the phone while editing a note/task, show "Poništi izmene?" dialog to discard unsaved changes and revert to last saved state.

**Architecture:**
```
Sensor API → ShakeDetector → ViewModel → UI Dialog
```

**New Files:**
- `util/ShakeDetector.kt` — Shake detection utility using SensorManager + TYPE_ACCELEROMETER
- `ui/components/UndoDialog.kt` — Confirmation dialog composable

**Files to Modify:**
- `ui/screens/notes/NoteDetailScreen.kt` — Add shake listener, show dialog
- `ui/screens/notes/NoteDetailViewModel.kt` — Add undo state, revert method
- `ui/screens/tasks/TaskDetailScreen.kt` — Add shake listener, show dialog
- `ui/screens/tasks/TaskDetailViewModel.kt` — Add undo state, revert method

**ShakeDetector Implementation:**
- Use `SensorManager` + `Sensor.TYPE_ACCELEROMETER`
- Threshold-based detection (~12 m/s²)
- Cooldown period to prevent multiple triggers
- Callback interface for shake events
- Lifecycle-aware (register/unregister with screen)

**UI Flow:**
1. User is editing note/task
2. User shakes phone
3. Dialog appears: "Poništi izmene?" with "Da" / "Ne" buttons
4. "Da" → Revert to last saved state
5. "Ne" → Dismiss dialog, keep current edits

**Dependencies:** None (uses Android Sensor API)

---

### FEATURE 3: Location-based Task Notifications (Geofencing)
**Status:** Completed  
**Complexity:** High  
**Estimated Time:** 3-5 days

**Description:** Attach a location to a task, get notified when near that location.

**Data Model Changes:**
```kotlin
// Task.kt - add fields:
val locationLat: Double? = null,
val locationLng: Double? = null,
val locationName: String? = null,
val locationRadius: Int = 100  // meters
```

**Room Migration (v5 → v6):**
- Add `locationLat REAL` to tasks table
- Add `locationLng REAL` to tasks table
- Add `locationName TEXT` to tasks table
- Add `locationRadius INTEGER NOT NULL DEFAULT 100` to tasks table

**New Dependencies:**
```toml
# libs.versions.toml
playServicesLocation = "21.3.0"

# build.gradle.kts
implementation("com.google.android.gms:play-services-location:21.3.0")
```

**Permissions (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**New Files:**
- `location/GeofenceManager.kt` — Add/remove/update geofences
- `location/GeofenceBroadcastReceiver.kt` — Handle GEOFENCE_TRANSITION_ENTER
- `location/LocationPermissionHelper.kt` — Runtime permission handling
- `ui/components/LocationPicker.kt` — Location selection UI
- `notification/LocationNotificationHelper.kt` — Geofence notifications

**Files to Modify:**
- `data/model/Task.kt` — Add location fields
- `data/local/AppDatabase.kt` — Add MIGRATION_5_6
- `ui/screens/tasks/TaskDetailScreen.kt` — Add location section
- `ui/screens/tasks/TaskDetailViewModel.kt` — Add location management
- `AndroidManifest.xml` — Add permissions, register receiver
- `ProjekatApplication.kt` — Create notification channel for location alerts

**Location Picker (Address Search + Current Location):**
- "Koristi trenutnu lokaciju" button (FusedLocationProviderClient)
- Search field for address (Geocoder API)
- Selected location preview (name + coordinates)
- Radius slider (50m - 500m)
- No Google Maps API key required

**Geofencing Flow:**
1. User adds location to task in TaskDetailScreen
2. GeofenceManager.addGeofenceForTask() called on save
3. When user enters geofence radius → BroadcastReceiver triggered
4. Notification: "Blizu ste lokacije za task: [Task Title]"
5. Tap notification → Opens task detail

**Background Location (Android 10+):**
- Request `ACCESS_BACKGROUND_LOCATION` separately (Android 11+)
- Show explanation dialog before requesting
- Handle permission denial gracefully

---

### FEATURE 4: Firebase Authentication + Cloud Sync
**Status:** Planned  
**Complexity:** High  
**Estimated Time:** 1-2 weeks

**Description:** User accounts with email/password login, data synced to Firebase Firestore when logged in. Offline-first architecture.

**Authentication:** Email/Password only

**Architecture:**
```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer                                                   │
│  ├── LoginScreen, RegisterScreen, ProfileScreen            │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ├── AuthRepository (Firebase Auth)                         │
│  ├── SyncManager (orchestrates sync)                       │
│  ├── NoteRepository (local + cloud aware)                  │
│  └── TaskRepository (local + cloud aware)                  │
├─────────────────────────────────────────────────────────────┤
│  Local: Room Database    │    Remote: Firebase Firestore   │
│  ├── notes               │    ├── users/{uid}/notes        │
│  └── tasks               │    └── users/{uid}/tasks        │
└─────────────────────────────────────────────────────────────┘
```

**Firebase Setup:**
1. Create Firebase project in Firebase Console
2. Enable Email/Password authentication
3. Create Firestore database
4. Download `google-services.json` to app/
5. Add Firebase dependencies

**New Dependencies:**
```toml
# libs.versions.toml
firebaseBom = "33.7.0"
googleServices = "4.4.2"

# build.gradle.kts (project)
id("com.google.gms.google-services") version "4.4.2" apply false

# build.gradle.kts (app)
id("com.google.gms.google-services")
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
```

**Data Model Changes:**
```kotlin
// Note.kt - add fields:
val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
val cloudId: String? = null

// Task.kt - add fields:
val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
val cloudId: String? = null

// New enum
enum class SyncStatus {
    LOCAL_ONLY,      // Not synced yet
    SYNCED,          // In sync with cloud
    PENDING_UPLOAD,  // Local changes need upload
    PENDING_DELETE   // Marked for deletion on cloud
}
```

**Room Migration (v6 → v7):**
- Add `syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'` to notes/tasks
- Add `cloudId TEXT` to notes/tasks

**Firestore Structure:**
```
users/
└── {userId}/
    ├── notes/
    │   └── {noteId}/
    │       ├── title, content, imageUrls, colorIndex
    │       ├── isBookmarked, isDeleted, checklistItems
    │       ├── createdAt, updatedAt, deletedAt
    │       └── deviceId (for conflict resolution)
    └── tasks/
        └── {taskId}/
            ├── title, description, status, priority
            ├── deadline, noteId, colorIndex, checklistItems
            ├── locationLat, locationLng, locationName, locationRadius
            ├── createdAt, updatedAt
            └── deviceId (for conflict resolution)
```

**New Files:**
```
data/
├── repository/
│   ├── AuthRepository.kt           # Firebase Auth operations
│   ├── CloudNoteRepository.kt      # Firestore note operations
│   └── CloudTaskRepository.kt      # Firestore task operations
└── sync/
    ├── SyncManager.kt              # Orchestrates sync
    ├── SyncStatus.kt               # Enum for sync states
    └── ConflictResolver.kt         # Last-write-wins based on updatedAt

di/
└── FirebaseModule.kt               # Hilt module for Firebase

worker/
├── NoteSyncWorker.kt               # Background sync for notes
└── TaskSyncWorker.kt               # Background sync for tasks

ui/screens/auth/
├── LoginScreen.kt                  # Email/password login
├── RegisterScreen.kt               # User registration
└── AuthViewModel.kt                # Auth state management

ui/screens/settings/
├── SettingsScreen.kt               # Sync settings, logout
└── SettingsViewModel.kt            # Settings state
```

**Files to Modify:**
- `navigation/Screen.kt` — Add auth routes
- `navigation/AppNavHost.kt` — Add auth screens
- `data/repository/NoteRepository.kt` — Cloud-aware operations
- `data/repository/TaskRepository.kt` — Cloud-aware operations
- `data/model/Note.kt` — Add sync fields
- `data/model/Task.kt` — Add sync fields
- `data/local/AppDatabase.kt` — Add MIGRATION_6_7
- `data/local/Converters.kt` — Add SyncStatus TypeConverter
- `ProjekatApplication.kt` — Initialize Firebase
- `MainActivity.kt` — Auth state handling
- App bar — Add user avatar/login button

**Sync Strategy:**
- **Offline-first:** All operations write to Room first
- **Background sync:** WorkManager syncs when online
- **Conflict resolution:** Last-write-wins based on `updatedAt`
- **Image upload:** Upload to Firebase Storage, store URL in Firestore

**UI Updates:**
- Login/Register screens with validation
- User avatar in app bar (or login button if not authenticated)
- Sync status indicator (syncing spinner, last synced time)
- "Sync now" button in settings
- "Offline mode" banner when no internet
- Logout with confirmation dialog

### FEATURE 5: Flexible Dates & Time
**Status:** Completed  
**Complexity:** Medium

**Description:** Replaced single `deadline` with `startDate`, `endDate`, and `hasTime`. Tasks can span multiple days and optionally have a specific time.

### FEATURE 6: Repeating Tasks
**Status:** Completed  
**Complexity:** High

**Description:** Added `RepeatInterval` (DAILY, WEEKLY, MONTHLY, YEARLY, NONE) and `repeatEndDate`. When a repeating task is marked COMPLETED, the app calculates the next occurrence and creates a new task while keeping the history of the completed one.

---

### FEATURE 7: Task Sharing & Collaboration (Firebase)
**Status:** Completed  
**Complexity:** High  

**Description:** Allow users to share tasks with others or assign tasks directly. Modeled simply like Apple Notes sharing. Users can collaborate (all can edit/complete). Shared tasks go to an "Inbox/Invites" tab where users must accept/decline.

**Implementation Details:**
- **Data Model:** Added `ownerId: String?`, `sharedWith: List<String>`, and `pendingInvites: List<String>` to `Task.kt`. Added database migration `MIGRATION_9_10`.
- **Firebase Refactoring:** Updated `CloudTaskRepository` and `SyncManager` to sync shared tasks. Migrated to a top-level `/shared_tasks` collection on Firestore to automatically index arrays and avoid manual Collection Group indexing.
- **Firestore Rules Update Required:**
  ```javascript
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId}/{collection=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      match /shared_tasks/{taskId} {
        allow read, write: if request.auth != null;
      }
    }
  }
  ```
- **UI:** 
  - Added "Inbox" tab to `TasksScreen` with a badge showing pending invite counts.
  - Implemented `InviteCard` with "Prihvati" (Accept) and "Odbij" (Decline) actions in `TasksScreen`.
  - Added "Share" button to `TaskDetailScreen` top app bar opening a dialog where users can invite collaborators by email and view current access lists.
- **SyncManager:** Correctly handles updating tasks when invites are accepted or declined, using the new `shared_tasks` path.

---

### FEATURE 8: Smarter AI Interaction
**Status:** Completed
**Complexity:** Medium  

**Description:** Allow partial acceptance of AI scheduling and flexible custom prompts.

**Implementation Details:**
- **Partial Acceptance:** Add checkboxes to each task suggested in `SchedulePreviewDialog`. Only checked tasks will have their dates updated when clicking "Primeni".
- **Flexible Prompts:** Add a text input field ("Dodatne instrukcije za AI...") in the AI dialog.
- **API Logic:** Append the user's custom instructions to the system prompt in `PollinationsAiClient`.

---

### FEATURE 9: Extended Statuses & 1-10 Priority Scale
**Status:** Completed  
**Complexity:** Medium  

**Description:** Added `PAUSED` and `CANCELED` statuses. Changed priority to 1-10 scale with color indicators.

**Implementation Details:**
- **Data Model:** Added `CANCELED` and `PAUSED` to `TaskStatus`. Replaced `priority` enum with `priorityScore: Int = 5` in `Task.kt`.
- **Database Migration:** Created `MIGRATION_11_12` replacing the enum with integers (LOW=3, MEDIUM=5, HIGH=8).
- **UI:** 
  - Replaced 3-chip priority selector with a 1-10 Slider in `TaskDetailScreen`.
  - Updated `TaskCard` to display color-coded priority badge (green=low, orange=mid, red=high) based on `priorityScore`.
  - Added `PAUSED` and `CANCELED` status chips to `TaskDetailScreen` alongside existing `IN_PROGRESS` and `COMPLETED`.
  - Updated stats row in `TasksScreen` to show 4 cards (U toku, Pauzirano, Zavrseno, Otkazano).
  - Updated `TaskCard` status color and overdue logic to handle all 4 statuses.
  - Added `StatusPaused` (blue-grey) and `StatusCanceled` (light grey) colors.
- **Toggle Logic:** `toggleTaskStatus` in both `TaskRepository` and `TasksViewModel` handles all 4 statuses (PAUSED/CANCELED → IN_PROGRESS).
- **AI Prompt:** The `TaskItem` priority field now sends the numeric score (1-10) as a string to the AI.

---

## Room Migrations Summary

| Version | Changes |
|---------|---------|
| v1 | Initial schema (notes with imageUri) |
| v2 | +imageUris (List) to notes, migrate single image |
| v3 | +colorIndex to tasks |
| v4 | +priority to tasks (current) |
| v5 | +checklistItems to notes and tasks |
| v6 | +locationLat/Lng/Name/Radius to tasks |
| v7 | +syncStatus, +cloudId to notes and tasks |
| v8 | +startDate, +endDate, +hasTime to tasks |
| v9 | +repeatInterval, +repeatEndDate, +lastCompletedAt to tasks |
| v10 | +ownerId, +sharedWith, +pendingInvites to tasks |
| v11 | +ownerEmail to tasks |
| v12 | +priorityScore to tasks, migrate old priority enum to integers |

---

## Dependencies Summary (After All Features)

```toml
# libs.versions.toml

[versions]
# ... existing versions ...
playServicesLocation = "21.3.0"
firebaseBom = "33.7.0"
googleServices = "4.4.2"

[libraries]
# ... existing libraries ...

# Play Services Location
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-storage-ktx = { group = "com.google.firebase", name = "firebase-storage-ktx" }

[plugins]
# ... existing plugins ...
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

---

## Final Project Structure (After All Features)

```
app/src/main/java/com/example/projekat/
├── ProjekatApplication.kt                   # +Firebase init
├── MainActivity.kt                          # +Auth state handling
├── data/
│   ├── ai/
│   │   ├── AiScheduleModels.kt
│   │   ├── AiScheduleService.kt
│   │   └── PollinationsAiClient.kt
│   ├── local/
│   │   ├── AppDatabase.kt                   # +Migrations v5, v6, v7, v8
│   │   ├── Converters.kt                    # +ChecklistItem, SyncStatus converters
│   │   ├── NoteDao.kt
│   │   └── TaskDao.kt
│   ├── model/
│   │   ├── Note.kt                          # +checklistItems, syncStatus, cloudId
│   │   ├── Task.kt                          # +checklistItems, location fields, syncStatus, cloudId
│   │   ├── ChecklistItem.kt                 # [NEW]
│   │   └── SyncStatus.kt                    # [NEW]
│   ├── repository/
│   │   ├── AiScheduleRepository.kt
│   │   ├── AuthRepository.kt                # [NEW]
│   │   ├── CloudNoteRepository.kt           # [NEW]
│   │   ├── CloudTaskRepository.kt           # [NEW]
│   │   ├── NoteRepository.kt                # +cloud-aware
│   │   └── TaskRepository.kt                # +cloud-aware
│   └── sync/
│       ├── SyncManager.kt                   # [NEW]
│       └── ConflictResolver.kt              # [NEW]
├── di/
│   ├── AiModule.kt
│   ├── DatabaseModule.kt
│   ├── FirebaseModule.kt                    # [NEW]
│   └── LocationModule.kt                    # [NEW]
├── location/
│   ├── GeofenceManager.kt                   # [NEW]
│   ├── GeofenceBroadcastReceiver.kt         # [NEW]
│   └── LocationPermissionHelper.kt          # [NEW]
├── navigation/
│   ├── Screen.kt                            # +auth, settings routes
│   └── AppNavHost.kt                        # +auth, settings screens
├── notification/
│   ├── DeadlineScheduler.kt
│   └── LocationNotificationHelper.kt        # [NEW]
├── util/
│   └── ShakeDetector.kt                     # [NEW]
├── worker/
│   ├── CleanupWorker.kt
│   ├── DeadlineWorker.kt
│   ├── NoteSyncWorker.kt                    # [NEW]
│   └── TaskSyncWorker.kt                    # [NEW]
└── ui/
    ├── components/
    │   ├── BottomNavigationBar.kt
    │   ├── SwipeBackContainer.kt
    │   ├── ChecklistEditor.kt               # [NEW]
    │   ├── LocationPicker.kt                # [NEW]
    │   └── UndoDialog.kt                    # [NEW]
    ├── screens/
    │   ├── auth/                            # [NEW PACKAGE]
    │   │   ├── LoginScreen.kt
    │   │   ├── RegisterScreen.kt
    │   │   └── AuthViewModel.kt
    │   ├── notes/
    │   │   ├── NotesScreen.kt
    │   │   ├── NotesViewModel.kt
    │   │   ├── NoteDetailScreen.kt          # +checklist, +shake
    │   │   └── NoteDetailViewModel.kt       # +checklist, +undo
    │   ├── tasks/
    │   │   ├── TasksScreen.kt
    │   │   ├── TasksViewModel.kt
    │   │   ├── TaskDetailScreen.kt          # +checklist, +location, +shake
    │   │   └── TaskDetailViewModel.kt       # +checklist, +location, +undo
    │   ├── calendar/
    │   │   ├── CalendarScreen.kt
    │   │   └── CalendarViewModel.kt
    │   └── settings/                        # [NEW PACKAGE]
    │       ├── SettingsScreen.kt
    │       └── SettingsViewModel.kt
    └── theme/
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt
```

## Project Structure

```
app/src/main/java/com/example/projekat/
├── ProjekatApplication.kt                   # @HiltAndroidApp Application class, WorkManager scheduling
├── MainActivity.kt                          # @AndroidEntryPoint, NavController, outer Scaffold, bottom bar
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt                   # Room database (notes + tasks tables)
│   │   ├── Converters.kt                    # Room TypeConverters (TaskStatus enum)
│   │   ├── NoteDao.kt                       # DAO for notes (CRUD, search, filter, cleanup)
│   │   └── TaskDao.kt                       # DAO for tasks (CRUD, filter by status/day)
│   ├── model/
│   │   ├── Note.kt                          # @Entity data class
│   │   └── Task.kt                          # @Entity data class + TaskStatus enum
│   ├── ai/
│   │   ├── AiScheduleModels.kt              # Data models for AI scheduling (TaskItem, ScheduledTask, etc.)
│   │   ├── PollinationsAiClient.kt          # Direct OkHttp client for Pollinations.ai API
│   │   └── AiScheduleService.kt             # Business logic layer for AI scheduling
│   └── repository/
│       ├── AiScheduleRepository.kt          # AI scheduling business logic (calls AiScheduleService, maps results)
│       ├── NoteRepository.kt                # Business logic for notes
│       └── TaskRepository.kt                # Business logic for tasks
├── di/
│   ├── DatabaseModule.kt                    # Hilt module providing AppDatabase, NoteDao, TaskDao
│   └── AiModule.kt                          # Hilt module providing PollinationsAiClient, AiScheduleService
├── navigation/
│   ├── Screen.kt                            # Sealed class with all routes
│   └── AppNavHost.kt                        # NavHost with all composable destinations
└── ui/
    ├── components/
    │   ├── BottomNavigationBar.kt           # 3-tab bottom nav (Beleske, Taskovi, Kalendar)
    │   └── SwipeBackContainer.kt            # Reusable swipe-right-to-go-back wrapper
    ├── screens/
    │   ├── notes/
    │   │   ├── NotesScreen.kt               # Notes list with search, drawer, staggered grid
    │   │   ├── NotesViewModel.kt            # ViewModel: filter, search, bookmark, soft-delete
    │   │   ├── NoteDetailScreen.kt          # Note create/edit form with save
    │   │   └── NoteDetailViewModel.kt       # ViewModel: load/save/delete note
    │   ├── tasks/
    │   │   ├── TasksScreen.kt               # Tasks list with stats cards
    │   │   ├── TasksViewModel.kt            # ViewModel: task list, toggle status
    │   │   ├── TaskDetailScreen.kt          # Task create/edit form with DatePicker
    │   │   └── TaskDetailViewModel.kt       # ViewModel: load/save/delete task, attach note
    │   └── calendar/
    │       ├── CalendarScreen.kt            # Calendar grid + task previews
    │       └── CalendarViewModel.kt         # ViewModel: tasks with deadlines
    └── theme/
        ├── Color.kt                         # Full color palette
        ├── Type.kt                          # Full Typography definitions
        └── Theme.kt                         # Light/dark color schemes, status bar
├── notification/
│   └── DeadlineScheduler.kt                 # Hilt @Singleton for scheduling/cancelling deadline notifications
├── worker/
│   ├── CleanupWorker.kt                     # @HiltWorker periodic cleanup of soft-deleted notes
│   └── DeadlineWorker.kt                    # @HiltWorker fires notification when task deadline expires
```

## Key Config Files
- `gradle/libs.versions.toml` — Version catalog with all dependency versions (Room, Hilt, KSP, Coroutines, etc.)
- `build.gradle.kts` — Root build config with KSP and Hilt plugins
- `app/build.gradle.kts` — App-level build config with dependencies
- `app/src/main/AndroidManifest.xml` — Manifest with `android:name=".ProjekatApplication"`

## Key Dependencies
- **Room** 2.6.1 — Local SQLite database (entities, DAOs, database)
- **Hilt** 2.51.1 — Dependency injection (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **KSP** 2.0.21-1.0.27 — Kotlin Symbol Processing for Room + Hilt annotation processing
- **Hilt Navigation Compose** 1.2.0 — `hiltViewModel()` in composables
- **Coroutines** 1.8.1 — Async operations (Flow, suspend functions)
- **Coil** 2.6.0 — Image loading library for Compose (AsyncImage)
- **WorkManager** 2.9.1 — Background periodic task scheduling (7-day cleanup)
- **Hilt Work** 1.2.0 — Hilt integration for WorkManager (`@HiltWorker`, `HiltWorkerFactory`)
- **OkHttp** 4.12.0 — HTTP client for direct API calls to Pollinations.ai (with logging interceptor)
- **Gson** 2.10.1 — JSON parsing for AI API requests/responses

## Screenshots
- `Screenshot 2026-02-28 211429.png` — Notes screen (showed excessive top padding before fix)
- `Screenshot 2026-02-28 211450.png` — Tasks screen
- `Screenshot 2026-02-28 211500.png` — Calendar screen



## Goal
Obezbediti funkcije za "Smarter AI Interaction" i nakon toga početi Feature 9.

## Instructions
- Feature 8 završen (dodate opcije za custom prompt i odabir pojedinačnih taskova za primenu).

## Accomplished
### Uradjeno:
- **Feature 7:** Završen. UI u Inboxu sada prikazuje email pošiljaoca.
- **Feature 8:** Završen. `AiPromptDialog` omogucava unos opcionih instrukcija (custom prompt) pre slanja zahteva. Prilagodjen je `PollinationsAiClient` da iskoristi custom instrukcije. U `SchedulePreviewDialog` dodati su checkboxovi za partial acceptance, cime se omogucuje primena samo izabranih rasporeda preko prosledjivanja izabranih task id-ijeva.
- **Feature 9:** Završen. Dodati `PAUSED` i `CANCELED` statusi. Prioritet prebacen sa `TaskPriority` enuma na `priorityScore: Int = 1..10`. UI azuriran: Slider za prioritet umesto 3 chipa, 4 status chip-a, 4 stat kartice, TaskCard prikazuje boju na osnovu `priorityScore`.
- **AI task selection filter:** Selection mode now hides ineligible tasks instead of dimming them (`TasksScreen.kt`).
- **Color conflict fix:** Status colors (`StatusInProgress` → blue, `StatusCompleted` → green) and priority colors (`PriorityHigh` → dark red, `PriorityMedium` → amber, `PriorityLow` → cyan) no longer overlap in `Color.kt`.
- **Removed strikethrough:** Completed task titles no longer get crossed out in `TasksScreen` and `CalendarScreen`.
- **InviteCard redesign:** Inbox task cards now use the same color-coded background, dynamic text colors, and primary-colored accept button (instead of hardcoded white/grey/green).
- **Pull-to-refresh sync:** Added `PullToRefreshBox` to `TasksScreen` — pulling down triggers `SyncManager.syncAll()` to sync with Firebase, with a spinner indicator during sync.
- **SchedulePreviewDialog redesign:** Cards now uniform in collapsed state (checkbox + task name + compact date chips) with "Vise detalja"/"Manje detalja" expand button revealing detailed original/AI date sections. Uses `animateContentSize()` and `AnimatedVisibility` for smooth transitions. Selected cards use `primaryContainer` color.
- **compileSdk fix in app/build.gradle.kts:** Changed `compileSdk { version = release(36) }` to `compileSdk = 36` to fix Android Studio Live Edit crash (`IllegalFormatConversionException: d != AndroidApiLevel`).
- **PollinationsAiClient retry + model fallback:** Added exponential backoff retry for 429/504 errors. Now tries `openai` first, falls back to `mistral` if first model fails. Read timeout increased from 90s to 120s. Prompt shortened significantly for faster AI processing.
- **Fixed endDateTime in AiScheduleRepository:** Removed fallback that set `endDateTime = startDateTime` when `endDate` was null — now passes `null` properly so AI isn't confused by meaningless same-day end dates.
- **Local scheduling fallback in AiScheduleService:** When Pollinations.ai fails (429/504/any error), falls back to a local algorithm that sorts tasks by priority and spaces them evenly from tomorrow onward. Users never see an error — always get a schedule result.

# last feature
# ongoing
the goal is to make it so that when user1 sends user2 a task. it is not needed for user1 to sync and then user2 to sync so that user2 can see the new task in inbox screen rather when user1 sends the task with the help of fcm user2 gets notified (the task appears automatically in the inbox screen).

Here's the plan for sending FCM notifications directly from the Android app, no Cloud Functions needed.

How it works
Instead of a server triggering the notification, User A's app:
Looks up User B's FCM token from Firestore
Calls the FCM HTTP API directly with that token
User B's device receives the push and auto-syncs

Step 1 — Get your FCM Server Key
Go to Firebase Console → Project Settings → Cloud Messaging tab → find Server key under "Cloud Messaging API (Legacy)". Copy it.
Note: you'll use the legacy HTTP API (https://fcm.googleapis.com/fcm/send) since the newer API requires a service account/server environment.

Step 2 — Store FCM tokens in Firestore
When a user logs in or the token refreshes, save it to Firestore. In your AuthRepository.kt, after successful login/register add:
kotlin
suspend fun saveFcmToken() {
    val uid = auth.currentUser?.uid ?: return
    val token = FirebaseMessaging.getInstance().token.await()
    firestore.collection("users").document(uid)
        .set(mapOf("fcmToken" to token, "email" to auth.currentUser?.email), SetOptions.merge())
        .await()
}
Call saveFcmToken() after login and after register in AuthViewModel.

Step 3 — Create FcmService.kt for token refresh
New file notification/FcmService.kt:
kotlin
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var syncManager: SyncManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save refreshed token to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.saveFcmToken()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Auto-sync when notification arrives
        CoroutineScope(Dispatchers.IO).launch {
            syncManager.syncAll()
        }
        // Show local notification
        showNotification(
            title = message.notification?.title ?: "Novi zadatak u Inboxu",
            body = message.notification?.body ?: ""
        )
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("openInbox", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "inbox_notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
Register in AndroidManifest.xml:
xml
<service
    android:name=".notification.FcmService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT"/>
    </intent-filter>
</service>

Step 4 — Create FcmNotificationSender.kt
New file notification/FcmNotificationSender.kt. This calls the FCM HTTP API directly using your existing OkHttp dependency:
kotlin
@Singleton
class FcmNotificationSender @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val client = OkHttpClient()
    private val serverKey = "YOUR_SERVER_KEY_HERE" // from Firebase Console

    suspend fun notifyTaskShared(
        recipientEmail: String,
        taskTitle: String,
        senderEmail: String,
        taskId: String
    ) {
        // 1. Look up recipient's UID by email
        val userQuery = firestore.collection("users")
            .whereEqualTo("email", recipientEmail)
            .get()
            .await()

        if (userQuery.isEmpty) return

        val recipientDoc = userQuery.documents.first()
        val fcmToken = recipientDoc.getString("fcmToken") ?: return

        // 2. Send FCM notification
        val json = JSONObject().apply {
            put("to", fcmToken)
            put("notification", JSONObject().apply {
                put("title", "Novi zadatak u Inboxu")
                put("body", "$senderEmail vam je poslao: $taskTitle")
            })
            put("data", JSONObject().apply {
                put("taskId", taskId)
            })
            put("android", JSONObject().apply {
                put("priority", "high")
            })
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .addHeader("Authorization", "key=$serverKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().close()
        }
    }
}

Step 5 — Wire it into CloudTaskRepository
In CloudTaskRepository.kt, inject FcmNotificationSender and call it when sharing a task. Find where pendingInvites gets updated (when the share dialog confirms) and add:
kotlin
// After saving the task to Firestore with new pendingInvites:
newInviteeEmails.forEach { email ->
    launch {
        fcmNotificationSender.notifyTaskShared(
            recipientEmail = email,
            taskTitle = task.title,
            senderEmail = currentUserEmail,
            taskId = task.id
        )
    }
}

Step 6 — Add notification channel
In ProjekatApplication.onCreate(), add alongside your existing channels:
kotlin
NotificationChannel(
    "inbox_notifications",
    "Inbox obaveštenja",
    NotificationManager.IMPORTANCE_HIGH
)

Step 7 — Handle inbox open on notification tap
In MainActivity.kt, after setContent, check for the intent extra:
kotlin
if (intent.getBooleanExtra("openInbox", false)) {
    // navigate to Tasks tab, Inbox section
    navController.navigate(Screen.Tasks.route)
}

Step 8 — Update Firestore rules
Make sure users can read other users' tokens (needed for the lookup in Step 4). Update the users rule:
javascript
match /users/{userId} {
  // Owner can read/write their own doc
  allow write: if request.auth != null && request.auth.uid == userId;
  // Any logged-in user can read (needed to look up FCM tokens by email)
  allow read: if request.auth != null;
}

Summary of new files
File
Purpose
notification/FcmService.kt
Token refresh + receive notifications + trigger sync
notification/FcmNotificationSender.kt
Look up token + call FCM HTTP API
AuthRepository.kt
Add saveFcmToken()
CloudTaskRepository.kt
Call sender after sharing
ProjekatApplication.kt
Add inbox notification channel
AndroidManifest.xml
Register FcmService
The only real security tradeoff vs Cloud Functions is that the server key lives in your APK. For a university project this is completely fine — just don't publish it to the Play Store with a hardcoded key.

Legacy API is disabled — need to use V1 API instead
The plan I gave you used the legacy FCM HTTP API (fcm.googleapis.com/fcm/send) which is disabled on your project. You need to use the V1 API instead, which means the FcmNotificationSender.kt needs to change significantly — the V1 API requires an OAuth2 access token, not a server key.
The cleanest way to get that access token from Android is to use a service account JSON. Here's the updated approach:
Go to Firebase Console → Project Settings → Service Accounts → click "Generate new private key" — this downloads a JSON file. Put it in app/src/main/assets/service_account.json.
Then update FcmNotificationSender.kt to use the V1 endpoint with a JWT token generated from that service account. This gets quite complex on Android though.
Honestly the simpler path now: enable the Legacy API. Click the three-dot menu (⋮) next to "Cloud Messaging API (Legacy) — Disabled" and enable it. Then the original FcmNotificationSender.kt code works as-is with the server key. The legacy API still works fine, it's just not recommended for new projects — for a university project it's perfectly fine.
You said: ~/faks/pmu/ProjekatPMU/app/src/main master*
~/faks/pmu/ProjekatPMU/app/src/main master* ❯ ls drwxr-xr-x - lazarstanivukovic 2 Apr 21:39  java drwxr-xr-x - lazarstanivukovic 2 Apr 21:39  res .rw-r--r-- 2.7k lazarstanivukovic 4 Apr 20:27 󰗀 AndroidManifest.xml i dont have app/src/main/assets and i cant activate cloud messaging api legacy because its deprecated
6:40 PM
Claude responded: No problem.
No problem. Create the assets folder first:
bash
mkdir -p app/src/main/assets
Then put the service_account.json file there.
Now update FcmNotificationSender.kt to use the V1 API with a JWT from the service account. You'll also need to add one dependency — add to app/build.gradle.kts:
kotlin
implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
Then FcmNotificationSender.kt:
kotlin
@Singleton
class FcmNotificationSender @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient()
    private val projectId = "projekat-1414d"

    private fun getAccessToken(): String {
        val stream = context.assets.open("service_account.json")
        val credentials = GoogleCredentials
            .fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    suspend fun notifyTaskShared(
        recipientEmail: String,
        taskTitle: String,
        senderEmail: String,
        taskId: String
    ) {
        // Look up recipient's FCM token by email
        val userQuery = firestore.collection("users")
            .whereEqualTo("email", recipientEmail)
            .get()
            .await()

        if (userQuery.isEmpty) return
        val fcmToken = userQuery.documents.first().getString("fcmToken") ?: return

        // Get OAuth2 access token from service account
        val accessToken = withContext(Dispatchers.IO) { getAccessToken() }

        // Build V1 API request
        val json = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", fcmToken)
                put("notification", JSONObject().apply {
                    put("title", "Novi zadatak u Inboxu")
                    put("body", "$senderEmail vam je poslao: $taskTitle")
                })
                put("data", JSONObject().apply {
                    put("taskId", taskId)
                })
                put("android", JSONObject().apply {
                    put("priority", "high")
                })
            })
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().close()
        }
    }
}
The key differences from before: uses /v1/projects/projekat-1414d/messages:send instead of the legacy endpoint, wraps the payload in a "message" object, and authenticates with a Bearer token generated from the service account JSON instead of a hardcoded server key.
