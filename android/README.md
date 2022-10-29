# android

This folder holds source code for TakTak's native [Android](https://www.android.com/) app.

## Pre-requisites

1. Create a project (if not already) in [Firebase](https://console.firebase.google.com/) console, download the `google-services.json` file and place it in the [app](app) folder.
2. Create an app (if not already) in [Meta for Developers](https://developers.facebook.com/) console. Make note of the `facebook_app_id`, `facebook_client_token` and `fb_login_protocol_scheme` when setting up **Login with Facebook** for your app.

## Development

To configure the least, you must update following values in the [config.xml](app/src/main/res/values/config.xml) file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!--
    This is the base URL where you have hosted the api sub-project.
    Use http://10.0.2.2:8000/ if connecting from emulator to api running on host.
    -->
    <string name="server_url">_____</string>

    <!--
    You will find these values when setting up Login with Facebook for your app in the Facebook developer console.
    -->
    <string name="facebook_app_id">_____</string>
    <string name="facebook_client_token">_____</string>
    <string name="fb_login_protocol_scheme">fb_____</string>

    <!--
    This is usually the client ID (with "client_type": 3) from your google-services.json file.
    -->
    <string name="google_client_id">_____</string>

    <!-- other as well (optional) -->

</resources>
```

You can now go ahead to building and running the app in [Android Studio](https://developer.android.com/studio).
