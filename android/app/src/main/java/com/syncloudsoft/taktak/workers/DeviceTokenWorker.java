package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.syncloudsoft.taktak.R;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeviceTokenWorker extends Worker {

    public static final String KEY_FCM_TOKEN = "fcm_token";
    public static final String KEY_SERVER_TOKEN = "server_token";
    private static final String TAG = "DeviceTokenWorker";

    private final Context mContext;
    private final OkHttpClient mHttpClient = new OkHttpClient();

    public DeviceTokenWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        String token1 = getInputData().getString(KEY_FCM_TOKEN);
        String token2 = getInputData().getString(KEY_SERVER_TOKEN);
        RequestBody form = new FormBody.Builder()
                .add("token", token1)
                .build();
        Request.Builder request = new Request.Builder()
                .url(mContext.getString(R.string.server_url) + "api/devices")
                .post(form);
        if (!TextUtils.isEmpty(token2)) {
            request = request.header("Authorization", "Bearer " + token2);
        }

        Response response = null;
        try {
            response = mHttpClient.newCall(request.build()).execute();
        } catch (IOException e) {
            Log.e(TAG, "Failed when updating device token with server.", e);
        }

        if (response != null && response.isSuccessful()) {
            return Result.success();
        }

        try {
            Log.w(TAG, "Server returned " + response.code() + " status.");
            Log.v(TAG, "Server returned\n" + response.body().string());
        } catch (Exception ignore) {
        }

        return Result.failure();
    }
}
