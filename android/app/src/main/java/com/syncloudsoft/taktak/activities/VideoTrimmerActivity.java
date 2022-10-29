package com.syncloudsoft.taktak.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;

import java.io.File;

import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class VideoTrimmerActivity extends AppCompatActivity implements OnTrimVideoListener {

    public static final String EXTRA_VIDEO = "video";
    public static final String TAG = "VideoTrimmerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_trimmer);
        String video = getIntent().getStringExtra(EXTRA_VIDEO);
        Log.v(TAG, "To trim video file path is " + video);
        File trimmed = new File(getCacheDir(), "trimmed");
        if (!trimmed.exists() && !trimmed.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + trimmed);
        }

        K4LVideoTrimmer trimmer = ((K4LVideoTrimmer) findViewById(R.id.trimmer));
        trimmer.setDestinationPath(trimmed.getAbsolutePath());
        trimmer.setMaxDuration((int) SharedConstants.MAX_DURATION);
        trimmer.setOnTrimVideoListener(this);
        //noinspection ConstantConditions
        trimmer.setVideoURI(Uri.fromFile(new File(video)));
    }

    @Override
    public void cancelAction() {
        finish();
    }

    @Override
    public void getResult(Uri uri) {
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra(FilterActivity.EXTRA_VIDEO, uri.getPath());
        startActivity(intent);
        finish();
    }
}
