package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.daasuu.mp4compose.filter.GlBrightnessFilter;
import com.daasuu.mp4compose.filter.GlExposureFilter;
import com.daasuu.mp4compose.filter.GlGammaFilter;
import com.daasuu.mp4compose.filter.GlGrayScaleFilter;
import com.daasuu.mp4compose.filter.GlHazeFilter;
import com.daasuu.mp4compose.filter.GlInvertFilter;
import com.daasuu.mp4compose.filter.GlMonochromeFilter;
import com.daasuu.mp4compose.filter.GlPixelationFilter;
import com.daasuu.mp4compose.filter.GlPosterizeFilter;
import com.daasuu.mp4compose.filter.GlSepiaFilter;
import com.daasuu.mp4compose.filter.GlSharpenFilter;
import com.daasuu.mp4compose.filter.GlSolarizeFilter;
import com.daasuu.mp4compose.filter.GlVignetteFilter;
import com.daasuu.mp4compose.composer.Mp4Composer;
import com.google.common.util.concurrent.ListenableFuture;
import com.syncloudsoft.taktak.common.VideoFilter;
import com.syncloudsoft.taktak.utils.VideoUtil;

public class VideoFilterWorker extends ListenableWorker {

    public static final String KEY_FILTER = "filter";
    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    public static final String TAG = "VideoFilterWorker";

    public VideoFilterWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            doActualWork(completer);
            return null;
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void doActualWork(CallbackToFutureAdapter.Completer<Result> completer) {
        String input = getInputData().getString(KEY_INPUT);
        Size size = VideoUtil.getDimensions(input);
        String output = getInputData().getString(KEY_OUTPUT);
        Mp4Composer composer = new Mp4Composer(input, output);
        composer.videoBitrate((int) (.07 * 30 * size.getWidth() * size.getHeight()));
        VideoFilter filter = VideoFilter.valueOf(getInputData().getString(KEY_FILTER));
        switch (filter) {
            case BRIGHTNESS: {
                GlBrightnessFilter glf = new GlBrightnessFilter();
                glf.setBrightness(0.1f);
                composer.filter(glf);
                break;
            }
            case EXPOSURE:
                composer.filter(new GlExposureFilter());
                break;
            case GAMMA: {
                GlGammaFilter glf = new GlGammaFilter();
                glf.setGamma(2f);
                composer.filter(glf);
                break;
            }
            case GRAYSCALE:
                composer.filter(new GlGrayScaleFilter());
                break;
            case HAZE: {
                GlHazeFilter glf = new GlHazeFilter();
                glf.setSlope(-0.5f);
                composer.filter(glf);
                break;
            }
            case INVERT:
                composer.filter(new GlInvertFilter());
                break;
            case MONOCHROME:
                composer.filter(new GlMonochromeFilter());
                break;
            case PIXELATED:
                composer.filter(new GlPixelationFilter());
                break;
            case POSTERIZE:
                composer.filter(new GlPosterizeFilter());
                break;
            case SEPIA:
                composer.filter(new GlSepiaFilter());
                break;
            case SHARP: {
                GlSharpenFilter glf = new GlSharpenFilter();
                glf.setSharpness(3f);
                composer.filter(glf);
                break;
            }
            case SOLARIZE:
                composer.filter(new GlSolarizeFilter());
                break;
            case VIGNETTE:
                composer.filter(new GlVignetteFilter());
                break;
            default:
                break;
        }

        composer.listener(new Mp4Composer.Listener() {
            @Override
            public void onProgress(double progress) { }

            @Override
            public void onCompleted() {
                Log.d(TAG, "MP4 composition has finished.");
                completer.set(Result.success());
            }

            @Override
            public void onCanceled() {
                Log.d(TAG, "MP4 composition was cancelled.");
                completer.setCancelled();
            }

            @Override
            public void onFailed(Exception e) {
                Log.d(TAG, "MP4 composition failed with error.", e);
                completer.setException(e);
            }
        });
        composer.start();
    }
}
