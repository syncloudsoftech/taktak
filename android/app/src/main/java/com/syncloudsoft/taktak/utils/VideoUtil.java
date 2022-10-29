package com.syncloudsoft.taktak.utils;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class VideoUtil {

    private static final String TAG = "VideoUtil";

    @NotNull
    public static Size getDimensions(String path) {
        int width = 0, height = 0;
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (w != null && h != null) {
                width = Integer.parseInt(w);
                height = Integer.parseInt(h);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + path, e);
        } finally {
            if (mmr != null) {
                try {
                    mmr.release();
                } catch (IOException ignore) {
                }
            }
        }

        return new Size(width, height);
    }

    public static long getDuration(String path) {
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (!TextUtils.isEmpty(duration)) {
                //noinspection ConstantConditions
                return Long.parseLong(duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + path, e);
        } finally {
            if (mmr != null) {
                try {
                    mmr.release();
                } catch (IOException ignore) {
                }
            }
        }

        return 0;
    }

    @Nullable
    public static Bitmap getFrameAtTime(String path, long micros) {
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            return mmr.getFrameAtTime(micros);
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + path, e);
        } finally {
            if (mmr != null) {
                try {
                    mmr.release();
                } catch (IOException ignore) {
                }
            }
        }

        return null;
    }
}
