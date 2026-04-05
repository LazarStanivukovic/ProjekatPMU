# NoteApp

A modern Android note-taking and task management application built with Kotlin and Jetpack Compose. Features cloud sync, AI-powered scheduling, and offline-first architecture.

## Features

### Notes
- Create and edit notes with rich content
- Add multiple images from camera or gallery
- Checklist support with add/edit/toggle functionality
- Color-coded backgrounds (6 colors)
- Bookmark important notes
- Soft-delete with 7-day auto-cleanup
- Search and filter (All, Bookmarked, Deleted)

### Tasks
- Create tasks with title, description, and deadlines
- Priority levels (High, Medium, Low)
- Status tracking (In Progress, Completed)
- Link tasks to notes
- Location-based reminders with geofencing
- Checklist support
- Color-coded backgrounds

### Calendar
- Monthly calendar view with task indicators
- Swipe navigation between months
- Quick task preview on date selection

### AI Scheduling
- Intelligent task scheduling powered by Pollinations.ai
- Automatically suggests optimal scheduling based on priority and deadlines

### Cloud Sync
- Firebase Authentication (email/password)
- Real-time sync with Firestore
- Offline-first architecture with local Room database
- Automatic conflict resolution (last-write-wins)
- Image sync to Firebase Storage

### Additional Features
- Push notifications for task deadlines
- Dark/Light theme support
- Shake to undo changes
- Swipe-back navigation
- Edge-to-edge display

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Local Database | Room |
| Dependency Injection | Hilt |
| Navigation | Navigation Compose |
| Background Work | WorkManager |
| Networking | OkHttp |
| Image Loading | Coil |
| Location | Play Services Location |
| Backend | Firebase (Auth, Firestore, Storage) |
| Async | Kotlin Coroutines + Flow |

## Project Structure

```
app/src/main/java/com/example/projekat/
├── data/
│   ├── ai/                 # AI scheduling service (Pollinations.ai)
│   ├── local/              # Room database, DAOs, migrations
│   ├── model/              # Data models (Note, Task, ChecklistItem)
│   ├── repository/         # Repository layer (local + cloud)
│   └── sync/               # Sync manager and conflict resolution
├── di/                     # Hilt dependency injection modules
├── location/               # Geofencing for location reminders
├── navigation/             # Navigation routes and NavHost
├── notification/           # Deadline notification scheduling
├── util/                   # Utilities (ShakeDetector)
├── worker/                 # WorkManager jobs (sync, cleanup, deadlines)
└── ui/
    ├── components/         # Reusable UI components
    ├── screens/            # Screen composables and ViewModels
    └── theme/              # Material theme, colors, typography
```

## Requirements

- Android Studio Hedgehog or newer
- Min SDK 24 (Android 7.0)
- Target SDK 36
- Java 11

## Setup

1. Clone the repository
   ```bash
   git clone https://github.com/LazarStanivukovic/ProjekatPMU
   ```

2. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)

3. Enable the following Firebase services:
   - Authentication (Email/Password provider)
   - Cloud Firestore
   - Storage

4. Download `google-services.json` and place it in the `app/` directory

5. Open the project in Android Studio

6. Sync Gradle and build the project

## Permissions

The app requires the following permissions:

- `CAMERA` - For capturing images
- `INTERNET` - For cloud sync
- `POST_NOTIFICATIONS` - For deadline reminders
- `ACCESS_FINE_LOCATION` - For location-based reminders
- `ACCESS_COARSE_LOCATION` - For location-based reminders
- `ACCESS_BACKGROUND_LOCATION` - For geofencing

## Architecture

The app follows a clean architecture pattern with:

- **UI Layer**: Jetpack Compose screens with ViewModels
- **Domain Layer**: Repository interfaces
- **Data Layer**: Room database + Firebase services

### Offline-First Approach

1. All data is stored locally in Room database first
2. Changes are synced to Firebase when network is available
3. WorkManager handles periodic background sync (every 15 minutes)
4. Conflict resolution uses last-write-wins strategy

## Screenshots

*Coming soon*

## License

This project is for educational purposes as part of PMU (Programiranje Mobilnih Uredjaja) course.
