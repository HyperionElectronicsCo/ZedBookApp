# ZedBook

AIDE-compatible Java Android social networking starter app, built as a Facebook-style prototype using only standard Android Java APIs and Firebase REST calls. No lambdas are used.

## Features included

- Splash screen
- Login and sign-up using Firebase Authentication REST API
- Realtime Database user profiles
- Home landing page / News Feed after login
- Home composer for posting statuses to the main feed
- Profile tab with profile header and a separate profile status composer
- Profile picture and display name editing
- Registered user search
- Add users as friends from search results
- Invite friends through the Android share sheet
- Picture attachment for statuses
- Video URI attachment placeholder
- Current location attachment and map opening
- Like / dislike reactions
- Comments
- Comment notifications for post owners
- Editable statuses by the original author
- Separate Notifications tab
- Tag notifications when someone you have added tags you with @DisplayName, @DisplayNameWithoutSpaces, @emailname, or @emailaddress
- Portrait layout matching the rough ZedBook mockup style

## Firebase setup

Open:

`app/src/main/java/com/hyperion/zedbook/MainActivity.java`

Replace:

```java
private static final String FIREBASE_WEB_API_KEY = "PUT_YOUR_FIREBASE_WEB_API_KEY_HERE";
private static final String FIREBASE_DATABASE_URL = "https://YOUR_PROJECT_ID-default-rtdb.firebaseio.com";
```

With your Firebase Web API key and Realtime Database URL.

In Firebase Console:

1. Enable Authentication -> Email/Password.
2. Enable Realtime Database.
3. For testing, use open rules while developing:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

## Notes

Images are stored as compressed Base64 in Realtime Database for easy AIDE compatibility. For a production app, move media uploads to Firebase Storage. Video support is included as a URI placeholder because direct large video upload needs Firebase Storage or your own server.

Tag notifications are friend-aware. If you add a user from Search, you will receive a notification when that user tags you in a status.
