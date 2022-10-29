package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
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

public class PostVideoWorker extends Worker {

    public static final String KEY_COMMENTS = "comments";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_PREVIEW = "preview";
    public static final String KEY_PRIVATE = "private";
    public static final String KEY_SCREENSHOT = "screenshot";
    public static final String KEY_SONG = "song";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_VIDEO = "video";
    public static final String TAG = "PostVideoWorker";

    private final OkHttpClient mHttpClient = new OkHttpClient.Builder()
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build();

    public PostVideoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        String video = getInputData().getString(KEY_VIDEO);
        Log.v(TAG, "Uploading " + video);
        String screenshot = getInputData().getString(KEY_SCREENSHOT);
        String preview = getInputData().getString(KEY_PREVIEW);
        int song = getInputData().getInt(KEY_SONG, 0);
        String description = getInputData().getString(KEY_DESCRIPTION);
        boolean private2 = getInputData().getBoolean(KEY_PRIVATE, false);
        boolean comments = getInputData().getBoolean(KEY_COMMENTS, false);
        String token = getInputData().getString(KEY_TOKEN);
        MultipartBody.Builder body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("video", "video.mp4", RequestBody.create(MediaType.parse("video/mp4"), new File(video)))
                .addFormDataPart("screenshot", "screenshot.png", RequestBody.create(MediaType.parse("image/png"), new File(screenshot)))
                .addFormDataPart("preview", "preview.gif", RequestBody.create(MediaType.parse("image/gif"), new File(preview)));
        if (song > 0) {
            body.addFormDataPart("song_id", song + "");
        }

        if (!TextUtils.isEmpty(description)) {
            body.addFormDataPart("description", description);
        }

        if (private2) {
            body.addFormDataPart("private", "1");
        }

        if (comments) {
            body.addFormDataPart("comments", "1");
        }

        Request request = new Request.Builder()
                .url(getApplicationContext().getString(R.string.server_url) + "api/videos")
                .post(body.build())
                .header("Authorization", "Bearer " + token)
                .build();
        Response response = null;
        try {
            response = mHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "Failed when uploading video to server.", e);
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
