package com.syncloudsoft.taktak.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.daasuu.gpuv.egl.filter.GlBrightnessFilter;
import com.daasuu.gpuv.egl.filter.GlExposureFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.egl.filter.GlGammaFilter;
import com.daasuu.gpuv.egl.filter.GlGrayScaleFilter;
import com.daasuu.gpuv.egl.filter.GlHazeFilter;
import com.daasuu.gpuv.egl.filter.GlInvertFilter;
import com.daasuu.gpuv.egl.filter.GlMonochromeFilter;
import com.daasuu.gpuv.egl.filter.GlPixelationFilter;
import com.daasuu.gpuv.egl.filter.GlPosterizeFilter;
import com.daasuu.gpuv.egl.filter.GlSepiaFilter;
import com.daasuu.gpuv.egl.filter.GlSharpenFilter;
import com.daasuu.gpuv.egl.filter.GlSolarizeFilter;
import com.daasuu.gpuv.egl.filter.GlVignetteFilter;
import com.daasuu.gpuv.player.GPUPlayerView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.common.VideoFilter;
import com.syncloudsoft.taktak.utils.VideoUtil;
import com.syncloudsoft.taktak.workers.VideoFilterWorker;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHazeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageMonochromeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSolarizeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter;

public class FilterActivity extends AppCompatActivity {

    public static final String EXTRA_SONG = "song";
    public static final String EXTRA_VIDEO = "video";
    public static final String TAG = "FilterActivity";

