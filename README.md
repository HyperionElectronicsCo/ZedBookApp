# ZedBook Android Social App

A Java-only, AIDE-compatible social networking app mockup/project.

## What is included

- Splashscreen.java
- Loginsignup.java
- Homescreen.java feed landing page
- Profile.java
- Notifications.java
- Settings.java
- BaseSocialActivity.java shared UI logic
- helpers/ for Firebase REST, sessions, media, location, UI helpers
- adapters/ for posts, notifications, and user search rows
- models/ for app data wrappers
- drawable/ contains Facebook-esque blue/white/grey UI elements used through the app

## Install compatibility fix in this build

This zip includes both layouts:

1. Classic AIDE layout at the project root:
   - AndroidManifest.xml
   - src/
   - res/
   - project.properties

2. Gradle-style layout:
   - app/src/main/
   - app/build.gradle

The manifest now explicitly sets:

```xml
<uses-sdk android:minSdkVersion="21" android:targetSdkVersion="28" />
```

and marks location/camera hardware as optional so Android should not reject the APK as incompatible.

If AIDE says it cannot find `android-28`, open `project.properties` and change:

```properties
target=android-28
```

to the SDK platform installed in your AIDE, for example:

```properties
target=android-35
```

Keep the min SDK as 21 in AndroidManifest.xml.

## Firebase setup

Open:

```text
src/com/hyperion/zedbook/helpers/AppConfig.java
```

or, in the Gradle module:

```text
app/src/main/java/com/hyperion/zedbook/helpers/AppConfig.java
```

Replace:

```java
public static final String FIREBASE_WEB_API_KEY = "PUT_YOUR_FIREBASE_WEB_API_KEY_HERE";
public static final String FIREBASE_DATABASE_URL = "https://YOUR_PROJECT_ID-default-rtdb.firebaseio.com";
```

Enable Firebase Authentication Email/Password and Realtime Database.

No lambdas are used.
