package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MergeVideosWorker extends Worker {

    public static final String KEY_OUTPUT = "output";
    public static final String KEY_VIDEOS = "videos";
    public static final String TAG = "MergeVideosWorker";

    public MergeVideosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] paths = getInputData().getStringArray(KEY_VIDEOS);
        String output = getInputData().getString(KEY_OUTPUT);
        List<Track> audios = new ArrayList<>();
        List<Track> videos = new ArrayList<>();
        FileOutputStream os = null;
        try {
            //noinspection ConstantConditions
            for (String video : paths) {
                Movie movie = MovieCreator.build(video);
                for (Track track : movie.getTracks()) {
                    if (TextUtils.equals(track.getHandler(), "soun")) {
                        audios.add(track);
                    } else if (TextUtils.equals(track.getHandler(), "vide")) {
                        videos.add(track);
                    }
                }
            }

            Movie merged = new Movie();
            if (!audios.isEmpty()) {
                merged.addTrack(new AppendTrack(audios.toArray(new Track[0])));
            }

            if (!videos.isEmpty()) {
                merged.addTrack(new AppendTrack(videos.toArray(new Track[0])));
            }

            Container mp4 = new DefaultMp4Builder().build(merged);
            //noinspection ConstantConditions
            os = new FileOutputStream(new File(output));
            mp4.writeContainer(os.getChannel());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Filed to output at " + output, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                }
            }
        }

        return Result.failure();
    }
}
