# Secret Spaces - Location-Based Social App

A location-based social Android app built with Kotlin and Firebase that allows users to drop and discover "Secrets" (posts/messages) tied to real-world locations.

## 🌟 Features

### ✅ Implemented Features

#### 1. **Authentication & User Profiles**
- ✅ Sign up / Sign in with email and password (Firebase Auth)
- ✅ User profiles with:
  - Username
  - Profile picture
  - Bio
  - Email
- ✅ Edit profile functionality
- ✅ Sign out

#### 2. **Drop Secrets (Post Creation)**
- ✅ Create location-based posts with:
  - Text content (required)
  - Optional image attachment
  - GPS location (lat, lng)
  - Timestamp
  - User reference
  - **Anonymous mode** - Post as "Anonymous"
- ✅ Posts are tied to user's current GPS location
- ✅ Image upload to Firebase Storage

#### 3. **Location-Based Discovery**
- ✅ **Map View**: Interactive map showing secret pins around user's location
  - Tap pins to view secret details
  - Current location marker
  - Visual clustering of nearby secrets
- ✅ **Feed View**: List of secrets sorted by proximity
  - Closest secrets appear first
  - Distance indicators
  - User avatars and timestamps

#### 4. **Social Engagement**
- ✅ **Like System**:
  - Toggle like/unlike on secrets
  - Real-time like count updates
  - Visual indication of user's likes
- ✅ **Comment System**:
  - Add comments to any secret
  - View all comments in threaded view
  - User avatars in comments
  - Comment count display
- ✅ **View Engagement**:
  - See who liked a post
  - Read all comments
  - Engagement statistics

#### 5. **My Secrets**
- ✅ View all your posted secrets
- ✅ See engagement stats (likes, comments)
- ✅ Location and timestamp for each post
- ✅ Tap to view full details

#### 6. **Secret Detail View**
- ✅ Full secret content with image
- ✅ User information
- ✅ Like/Unlike button
- ✅ Comment section with real-time updates
- ✅ View all likes list

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Backend**: Firebase
  - Firebase Authentication
  - Cloud Firestore (Database)
  - Cloud Storage (Images)
- **Location**: Google Play Services Location API
- **Maps**: OSMDroid (OpenStreetMap)
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines & Flow

### Project Structure
```
app/src/main/java/com/example/myapplication/
├── data/
│   ├── model/
│   │   ├── User.kt              # User data model
│   │   ├── Secret.kt            # Secret/Post data model
│   │   ├── Comment.kt           # Comment data model
│   │   └── Like.kt              # Like data model
│   └── repository/
│       ├── AuthRepository.kt           # Firebase Auth
│       ├── FirebaseUserRepository.kt   # User management
│       └── FirebaseSecretRepository.kt # Secrets, likes, comments
├── ui/
│   ├── navigation/
│   │   └── SecretSpacesApp.kt   # Main app navigation
│   └── screens/
│       ├── AuthScreen.kt        # Login/Signup
│       ├── MapScreen.kt         # Map view with pins
│       ├── FeedScreen.kt        # List of secrets
│       ├── DropSecretScreen.kt  # Create new secret
│       ├── ProfileScreen.kt     # User profile
│       ├── MySecretsScreen.kt   # User's secrets
│       └── SecretDetailScreen.kt # Secret details with comments
├── viewmodel/
│   └── MainViewModel.kt         # Central ViewModel
├── utils/
│   ├── LocationHelper.kt        # GPS utilities
│   └── ImageUtils.kt            # Image processing
└── MainActivity.kt              # Entry point
```

## 🔥 Firebase Collections Structure

### Users Collection (`users`)
```json
{
  "userId": {
    "id": "string",
    "email": "string",
    "username": "string",
    "profilePictureUrl": "string?",
    "bio": "string",
    "createdAt": "timestamp"
  }
}
```

### Secrets Collection (`secrets`)
```json
{
  "secretId": {
    "id": "string",
    "text": "string",
    "imageUrl": "string?",
    "latitude": "double",
    "longitude": "double",
    "timestamp": "long",
    "userId": "string",
    "username": "string",
    "userProfilePicture": "string?",
    "isAnonymous": "boolean",
    "likeCount": "int",
    "commentCount": "int"
  }
}
```

### Likes Collection (`likes`)
```json
{
  "likeId": {
    "id": "userId_secretId",
    "secretId": "string",
    "userId": "string",
    "username": "string",
    "timestamp": "long"
  }
}
```

### Comments Collection (`comments`)
```json
{
  "commentId": {
    "id": "string",
    "secretId": "string",
    "userId": "string",
    "username": "string",
    "userProfilePicture": "string?",
    "text": "string",
    "timestamp": "long"
  }
}
```

## 🚀 Setup Instructions

### Prerequisites
- Android Studio (latest version)
- JDK 11 or higher
- Android device or emulator with Google Play Services
- Firebase account

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Project_Android
```

### 2. Firebase Setup (REQUIRED!)
⚠️ **The app will NOT work without Firebase configuration!**

See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for detailed instructions.

Quick steps:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add an Android app (package: `com.example.myapplication`)
4. Download `google-services.json`
5. Replace `app/google-services.json` with your file
6. Enable Authentication (Email/Password)
7. Create Firestore Database
8. Enable Cloud Storage

### 3. Build and Run
```bash
# Open in Android Studio and run, or use command line:
./gradlew assembleDebug
./gradlew installDebug
```

## 📱 App Flow

1. **Launch**: User sees authentication screen
2. **Sign Up/In**: Create account or log in
3. **Permission Request**: App requests location permissions
4. **Main App**:
   - **Map Tab**: See secrets on a map
   - **Feed Tab**: Browse secrets in a list
   - **Drop Tab**: Create a new secret at current location
   - **Mine Tab**: View your posted secrets
   - **Profile Tab**: Edit profile or sign out
5. **Interact**: Like, comment, and engage with secrets

## 🔒 Permissions

- **Location** (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION): Required for dropping and discovering location-based secrets
- **Internet**: Required for Firebase
- **Camera**: Optional, for taking photos
- **Storage**: For selecting images from gallery

## 🎨 UI Features

- Material 3 Design
- Dark/Light theme support
- Smooth animations
- Loading states
- Error handling with toast messages
- Bottom navigation bar
- Pull-to-refresh capability
- Image previews
- Distance indicators
- Timestamp formatting

## 🔐 Security Considerations

**Current State (Development/Testing)**:
- Firestore rules allow authenticated users to read/write all documents
- Storage rules allow authenticated users to upload/download all files

**For Production**, implement proper security rules:

### Firestore Rules (Production)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    match /secrets/{secretId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    match /comments/{commentId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    match /likes/{likeId} {
      allow read: if request.auth != null;
      allow create, delete: if request.auth != null;
    }
  }
}
```

### Storage Rules (Production)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile_pictures/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /secret_images/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## 🚧 Future Enhancements

- [ ] Push notifications for likes/comments
- [ ] Follow/unfollow users
- [ ] Secret expiration (time-limited posts)
- [ ] Geohash indexing for efficient location queries
- [ ] Search functionality
- [ ] Hashtags and categories
- [ ] Report/flag inappropriate content
- [ ] Direct messaging
- [ ] Secret collections/playlists
- [ ] AR view for discovering secrets
- [ ] Share secrets to other platforms

## 📝 License

This project is for educational purposes.

## 🤝 Contributing

Contributions welcome! Please follow standard Git workflow:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📧 Support

For issues or questions, please open an issue on GitHub.

---

**Happy Secret Dropping! 🤫📍**

