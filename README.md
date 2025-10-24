# Secret Spaces

A location-based anonymous social media Android application built with Jetpack Compose, Firebase, and MapLibre.

## Features

- 📍 Location-Based Secrets: Drop and discover secrets at specific geographic locations
- 🔐 Authentication: Email/Password and Google Sign-In support
- 👻 Anonymous Posting: Option to post secrets anonymously
- 💬 Social Interactions: Like and comment on secrets
- 🗺️ Interactive Map: View secrets on a vector map powered by MapLibre + MapTiler
- 📸 Image Support: Attach images to your secrets
- 👤 User Profiles: Customizable profiles with bio and profile picture

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose with Material 3
- Architecture: MVVM
- Backend: Firebase (Auth, Firestore, Storage)
- Location: Google Play Services Location API
- Maps: MapLibre + MapTiler styles
- Image Loading: Coil
- Permissions: Accompanist Permissions

## Project Structure

```
app/src/main/java/com/secretspaces32/android/
├── MainActivity.kt
├── data/
│   ├── model/              # Data classes (User, Secret, Comment, Like)
│   ├── storage/            # Cloudinary storage manager
│   └── repository/         # Data repositories
├── ui/
│   ├── navigation/         # App navigation
│   ├── screens/            # All UI screens
│   └── theme/              # App theming
├── utils/                  # Utility classes
└── viewmodel/              # ViewModels
```

## Setup

1) Create a Firebase project and add an Android app with package `com.secretspaces32.android`. Download `google-services.json` into the `app/` folder.
2) In MapTiler, create an API key with Maps scope.
3) Create (or update) a local.properties file at the project root and add these entries:

```
MAPTILER_API_KEY=your_maptiler_api_key

# Cloudinary (recommended: unsigned uploads; create an unsigned upload preset)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_UNSIGNED_PRESET=your_unsigned_preset

# Optional (legacy signed uploads; not recommended to ship secrets in the app)
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

4) Sync and build the project in Android Studio.

## Firestore/Storage Rules (example)

See Firebase console for production-ready rules. Start with locked-down rules and open as needed.

## Notes

- Avoid committing API keys or secrets to version control. This project reads keys from local.properties and exposes them via BuildConfig at compile-time.
- For Cloudinary, prefer unsigned uploads (upload preset) to avoid embedding API secrets in the app.

## License

This project is for educational purposes.
