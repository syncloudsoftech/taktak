package com.syncloudsoft.taktak.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MergeAudioVideoWorker extends Worker {

    public static final String KEY_AUDIO = "audio";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_VIDEO = "video";
    public static final String TAG = "MergeAudioVideoWorker";

    public MergeAudioVideoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String audio = getInputData().getString(KEY_AUDIO);
        String video = getInputData().getString(KEY_VIDEO);
        String output = getInputData().getString(KEY_OUTPUT);
        FileOutputStream os = null;
        try {
            //noinspection ConstantConditions
            Movie temp = MovieCreator.build(video);
            Track v = null;
            for (Track track : temp.getTracks()) {
                if (TextUtils.equals(track.getHandler(), "vide")) {
                    v = track;
                    break;
                }
            }

            Movie merged = new Movie();
            //noinspection ConstantConditions
            merged.addTrack(v);
            //noinspection ConstantConditions
            if (audio.endsWith(".mp4")) {
                Track a = null;
                for (Track track : temp.getTracks()) {
                    if (TextUtils.equals(track.getHandler(), "soun")) {
                        a = track;
                        break;
                    }
                }

                //noinspection ConstantConditions
                merged.addTrack(crop(video, a));
            } else {
                merged.addTrack(crop(video, new AACTrackImpl(new FileDataSourceImpl(audio))));
            }

            Container mp4 = new DefaultMp4Builder().build(merged);
            //noinspection ConstantConditions
            os = new FileOutputStream(new File(output));
            mp4.writeContainer(os.getChannel());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to output at " + output, e);
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

    @SuppressWarnings("SameParameterValue")
    private static CroppedTrack crop(String video, Track audio) throws IOException {
        IsoFile iso = new IsoFile(video);
        MovieHeaderBox header = iso.getMovieBox().getMovieHeaderBox();
        double duration = (double) header.getDuration() / header.getTimescale();
        double time_c = 0; // current time
        double time_p = -1; // previous time
        long sample_c = 0; // current sample
        long sample_s = -1; // start sample
        long sample_e = -1; // end sample
        for (int i = 0; i < audio.getSampleDurations().length; i++) {
            long delta = audio.getSampleDurations()[i];
            if (time_c > time_p && time_c <= 0) {
                sample_s = sample_c;
            }

            if (time_c > time_p && time_c <= duration) {
                sample_e = sample_c;
            }

            time_p = time_c;
            time_c += (double) delta / (double) audio.getTrackMetaData().getTimescale();
            sample_c++;
        }

        return new CroppedTrack(audio, sample_s, sample_e);
    }
}
