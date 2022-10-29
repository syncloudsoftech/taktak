package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.beak.gifmakerlib.GifMaker;
import com.syncloudsoft.taktak.utils.VideoUtil;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class GeneratePreviewWorker extends Worker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_PREVIEW = "preview";
    public static final String KEY_SCREENSHOT = "screenshot";
    public static final String TAG = "GeneratePreviewWorker";

    public GeneratePreviewWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String input = getInputData().getString(KEY_INPUT);
        String screenshot = getInputData().getString(KEY_SCREENSHOT);
        OutputStream os = null;
        try {
            Bitmap frame = VideoUtil.getFrameAtTime(input, TimeUnit.SECONDS.toMicros(1));
            os = new FileOutputStream(screenshot);
            //noinspection ConstantConditions
            frame.compress(Bitmap.CompressFormat.PNG, 75, os);
            frame.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + input, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                }
            }
        }

        String preview = getInputData().getString(KEY_PREVIEW);
        GifMaker gif = new GifMaker(2);
        boolean ok = gif.makeGifFromVideo(
                input, 1000, 3000, 250, preview);
        return ok ? Result.success() : Result.failure();
    }
}
