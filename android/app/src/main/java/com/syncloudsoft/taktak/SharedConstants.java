package com.syncloudsoft.taktak;

import java.util.concurrent.TimeUnit;

public final class SharedConstants {

    public static final long MAX_DURATION = TimeUnit.SECONDS.toMillis(60);

    public static final String PREF_FCM_TOKEN = "fcm_token";
    public static final String PREF_FCM_TOKEN_SYNCED_AT = "fcm_token_synced_at";
    public static final String PREF_INTRO_SHOWN = "intro_shown";
    public static final String PREF_SERVER_TOKEN = "server_token";

    public static final int REQUEST_CODE_LOGIN_GOOGLE = 60600 + 1;
    public static final int REQUEST_CODE_PICK_SONG = 60600 + 2;
    public static final int REQUEST_CODE_READ_STORAGE = 60600 + 3;
}