    private FilterActivityViewModel mModel;
    private SimpleExoPlayer mPlayer;
    private int mSong;
    private String mVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        mModel = new ViewModelProvider(this).get(FilterActivityViewModel.class);
        mSong = getIntent().getIntExtra(EXTRA_SONG, 0);
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        ImageButton close = findViewById(R.id.close);
        close.setOnClickListener(view -> finish());
        ImageButton done = findViewById(R.id.done);
        done.setOnClickListener(view -> commitSelection());
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        mPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
        GPUPlayerView player = findViewById(R.id.player);
        player.setSimpleExoPlayer(mPlayer);
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(this, getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(Uri.fromFile(new File(mVideo))));
        mPlayer.setPlayWhenReady(true);
        mPlayer.prepare(source);
        RecyclerView rv = findViewById(R.id.filters);
        long duration = VideoUtil.getDuration(mVideo);
        long timestamp = TimeUnit.SECONDS.toMicros(2);
        if (timestamp > duration) {
            timestamp = duration;
        }
        Bitmap thumbnail = VideoUtil.getFrameAtTime(mVideo, timestamp);
        rv.setAdapter(new FilterAdapter(thumbnail));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.setPlayWhenReady(false);
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }

        mPlayer.release();
        mPlayer = null;

        RecyclerView rv = findViewById(R.id.filters);
        FilterAdapter adapter = (FilterAdapter)rv.getAdapter();
        //noinspection ConstantConditions
        adapter.recycle();
    }

    public void applyFilter(VideoFilter filter) {
        Log.v(TAG, "User wants to apply " + filter.name() + " filter.");
        GPUPlayerView player = findViewById(R.id.player);
        switch (mModel.filter = filter) {
            case BRIGHTNESS: {
                GlBrightnessFilter glf = new GlBrightnessFilter();
                glf.setBrightness(0.1f);
                player.setGlFilter(glf);
                break;
            }
            case EXPOSURE:
                player.setGlFilter(new GlExposureFilter());
                break;
            case GAMMA: {
                GlGammaFilter glf = new GlGammaFilter();
                glf.setGamma(2f);
                player.setGlFilter(glf);
                break;
            }
            case GRAYSCALE:
                player.setGlFilter(new GlGrayScaleFilter());
                break;
            case HAZE: {
                GlHazeFilter glf = new GlHazeFilter();
                glf.setSlope(-0.5f);
                player.setGlFilter(glf);
                break;
            }
            case INVERT:
                player.setGlFilter(new GlInvertFilter());
                break;
            case MONOCHROME:
                player.setGlFilter(new GlMonochromeFilter());
                break;
            case PIXELATED:
                player.setGlFilter(new GlPixelationFilter());
                break;
            case POSTERIZE:
                player.setGlFilter(new GlPosterizeFilter());
                break;
            case SEPIA:
                player.setGlFilter(new GlSepiaFilter());
                break;
            case SHARP: {
                GlSharpenFilter glf = new GlSharpenFilter();
                glf.setSharpness(3f);
                player.setGlFilter(glf);
                break;
            }
            case SOLARIZE:
                player.setGlFilter(new GlSolarizeFilter());
                break;
            case VIGNETTE:
                player.setGlFilter(new GlVignetteFilter());
                break;
            default:
                player.setGlFilter(new GlFilter());
                break;
        }
    }

    private void closeFinally(File video) {
        Log.v(TAG, "Filter was successfully applied to " + video);
        Intent intent = new Intent(this, PostVideoActivity.class);
        intent.putExtra(PostVideoActivity.EXTRA_SONG, mSong);
        intent.putExtra(PostVideoActivity.EXTRA_VIDEO, video.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void commitSelection() {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mPlayer.setPlayWhenReady(false);
        WorkManager wm = WorkManager.getInstance(this);
        final File filtered = new File(getCacheDir(), UUID.randomUUID().toString());
        Data data = new Data.Builder()
                .putString(VideoFilterWorker.KEY_FILTER, mModel.filter.name())
                .putString(VideoFilterWorker.KEY_INPUT, mVideo)
                .putString(VideoFilterWorker.KEY_OUTPUT, filtered.getAbsolutePath())
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(VideoFilterWorker.class)
                .setInputData(data)
                .build();
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED;
                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        progress.dismiss();
                        closeFinally(filtered);
                    } else if (ended) {
                        progress.dismiss();
                    }
                });
    }

    public static class FilterActivityViewModel extends ViewModel {

        public VideoFilter filter = VideoFilter.NONE;
        public List<VideoFilter> filters = Arrays.asList(VideoFilter.values());
    }

    private class FilterAdapter extends RecyclerView.Adapter<FilterViewHolder> {

        private Bitmap mThumbnail;

        public FilterAdapter(Bitmap thumbnail) {
            mThumbnail = thumbnail;
        }

        @Override
        public int getItemCount() {
            return mModel.filters.size();
        }

        @NonNull
        @Override
        public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(FilterActivity.this)
                    .inflate(R.layout.item_filter, parent, false);
            FilterViewHolder holder = new FilterViewHolder(view);
            holder.setIsRecyclable(false);
            holder.image.setImage(mThumbnail);
            return holder;
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
            final VideoFilter filter = mModel.filters.get(position);
            switch (filter) {
                case BRIGHTNESS:
                    holder.image.setFilter(new GPUImageBrightnessFilter());
                    break;
                case EXPOSURE:
                    holder.image.setFilter(new GPUImageExposureFilter());
                    break;
                case GAMMA:
                    holder.image.setFilter(new GPUImageGammaFilter());
                    break;
                case GRAYSCALE:
                    holder.image.setFilter(new GPUImageGrayscaleFilter());
                    break;
                case HAZE:
                    holder.image.setFilter(new GPUImageHazeFilter());
                    break;
                case INVERT:
                    holder.image.setFilter(new GPUImageColorInvertFilter());
                    break;
                case MONOCHROME:
                    holder.image.setFilter(new GPUImageMonochromeFilter());
                    break;
                case PIXELATED:
                    holder.image.setFilter(new GPUImagePixelationFilter());
                    break;
                case POSTERIZE:
                    holder.image.setFilter(new GPUImagePosterizeFilter());
                    break;
                case SEPIA:
                    holder.image.setFilter(new GPUImageSepiaToneFilter());
                    break;
                case SHARP:
                    holder.image.setFilter(new GPUImageSharpenFilter());
                    break;
                case SOLARIZE:
                    holder.image.setFilter(new GPUImageSolarizeFilter());
                    break;
                case VIGNETTE:
                    holder.image.setFilter(new GPUImageVignetteFilter());
                    break;
                default:
                    holder.image.setFilter(new GPUImageFilter());
                    break;
            }

            String name = filter.name().toLowerCase(Locale.US);
            holder.name.setText(name.substring(0, 1).toUpperCase() + name.substring(1));
            holder.itemView.setOnClickListener(view -> applyFilter(filter));
        }

        public void recycle() {
            mThumbnail.recycle();
        }
    }

    private static class FilterViewHolder extends RecyclerView.ViewHolder {

        public GPUImageView image;
        public TextView name;

        public FilterViewHolder(@NonNull View root) {
            super(root);
            image = root.findViewById(R.id.image);
            name = root.findViewById(R.id.name);
        }
    }
}
