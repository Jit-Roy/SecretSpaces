# Secret Spaces - Setup Instructions

## Prerequisites
- Android Studio (latest version)
- Android SDK
- Java 11 or higher

## Initial Setup

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd Project_Android
```

### 2. Configure API Keys

Create or edit the `local.properties` file in the root directory and add the following:

```properties
# Android SDK Location
sdk.dir=<path-to-your-android-sdk>

# MapTiler API Key
# Get your free API key from: https://cloud.maptiler.com/
MAPTILER_API_KEY=your_maptiler_api_key_here

# Cloudinary Configuration
# Get your credentials from: https://console.cloudinary.com/
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

⚠️ **IMPORTANT**: Never commit `local.properties` to version control. It's already in `.gitignore`.

### 3. Firebase Setup

1. Create a Firebase project at https://console.firebase.google.com/
2. Add an Android app to your Firebase project
3. Download `google-services.json` and place it in the `app/` directory
4. Enable Authentication (Email/Password and Google Sign-In)
5. Enable Firestore Database
6. Set up Firestore security rules (see below)

### 4. Cloudinary Setup

1. Sign up at https://cloudinary.com/
2. Get your credentials from the dashboard
3. Add them to `local.properties` as shown above

### 5. Build and Run

```bash
./gradlew clean build
```

Or run directly from Android Studio.

## Firestore Security Rules

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
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Stories collection
    match /stories/{storyId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Comments collection
    match /comments/{commentId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Likes collection
    match /likes/{likeId} {
      allow read: if true;
      allow create, delete: if request.auth != null;
    }
  }
}
```

## Security Notes

- All API keys are stored in `local.properties` which is gitignored
- Never hardcode API keys in source files
- If API keys are accidentally exposed, rotate them immediately:
  - Cloudinary: https://console.cloudinary.com/ → Settings → Security
  - MapTiler: https://cloud.maptiler.com/account/keys/
  - Firebase: https://console.firebase.google.com/ → Project Settings → Service Accounts

## Troubleshooting

### Build fails with "CLOUDINARY_CLOUD_NAME not found"
- Ensure `local.properties` exists and contains all required keys
- Sync Gradle files in Android Studio

### Google Sign-In not working
- Add your SHA-1 and SHA-256 fingerprints to Firebase Console
- Ensure google-services.json is up to date

### Images not uploading
- Check Cloudinary credentials in `local.properties`
- Verify internet connection and permissions

## Contributing

When contributing, ensure you:
1. Never commit `local.properties`
2. Never hardcode API keys
3. Use BuildConfig for all sensitive data
4. Test on both debug and release builds

