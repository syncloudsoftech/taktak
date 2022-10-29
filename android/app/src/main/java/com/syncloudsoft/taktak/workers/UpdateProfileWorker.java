package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.syncloudsoft.taktak.R;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateProfileWorker extends Worker {

    public static final String KEY_BIO = "bio";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_ERRORS = "errors";
    public static final String KEY_NAME = "name";
    public static final String KEY_PHOTO = "photo";
    public static final String KEY_REMOVE = "remove";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USERNAME = "username";
    public static final String TAG = "UpdateProfileWorker";

    private final OkHttpClient mHttpClient = new OkHttpClient.Builder()
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build();

    public UpdateProfileWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String photo = getInputData().getString(KEY_PHOTO);
        String name = getInputData().getString(KEY_NAME);
        String username = getInputData().getString(KEY_USERNAME);
        String email = getInputData().getString(KEY_EMAIL);
        boolean remove = getInputData().getBoolean(KEY_REMOVE, false);
        String bio = getInputData().getString(KEY_BIO);
        String token = getInputData().getString(KEY_TOKEN);
        MultipartBody.Builder body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        if (photo != null) {
            body.addFormDataPart("photo", "photo.png", RequestBody.create(MediaType.parse("image/png"), new File(photo)));
        }

        if (name != null) {
            body.addFormDataPart("name", name);
        }

        if (username != null) {
            body.addFormDataPart("username", username);
        }

        if (email != null) {
            body.addFormDataPart("email", email);
        }

        if (bio != null) {
            body.addFormDataPart("bio", bio);
        }

        if (remove) {
            body.addFormDataPart("remove", "1");
        }

        Request request = new Request.Builder()
                .url(getApplicationContext().getString(R.string.server_url) + "api/self")
                .post(body.build())
                .header("Authorization", "Bearer " + token)
                .build();
        Response response = null;
        try {
            response = mHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "Failed when updating profile with server.", e);
        }

        if (response != null && response.isSuccessful()) {
            return Result.success();
        }

        try {
            //noinspection ConstantConditions
            Log.w(TAG, "Server returned " + response.code() + " status.");
            //noinspection ConstantConditions
            String content = response.body().string();
            Log.v(TAG, "Server returned\n" + content);
            if (response.code() == 422) {
                return Result.failure(new Data.Builder().putString(KEY_ERRORS, content).build());
            }
        } catch (Exception ignore) {
        }

        return Result.failure();
    }
}
