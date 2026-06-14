# ZedBook AIDE Modular Social App

A Java-only Android social networking app scaffold built to compile in AIDE without lambdas.

## New modular structure

- `MainActivity.java` - launcher/router
- `Splashscreen.java` - branded splash screen
- `Loginsignup.java` - Firebase email/password login and signup
- `Homescreen.java` - Facebook-style home feed and status composer
- `Profile.java` - profile header, profile-picture/name editing, and user's statuses
- `Notifications.java` - friend, tag, and comment notifications
- `Settings.java` - profile settings, invite friends, and logout
- `BaseSocialActivity.java` - shared feed/composer/search/invite logic
- `helpers/` - Firebase REST, session, UI, media, location, post/profile services
- `adapters/` - feed/status card adapter, notifications adapter, users search adapter
- `models/` - small model holder classes
- `res/drawable/` - Facebook-esque vague icon/buttons/cards/background drawables used across the app

## Firebase setup

Open:

`app/src/main/java/com/hyperion/zedbook/helpers/AppConfig.java`

Replace:

```java
public static final String FIREBASE_WEB_API_KEY = "PUT_YOUR_FIREBASE_WEB_API_KEY_HERE";
public static final String FIREBASE_DATABASE_URL = "https://YOUR_PROJECT_ID-default-rtdb.firebaseio.com";
```

Enable Firebase Authentication > Email/Password and Realtime Database while developing.

## Features included

- Splash screen
- Sign up and login through Firebase REST API
- Home landing feed showing public statuses
- Status composer on home and profile
- Picture attachment stored as base64 for prototype use
- Video URI attachment placeholder
- Current-location attachment
- Editable statuses
- Thumbs up/down reactions
- Comments
- User search and add-friend flow
- Invite friends through Android share sheet
- Profile picture and display-name setup
- Separate notifications section for friend/add, comments, and @tag alerts

No lambda expressions are used.
