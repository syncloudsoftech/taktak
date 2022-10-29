package com.syncloudsoft.taktak.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.segmentedprogressbar.SegmentedProgressBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.utils.TextFormatUtil;
import com.syncloudsoft.taktak.workers.MergeAudioVideoWorker;
import com.syncloudsoft.taktak.workers.MergeVideosWorker;

import net.alhazmy13.mediapicker.Video.VideoPicker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RecorderActivity extends AppCompatActivity {

    public static final String EXTRA_AUDIO = "audio";
    private static final String TAG = "RecorderActivity";

    private CameraView mCamera;
    private Handler mHandler = new Handler();
    private MediaPlayer mMediaPlayer;
    private RecorderActivityViewModel mModel;
    private Runnable mStopper = this::stopRecording;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == VideoPicker.VIDEO_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> selection = data.getStringArrayListExtra(VideoPicker.EXTRA_VIDEO_PATH);
            if (selection != null && !selection.isEmpty()) {
                String first = selection.get(0);
                Log.v(TAG, "User chose video file: " + first);
                Intent intent = new Intent(this, VideoTrimmerActivity.class);
                intent.putExtra(VideoTrimmerActivity.EXTRA_VIDEO, first);
                startActivity(intent);
                finish();
            }
        } else if (requestCode == SharedConstants.REQUEST_CODE_PICK_SONG && resultCode == RESULT_OK && data != null) {
            int id = data.getIntExtra(SongPickerActivity.EXTRA_SONG_ID, 0);
            String name = data.getStringExtra(SongPickerActivity.EXTRA_SONG_NAME);
            Uri audio = data.getParcelableExtra(SongPickerActivity.EXTRA_SONG_FILE);
            if (!TextUtils.isEmpty(name) && audio != null) {
                Log.v(TAG, "User chose audio file: " + audio);
                TextView sound = findViewById(R.id.sound);
                sound.setText(name);
                mModel.audio = audio;
                mModel.song = id;
                mMediaPlayer = MediaPlayer.create(this, audio);
                mMediaPlayer.setOnCompletionListener(mp -> mMediaPlayer = null);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        mModel = new ViewModelProvider(this).get(RecorderActivityViewModel.class);
        Uri audio = getIntent().getParcelableExtra(EXTRA_AUDIO);
        if (audio != null) {
            Log.v(TAG, "User chose audio file: " + audio);
            TextView sound = findViewById(R.id.sound);
            sound.setText(getString(R.string.audio_from_video));
            mModel.audio = audio;
            mMediaPlayer = MediaPlayer.create(this, audio);
            mMediaPlayer.setOnCompletionListener(mp -> mMediaPlayer = null);
        }

        mCamera = findViewById(R.id.camera);
        mCamera.setLifecycleOwner(this);
        mCamera.setMode(Mode.VIDEO);
        ImageButton close = findViewById(R.id.close);
        close.setOnClickListener(view -> confirmClose());
        View sheet = findViewById(R.id.timer_sheet);
        final BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
        sheet.findViewById(R.id.close)
                .setOnClickListener(view -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        sheet.findViewById(R.id.start)
                .setOnClickListener(view -> {
                    bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    startTimer();
                });
        ImageButton done = findViewById(R.id.done);
        done.setOnClickListener(view -> {
            if (mModel.segments.isEmpty()) {
                Toast.makeText(RecorderActivity.this, R.string.recorder_error_no_videos, Toast.LENGTH_SHORT).show();
            } else {
                commitRecordings();
            }
        });
        ImageButton flash = findViewById(R.id.flash);
        flash.setOnClickListener(view -> mCamera.setFlash(mCamera.getFlash() == Flash.OFF ? Flash.TORCH : Flash.OFF));
        ImageButton flip = findViewById(R.id.flip);
        flip.setOnClickListener(view -> mCamera.toggleFacing());
        TextView maximum = findViewById(R.id.maximum);
        ImageButton record = findViewById(R.id.record);
        record.setOnClickListener(view -> {
            if (mCamera.isTakingVideo()) {
                stopRecording();
            } else {
                startRecording();
            }
        });
        TextView sound = findViewById(R.id.sound);
        sound.setOnClickListener(view -> {
            Intent intent = new Intent(RecorderActivity.this, SongPickerActivity.class);
            startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_SONG);
        });
        Slider selection = findViewById(R.id.selection);
        selection.setLabelFormatter(value -> TextFormatUtil.toMMSS((long)value));
        ImageButton timer = findViewById(R.id.timer);
        timer.setOnClickListener(view -> bsb.setState(BottomSheetBehavior.STATE_EXPANDED));
        ImageButton upload = findViewById(R.id.upload);
        upload.setOnClickListener(view -> {
            int status1 = ActivityCompat.checkSelfPermission(
                    RecorderActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int status2 = ActivityCompat.checkSelfPermission(
                    RecorderActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean status = status1 == PackageManager.PERMISSION_GRANTED
                    && status2 == PackageManager.PERMISSION_GRANTED;
            if (status) {
                showVideoPicker();
            } else {
                String[] permissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
                ActivityCompat.requestPermissions(
                        RecorderActivity.this,
                        permissions,
                        SharedConstants.REQUEST_CODE_READ_STORAGE);
            }
        });
        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View v, int state) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    long max;
                    max = SharedConstants.MAX_DURATION - mModel.recorded();
                    max = TimeUnit.MILLISECONDS.toSeconds(max);
                    max = TimeUnit.SECONDS.toMillis(max);
                    selection.setValue(0);
                    selection.setValueTo(max);
                    selection.setValue(max);
                    maximum.setText(TextFormatUtil.toMMSS(max));
                }
            }

            @Override
            public void onSlide(@NonNull View v, float offset) { }
        });
        final SegmentedProgressBar segments = findViewById(R.id.segments);
        segments.enableAutoProgressView(SharedConstants.MAX_DURATION);
        segments.setDividerColor(Color.BLACK);
        segments.setDividerEnabled(true);
        segments.setDividerWidth(2f);
        segments.setListener(l -> { /* eaten */ });
        segments.setShader(new int[]{
                ContextCompat.getColor(this, R.color.colorAccent),
                ContextCompat.getColor(this, R.color.colorAccent),
        });
        mCamera.addCameraListener(new CameraListener() {

            @Override
            public void onVideoRecordingEnd() {
                Log.v(TAG, "Video recording has ended.");
                record.setSelected(false);
                record.clearAnimation();
                if (mMediaPlayer != null) {
                    mMediaPlayer.pause();
                }

                mHandler.postDelayed(() -> processCurrentRecording(), 250);
                segments.pause();
                segments.addDivider();
            }

            @Override
            public void onVideoRecordingStart() {
                Log.v(TAG, "Video recording has started.");
                record.setSelected(true);
                record.startAnimation(
                        AnimationUtils.loadAnimation(
                                RecorderActivity.this, R.anim.scale));
                if (mMediaPlayer != null) {
                    mMediaPlayer.start();
                }

                segments.resume();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SharedConstants.REQUEST_CODE_READ_STORAGE) {
            boolean ok = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                showVideoPicker();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void closeFinally(File video) {
        Log.v(TAG, "Finalised recorded video at " + video);
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra(FilterActivity.EXTRA_SONG, mModel.song);
        intent.putExtra(FilterActivity.EXTRA_VIDEO, video.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void commitRecordings() {
        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        List<String> videos = new ArrayList<>();
        for (RecordSegment segment : mModel.segments) {
            videos.add(segment.file.getAbsolutePath());
        }

        final File merged1 = new File(getCacheDir(), UUID.randomUUID().toString());
        Data data1 = new Data.Builder()
                .putStringArray(MergeVideosWorker.KEY_VIDEOS, videos.toArray(new String[0]))
                .putString(MergeVideosWorker.KEY_OUTPUT, merged1.getAbsolutePath())
                .build();
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(MergeVideosWorker.class)
                .setInputData(data1)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        if (mModel.audio != null) {
            final File merged2 = new File(getCacheDir(), UUID.randomUUID().toString());
            Data data2 = new Data.Builder()
                    .putString(MergeAudioVideoWorker.KEY_AUDIO, mModel.audio.getPath())
                    .putString(MergeAudioVideoWorker.KEY_VIDEO, merged1.getAbsolutePath())
                    .putString(MergeAudioVideoWorker.KEY_OUTPUT, merged2.getAbsolutePath())
                    .build();
            OneTimeWorkRequest request2 =
                    new OneTimeWorkRequest.Builder(MergeAudioVideoWorker.class)
                            .setInputData(data2)
                            .build();
            wm.beginWith(request1).then(request2).enqueue();
            wm.getWorkInfoByIdLiveData(request2.getId())
                    .observe(this, info -> {
                        boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                || info.getState() == WorkInfo.State.FAILED;
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            progress.dismiss();
                            closeFinally(merged2);
                        } else if (ended) {
                            progress.dismiss();
                        }
                    });
        } else {
            wm.enqueue(request1);
            wm.getWorkInfoByIdLiveData(request1.getId())
                    .observe(this, info -> {
                        boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                || info.getState() == WorkInfo.State.FAILED;
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            closeFinally(merged1);
                        } else if (ended) {
                            progress.dismiss();
                        }
                    });
        }
    }

    private void confirmClose() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirmation_close_recording)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.close_button, (dialog, i) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private void processCurrentRecording() {
        MediaMetadataRetriever mmr = null;
        long duration = 0;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(RecorderActivity.this, Uri.fromFile(mModel.video));
            String millis = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (millis != null) {
                duration = Long.parseLong(millis);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve duration from video file.");
        } finally {
            if (mmr != null) {
                try {
                    mmr.release();
                } catch (IOException ignore) {
                }
            }
        }

        if (duration >= 500) {
            RecordSegment segment = new RecordSegment();
            segment.file = mModel.video;
            segment.duration = duration;
            mModel.segments.add(segment);
        }

        mModel.video = null;
    }

    private void showVideoPicker() {
        new VideoPicker.Builder(RecorderActivity.this)
                .mode(VideoPicker.Mode.GALLERY)
                .directory(VideoPicker.Directory.DEFAULT)
                .extension(VideoPicker.Extension.MP4)
                .enableDebuggingMode(true)
                .build();
    }

    private void startRecording() {
        long recorded = mModel.recorded();
        if (recorded >= SharedConstants.MAX_DURATION) {
            Toast.makeText(RecorderActivity.this, R.string.recorder_error_maxed_out, Toast.LENGTH_SHORT).show();
        } else {
            mModel.video = new File(getCacheDir(), UUID.randomUUID().toString());
            mCamera.takeVideoSnapshot(
                    mModel.video, (int)(SharedConstants.MAX_DURATION - recorded));
        }
    }

    @SuppressLint("SetTextI18n")
    private void startTimer() {
        View countdown = findViewById(R.id.countdown);
        TextView count = findViewById(R.id.count);
        count.setText(null);
        Slider selection = findViewById(R.id.selection);
        long duration = (long)selection.getValue();
        CountDownTimer timer = new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long remaining) {
                mHandler.post(() -> {
                    count.setText(TimeUnit.MILLISECONDS.toSeconds(remaining) + 1 + "");
                });
            }

            @Override
            public void onFinish() {
                mHandler.post(() -> countdown.setVisibility(View.GONE));
                startRecording();
                mHandler.postDelayed(mStopper, duration);
            }
        };
        countdown.setOnClickListener(v -> {
            timer.cancel();
            countdown.setVisibility(View.GONE);
        });
        countdown.setVisibility(View.VISIBLE);
        timer.start();
    }

    private void stopRecording() {
        mCamera.stopVideo();
        mHandler.removeCallbacks(mStopper);
    }

    public static class RecorderActivityViewModel extends ViewModel {

        public Uri audio;
        public final List<RecordSegment> segments = new ArrayList<>();
        public int song = 0;
        public File video;

        public long recorded() {
            long recorded = 0;
            for (RecordSegment segment : segments) {
                recorded += segment.duration;
            }

            return recorded;
        }
    }

    private static class RecordSegment {

        public File file;
        public long duration;
    }
}
