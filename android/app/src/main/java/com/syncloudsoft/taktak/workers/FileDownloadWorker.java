package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class FileDownloadWorker extends Worker {

    public static final String KEY_PATH = "path";
    public static final String KEY_URL = "url";
    private static final String TAG = "FileDownloadWorker";

    private final OkHttpClient mHttpClient = new OkHttpClient();

    public FileDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String path = getInputData().getString(KEY_PATH);
        String url = getInputData().getString(KEY_URL);
        //noinspection ConstantConditions
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        try {
            response = mHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "Failed when downloading " + url, e);
        }

        if (response != null && response.isSuccessful()) {
            BufferedSink out = null;
            //noinspection TryFinallyCanBeTryWithResources
            try {
                //noinspection ConstantConditions
                out = Okio.buffer(Okio.sink(new File(path)));
                //noinspection ConstantConditions
                out.writeAll(response.body().source());
                return Result.success();
            } catch (Exception e) {
                Log.e(TAG, "Failed when saving to " + path, e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        return Result.failure();
    }
}
