- `ACCESS_NETWORK_STATE` - For checking network connectivity

## Known Issues & TODOs

- Google Sign-In requires proper OAuth client ID configuration in Firebase
- Map markers could use custom icons
- Implement geohash for efficient location queries in Firestore
- Add pull-to-refresh functionality
- Implement real-time updates for secrets feed

## Screenshots

(Add screenshots of your app here)

## Contributing

This is a personal project, but suggestions and bug reports are welcome!

## License

This project is for educational purposes.

## Contact

For questions or support, please open an issue on GitHub.

---

**Your SHA-1 Debug Key**: `F9:7C:9D:3F:9F:99:8F:4E:B7:48:2B:03:52:BE:98:8D:59:92:D9:EE`

Remember to add this to your Firebase project settings!
# Secret Spaces

A location-based anonymous social media Android application built with Jetpack Compose and Firebase.

## Features

- 📍 **Location-Based Secrets**: Drop and discover secrets at specific geographic locations
- 🔐 **Authentication**: Email/Password and Google Sign-In support
- 👻 **Anonymous Posting**: Option to post secrets anonymously
- 💬 **Social Interactions**: Like and comment on secrets
- 🗺️ **Interactive Map**: View secrets on an OpenStreetMap-based map
- 📸 **Image Support**: Attach images to your secrets
- 👤 **User Profiles**: Customizable profiles with bio and profile picture

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Backend**: Firebase
  - Firebase Authentication (Email/Password & Google Sign-In)
  - Cloud Firestore (Database)
  - Firebase Storage (Images)
- **Location Services**: Google Play Services Location API
- **Maps**: OSMDroid (OpenStreetMap)
- **Image Loading**: Coil
- **Permissions**: Accompanist Permissions

## Project Structure

```
app/src/main/java/com/secretspaces32/android/
├── MainActivity.kt
├── data/
│   ├── model/              # Data classes (User, Secret, Comment, Like)
│   ├── remote/             # API service interfaces (deprecated)
│   └── repository/         # Data repositories
│       ├── AuthRepository.kt
│       ├── FirebaseUserRepository.kt
│       └── FirebaseSecretRepository.kt
├── ui/
│   ├── navigation/         # App navigation
│   ├── screens/            # All UI screens
│   └── theme/              # App theming
├── utils/                  # Utility classes
└── viewmodel/              # ViewModels
```

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11 or higher
- Firebase account
- Android device or emulator with Google Play Services

### Firebase Setup

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project named "SecretSpaces" or use existing one

2. **Add Android App to Firebase**
   - Package name: `com.secretspaces32.android`
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Enable Authentication Methods**
   - Go to Authentication → Sign-in method
   - Enable Email/Password
   - Enable Google Sign-In

4. **Get SHA-1 Certificate**
   - Run `get_sha1.bat` (Windows) to get your SHA-1 fingerprint
   - Or use: `keytool -list -v -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android`
   - Copy the SHA-1 fingerprint

5. **Add SHA-1 to Firebase**
   - Go to Project Settings → Your apps
   - Click "Add fingerprint"
   - Paste your SHA-1 fingerprint
   - Download the updated `google-services.json`

6. **Enable Firestore Database**
   - Go to Firestore Database
   - Create database in production mode
   - Set up security rules (see below)

7. **Enable Firebase Storage**
   - Go to Storage
   - Get started with default settings
   - Update security rules (see below)

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Secrets collection
    match /secrets/{secretId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Comments collection
    match /comments/{commentId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow delete: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Likes collection
    match /likes/{likeId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### Firebase Storage Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile_pictures/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    match /secret_images/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

## Building the App

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Add your `google-services.json` file to `app/` directory
5. Build and run on device or emulator

## Permissions Required

- `INTERNET` - For Firebase and network requests
- `ACCESS_FINE_LOCATION` - For precise location tracking
- `ACCESS_COARSE_LOCATION` - For approximate location
- `CAMERA` - For taking photos
- `READ_MEDIA_IMAGES` - For selecting images from gallery

