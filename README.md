# Sliide KMP

KMP user management app using the [GoRest API](https://gorest.co.in/). Built with Compose Multiplatform, targets Android and iOS.

## Setup

1. Get a GoRest API token from https://gorest.co.in/consumer/login
2. Add `GOREST_TOKEN=your-token` to `local.properties`
3. Open in Android Studio and run the `composeApp` configuration
4. For iOS: open `iosApp/iosApp.xcodeproj` in Xcode
5. Run tests: `./gradlew allTests` (requires Xcode CLI tools for iOS targets, or `./gradlew testDebugUnitTest` for Android-only)

Requires Android API 24+ / iOS 16+.

## Architecture

Multi-module MVI. Room is the single source of truth - the UI observes the database via Flow, and network calls just update the local cache.

```
core/                  # Shared utilities - networking, error types, theme, UI components
feature-users/         # Domain (models, repo interface, use cases) + Presentation (MVI, screens)
shared/                # Data layer (Room, API, DTOs, mapper, repo impl) + DI wiring + iOS framework
composeApp/            # Android entry point
iosApp/                # iOS entry point (SwiftUI host)
```

Feature modules own domain + presentation. The data layer lives in `shared` alongside Room (KSP requires entities and `@Database` in the same module). DI wiring in `shared` connects data implementations to domain interfaces.

The presentation layer follows MVI: intents go in, the ViewModel runs use cases and emits results, a pure reducer produces the next state. One-shot events (snackbar, toasts) go through a `Channel<Effect>`.


## What it does

- Shows the most recently created users from GoRest (last page)
- Add users with name/email validation and gender picker
- Long-press to delete, with undo via snackbar
- Adaptive layout - single column on phones, list+detail on tablets
- Shimmer placeholders while loading
- Pull-to-refresh from the toolbar
- Light/dark theme follows system setting
- Offline support via Room cache

## Development approach

I used Claude Code as a pair programmer throughout this project. I designed the architecture and made the key decisions (MVI, Room as source of truth, multi-module structure), then used AI to accelerate the implementation — scaffolding out the layer boilerplate, writing the reducer tests, and iterating on the Room KMP setup which has some rough edges in alpha. I also ran a code review pass through it to catch things like missing transaction safety on the refresh flow and the undo-delete only restoring locally. The architecture and trade-offs are mine; AI helped me move faster on the parts that would otherwise just be time-consuming.
