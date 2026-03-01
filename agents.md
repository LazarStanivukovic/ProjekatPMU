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

### Still TODO (next steps):
- Verify everything builds correctly in Android Studio (user needs to rebuild with Gradle sync)
- Implement soft-delete with 7-day auto-cleanup (logic exists in repository, needs scheduling via WorkManager)
- Implement notifications for deadline expiry

## Project Structure

```
app/src/main/java/com/example/projekat/
├── ProjekatApplication.kt                   # @HiltAndroidApp Application class
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
│   └── repository/
│       ├── NoteRepository.kt                # Business logic for notes
│       └── TaskRepository.kt                # Business logic for tasks
├── di/
│   └── DatabaseModule.kt                    # Hilt module providing AppDatabase, NoteDao, TaskDao
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

## Screenshots
- `Screenshot 2026-02-28 211429.png` — Notes screen (showed excessive top padding before fix)
- `Screenshot 2026-02-28 211450.png` — Tasks screen
- `Screenshot 2026-02-28 211500.png` — Calendar screen
