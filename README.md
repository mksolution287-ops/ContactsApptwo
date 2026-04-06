# Contacts App — Android (Kotlin + Jetpack Compose)

A polished, production-ready Android Contacts app built with **Kotlin** and **Jetpack Compose Material 3**, using **no version catalog** (pure `build.gradle` DSL).

---

## Features

| Feature | Details |
|---|---|
| **Contacts List** | Fetches all device contacts, alphabetically grouped with sticky headers, fast search |
| **Recent Calls** | Reads call log, groups by Today / Yesterday / date, shows type (incoming/outgoing/missed) with color coding |
| **Favorites Grid** | Shows starred contacts in a 3-column card grid |
| **Contact Detail** | Full detail view with avatar hero header, quick-action chips, phone/email/notes sections |
| **Add Contact** | Launches system contact editor via `Intent` |
| **Call** | Launches system dialer via `tel:` URI |
| **SMS** | Launches system messaging via `smsto:` URI |
| **Email** | Launches system email via `mailto:` URI |
| **Edit Contact** | Opens system contact editor for the selected contact |
| **Share Contact** | Shares vCard via Android share sheet |
| **Permissions** | Runtime permission requests for `READ_CONTACTS` and `READ_CALL_LOG` using Accompanist Permissions |
| **String Resources** | 100% externalized — zero hardcoded strings |
| **Theming** | Custom Material 3 color scheme (Indigo + Coral), dark/light mode support, edge-to-edge |
| **Animations** | Fade + slide entry animations, avatar scale-in, search bar transition |

---

## Tech Stack

- **Language**: Kotlin 1.9.24
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Navigation**: Navigation Compose
- **Image loading**: Coil
- **Permissions**: Accompanist Permissions
- **Coroutines**: `kotlinx.coroutines`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build system**: Gradle (no version catalog)

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/contactsapp/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── model/
│   │   │   ├── Contact.kt
│   │   │   └── CallLogEntry.kt
│   │   └── repository/
│   │       ├── ContactsRepository.kt
│   │       └── CallLogRepository.kt
│   ├── ui/
│   │   ├── Navigation.kt
│   │   ├── ContactsViewModel.kt
│   │   ├── components/
│   │   │   ├── ContactAvatar.kt
│   │   │   ├── EmptyState.kt
│   │   │   └── PermissionScreen.kt
│   │   ├── screens/
│   │   │   ├── MainScreen.kt          ← Bottom nav host
│   │   │   ├── ContactsScreen.kt      ← Contacts list + search
│   │   │   ├── RecentsScreen.kt       ← Call history
│   │   │   ├── FavoritesScreen.kt     ← Starred contacts grid
│   │   │   └── ContactDetailScreen.kt ← Full contact detail
│   │   └── theme/
│   │       ├── Theme.kt
│   │       ├── Typography.kt
│   │       └── Shapes.kt
│   └── utils/
│       ├── IntentUtils.kt     ← All system app intents
│       ├── FormatUtils.kt     ← Date/duration formatting
│       └── AvatarColorUtils.kt
└── res/
    ├── values/
    │   ├── strings.xml        ← All strings externalized
    │   ├── colors.xml
    │   ├── themes.xml
    │   └── ic_launcher_background.xml
    └── drawable/
        └── ic_launcher_foreground.xml
```

---

## Setup & Build

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps

1. **Open** the project in Android Studio:  
   `File → Open → select ContactsApp/`

2. **Sync Gradle** — Android Studio will prompt automatically.

3. **Run** on a physical device or emulator with API 26+.

> **Note:** For best results, use a **physical device** — the emulator does not have real contacts or call log data.

---

## Permissions

| Permission | Usage |
|---|---|
| `READ_CONTACTS` | Display contacts list, search, favorites |
| `WRITE_CONTACTS` | (Declared; actual writes go through system UI) |
| `CALL_PHONE` | (Declared; calls go through system dialer) |
| `READ_CALL_LOG` | Display recent call history |
| `SEND_SMS` | (Declared; messages go through system app) |

All sensitive permissions are requested at runtime with a friendly explanation screen.

---

## Design Highlights

- **Color palette**: Deep Indigo primary (`#4A58CE`) + Warm Coral accent (`#D83B01`)
- **Adaptive avatars**: Color-coded initials avatar when no photo is set
- **Edge-to-edge**: Draws behind status bar and navigation bar
- **Sticky alpha headers**: Alphabetical section headers scroll with the list
- **Call type coloring**: Missed = red, Incoming = green, Outgoing = blue
- **Animated transitions**: Staggered fade + slide on list items
