package com.syncloudsoft.taktak.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.utils.VideoUtil;
import com.syncloudsoft.taktak.workers.GeneratePreviewWorker;
import com.syncloudsoft.taktak.workers.PostVideoWorker;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PostVideoActivity extends AppCompatActivity {

    public static final String EXTRA_SONG = "song";
    public static final String EXTRA_VIDEO = "video";
    public static final String TAG = "PostVideoActivity";

    private PostViewActivityViewModel mModel;
    private int mSong;
    private String mVideo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_video);
        mModel = new ViewModelProvider(this).get(PostViewActivityViewModel.class);
        mSong = getIntent().getIntExtra(EXTRA_SONG, 0);
        mVideo = getIntent().getStringExtra(EXTRA_VIDEO);
        EditText description = findViewById(R.id.description);
        description.setText(mModel.description);
        description.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.description = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        ImageView thumbnail = findViewById(R.id.thumbnail);
        long duration = VideoUtil.getDuration(mVideo);
        long timestamp = TimeUnit.SECONDS.toMicros(2);
        if (timestamp > duration) {
            timestamp = duration;
        }
        Bitmap image = VideoUtil.getFrameAtTime(mVideo, timestamp);
        thumbnail.setImageBitmap(image);
        SwitchMaterial private2 = findViewById(R.id.private2);
        private2.setChecked(mModel.private2);
        private2.setOnCheckedChangeListener((button, checked) -> mModel.private2 = checked);
        SwitchMaterial comments = findViewById(R.id.comments);
        comments.setChecked(mModel.comments);
        comments.setOnCheckedChangeListener((button, checked) -> mModel.comments = checked);
        Button upload = findViewById(R.id.upload);
        upload.setOnClickListener(v -> uploadToServer());
    }

    private void uploadToServer() {
        File preview = new File(getFilesDir(), UUID.randomUUID().toString());
        File screenshot = new File(getFilesDir(), UUID.randomUUID().toString());
        Data data1 = new Data.Builder()
                .putString(GeneratePreviewWorker.KEY_INPUT, mVideo)
                .putString(GeneratePreviewWorker.KEY_SCREENSHOT, screenshot.getAbsolutePath())
                .putString(GeneratePreviewWorker.KEY_PREVIEW, preview.getAbsolutePath())
                .build();
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(GeneratePreviewWorker.class)
                .setInputData(data1)
                .build();
        String token = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Data data2 = new Data.Builder()
                .putInt(PostVideoWorker.KEY_SONG, mSong)
                .putString(PostVideoWorker.KEY_VIDEO, mVideo)
                .putString(PostVideoWorker.KEY_SCREENSHOT, screenshot.getAbsolutePath())
                .putString(PostVideoWorker.KEY_PREVIEW, preview.getAbsolutePath())
                .putString(PostVideoWorker.KEY_DESCRIPTION, mModel.description)
                .putBoolean(PostVideoWorker.KEY_PRIVATE, mModel.private2)
                .putBoolean(PostVideoWorker.KEY_COMMENTS, mModel.comments)
                .putString(PostVideoWorker.KEY_TOKEN, token)
                .build();
        OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(PostVideoWorker.class)
                .setInputData(data2)
                .build();
        WorkManager.getInstance(this).beginWith(request1).then(request2).enqueue();
        Toast.makeText(this, R.string.uploading_message, Toast.LENGTH_SHORT).show();
        finish();
    }

    public static class PostViewActivityViewModel extends ViewModel {

        public String description = "";
        public boolean private2 = false;
        public boolean comments = true;
    }
}
