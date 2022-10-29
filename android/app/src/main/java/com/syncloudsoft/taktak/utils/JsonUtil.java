package com.syncloudsoft.taktak.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class JsonUtil {

    @Nullable
    public static Integer optInt(
            @NonNull JSONObject object,
            @NonNull String key
    ) throws JSONException {
        if (object.has(key) && !object.isNull(key)) {
            return object.getInt(key);
        }

        return null;
    }

    @Nullable
    public static String optString(
            @NonNull JSONObject object,
            @NonNull String key
    ) throws JSONException {
        if (object.has(key) && !object.isNull(key)) {
            return object.getString(key);
        }

        return null;
    }
}
