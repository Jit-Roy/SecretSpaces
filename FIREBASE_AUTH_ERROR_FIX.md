# Firebase Authentication Error Fix

## Error: "Sign in failed, an internal error occurred, API key not valid"

This error typically means Firebase Authentication is not properly enabled in your Firebase Console.

## Quick Fix Steps:

### Step 1: Enable Firebase Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **secretspaces**
3. Click on **Authentication** in the left sidebar
4. Click **Get Started** (if you haven't already)
5. Go to the **Sign-in method** tab

### Step 2: Enable Email/Password Authentication

1. Find **Email/Password** in the list
2. Click on it
3. Toggle **Enable** to ON
4. Click **Save**

### Step 3: Enable Google Sign-In (if not already done)

1. Find **Google** in the list
2. Click on it
3. Toggle **Enable** to ON
4. Select a **Project support email** from dropdown
5. Click **Save**

### Step 4: Verify Your API Key

1. Go to **Project Settings** (gear icon)
2. Scroll to **Web API Key**
3. Verify it matches your google-services.json: `AIzaSyA9WjUZpMYEi3KlDdV4Xoow1aZzCPgZLmM`
4. Make sure "Restrict key" is NOT enabled (or if it is, Android apps are allowed)

### Step 5: Check Firestore Database

1. Go to **Firestore Database** in the left sidebar
2. Click **Create database** if not created
3. Choose **Start in production mode** or **Test mode**
4. Select a location (preferably close to your users)
5. Click **Enable**

### Step 6: Rebuild Your App

After enabling Authentication:
1. In Android Studio: **Build → Clean Project**
2. Then: **Build → Rebuild Project**
3. Run the app again

## Your Current Configuration:

- ✅ Package name: `com.secretspaces32.android`
- ✅ SHA-1 fingerprint: `f97c9d3f9f998f4eb7482b0352be988d5992d9ee`
- ✅ OAuth clients configured
- ✅ API Key: `AIzaSyA9WjUZpMYEi3KlDdV4Xoow1aZzCPgZLmM`
- ❌ Firebase Authentication might not be enabled

## Alternative: Check API Key Restrictions

If Authentication is already enabled, the issue might be API key restrictions:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project: **secretspaces**
3. Go to **APIs & Services** → **Credentials**
4. Find your API key: `AIzaSyA9WjUZpMYEi3KlDdV4Xoow1aZzCPgZLmM`
5. Click on it
6. Under **Application restrictions**:
   - Choose "None" (for testing)
   - OR choose "Android apps" and make sure your package + SHA-1 are added
7. Under **API restrictions**:
   - Make sure these APIs are allowed:
     - Firebase Authentication API
     - Identity Toolkit API
     - Cloud Firestore API
     - Firebase Storage API
8. Click **Save**
9. Wait 5-10 minutes for changes to propagate

## Test Email/Password Sign-In First

Before testing Google Sign-In:
1. Try signing up with email/password first
2. If that works, it means Firebase Auth is enabled
3. Then try Google Sign-In

## Still Having Issues?

Check these:
- ✅ Internet connection is working
- ✅ Firebase project is not paused or disabled
- ✅ Billing is enabled (if using Blaze plan)
- ✅ App package name matches Firebase: `com.secretspaces32.android`

## Expected Behavior:

After fixing:
- Email/Password sign-in should work immediately
- Google Sign-In should show account picker
- User should be authenticated successfully
- App should navigate to the Map screen

