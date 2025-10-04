# Firebase Setup Instructions

## Important: You need to set up Firebase for this app to work!

This app uses Firebase for:
- Authentication (Email/Password)
- Cloud Firestore (Database)
- Cloud Storage (Images)

### Setup Steps:

1. **Go to Firebase Console**: https://console.firebase.google.com/

2. **Create a new project** or use an existing one

3. **Add an Android app** to your Firebase project:
   - Package name: `com.example.myapplication`
   - Download the `google-services.json` file

4. **Replace the placeholder file**:
   - Replace `app/google-services.json` with your downloaded file

5. **Enable Authentication**:
   - Go to Authentication > Sign-in method
   - Enable "Email/Password" provider

6. **Create Firestore Database**:
   - Go to Firestore Database
   - Click "Create database"
   - Start in **test mode** (for development)
   - Choose a location

7. **Enable Cloud Storage**:
   - Go to Storage
   - Click "Get started"
   - Start in **test mode** (for development)

8. **Firestore Security Rules** (for testing):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

9. **Storage Security Rules** (for testing):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### After Setup:
1. Clean and rebuild your project
2. Run the app on a physical device or emulator
3. Create an account and start dropping secrets!

### Note:
The current `google-services.json` is a placeholder. The app will NOT work until you replace it with your actual Firebase configuration file.

