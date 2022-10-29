package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.syncloudsoft.taktak.MainApplication;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.activities.RecorderActivity;
import com.syncloudsoft.taktak.data.VideoDataSource;
import com.syncloudsoft.taktak.workers.FileDownloadWorker;
import com.syncloudsoft.taktak.workers.WatermarkWorker;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlayerFragment extends Fragment implements AnalyticsListener {

    private static final String ARG_VIDEO = "video";
    private static final String TAG = "PlayerFragment";

    private View mBufferingProgressBar;
    private final OkHttpClient mHttpClient = new OkHttpClient();
    private ImageButton mLikeButton;
    private PlayerFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private SimpleExoPlayer mPlayer;
    private VideoDataSource.Video mVideo;
    private View mMusicDisc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideo = requireArguments().getParcelable(ARG_VIDEO);
        DefaultLoadControl control = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        5 * 1000,
                        10 * 1000,
                        1000,
                        1000)
                .createDefaultLoadControl();
        mPlayer = new SimpleExoPlayer.Builder(requireContext())
                .setLoadControl(control)
                .build();
        mPlayer.addAnalyticsListener(this);
        mModel1 = new ViewModelProvider(this).get(PlayerFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            stopPlayer();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            stopPlayer();
        }

        mMusicDisc.clearAnimation();
    }

    @Override
    public void onPlayerStateChanged(EventTime time, boolean play, @Player.State int state) {
        if (mBufferingProgressBar != null) {
            mBufferingProgressBar.setVisibility(
                    state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startPlayer();
        mMusicDisc.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_360));
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBufferingProgressBar = view.findViewById(R.id.buffering);
        mMusicDisc = view.findViewById(R.id.disc);
        mMusicDisc.setOnClickListener(v -> {
            if (mModel2.isLoggedIn) {
                confirmUseAudio();
            } else {
                ((MainActivity) requireActivity()).showLoginSheet();
            }
        });
        PlayerView player = view.findViewById(R.id.player);
        player.setPlayer(mPlayer);
        TextView likes = view.findViewById(R.id.likes);
        likes.setText(mVideo.likesCount + "");
        mLikeButton = view.findViewById(R.id.like);
        mLikeButton.setOnClickListener(v -> {
            if (mModel2.isLoggedIn) {
                if (mVideo.isLiked) {
                    revokeLike();
                    mVideo.isLiked = false;
                    likes.setText(--mVideo.likesCount + "");
                } else {
                    submitLike();
                    mVideo.isLiked = true;
                    likes.setText(++mVideo.likesCount + "");
                }

                refreshLike();
            } else {
                ((MainActivity) requireActivity()).showLoginSheet();
            }
        });
        ImageButton comment = view.findViewById(R.id.comment);
        comment.setOnClickListener(v -> showComments(mVideo.id));
        comment.setVisibility(mVideo.comments ? View.VISIBLE : View.GONE);
        TextView comments = view.findViewById(R.id.comments);
        comments.setText(mVideo.commentsCount + "");
        comments.setVisibility(mVideo.comments ? View.VISIBLE : View.GONE);
        ImageButton share = view.findViewById(R.id.share);
        share.setOnClickListener(v ->
                downloadAndRun(true, file -> shareVideo(requireContext(), file)));
        SimpleDraweeView photo = view.findViewById(R.id.photo);
        if (TextUtils.isEmpty(mVideo.userPhoto)) {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        } else {
            photo.setImageURI(mVideo.userPhoto);
        }

        view.findViewById(R.id.badge)
                .setOnClickListener(v -> showProfile(mVideo.userId));
        TextView username = view.findViewById(R.id.username);
        username.setOnClickListener(v -> showProfile(mVideo.userId));
        username.setText('@' + mVideo.userUsername);
        view.findViewById(R.id.verified)
                .setVisibility(mVideo.userVerified ? View.VISIBLE : View.GONE);
        TextView description = view.findViewById(R.id.description);
        description.setText(mVideo.description);
        description.setVisibility(TextUtils.isEmpty(mVideo.description) ? View.GONE : View.VISIBLE);
        TextView song = view.findViewById(R.id.song);
        if (mVideo.songId != null) {
            song.setText(mVideo.songName);
        } else {
            song.setText(R.string.original_audio);
        }

        song.setSelected(true);
        refreshLike();
    }

    private void confirmUseAudio() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_use_audio)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    downloadAndRun(false, file -> {
                        Intent intent = new Intent(requireContext(), RecorderActivity.class);
                        intent.putExtra(RecorderActivity.EXTRA_AUDIO, Uri.fromFile(file));
                        startActivity(intent);
                    });
                })
                .show();
    }

    private void downloadAndRun(boolean watermark, OnFinish callback) {
        File videos = new File(requireContext().getFilesDir(), "videos");
        if (!videos.exists() && !videos.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + videos);
        }

        File processed = new File(videos, mVideo.id + ".mp4");
        if (processed.exists()) {
            callback.finished(processed);
            return;
        }

        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        File original;
        if (watermark && !getResources().getBoolean(R.bool.watermark_enabled)) {
            original = processed;
        } else {
            original = new File(
                    requireContext().getCacheDir(),
                    UUID.randomUUID().toString() + ".mp4");
        }
        Data data1 = new Data.Builder()
                .putString(FileDownloadWorker.KEY_URL, mVideo.video)
                .putString(FileDownloadWorker.KEY_PATH, original.getAbsolutePath())
                .build();
        OneTimeWorkRequest request1 = new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
                .setInputData(data1)
                .build();
        WorkManager wm = WorkManager.getInstance(requireContext());
        if (watermark && getResources().getBoolean(R.bool.watermark_enabled)) {
            Data data2 = new Data.Builder()
                    .putString(WatermarkWorker.KEY_INPUT, original.getAbsolutePath())
                    .putString(WatermarkWorker.KEY_OUTPUT, processed.getAbsolutePath())
                    .build();
            OneTimeWorkRequest request2 = new OneTimeWorkRequest.Builder(WatermarkWorker.class)
                    .setInputData(data2)
                    .build();
            wm.beginWith(request1).then(request2).enqueue();
            wm.getWorkInfoByIdLiveData(request2.getId())
                    .observe(getViewLifecycleOwner(), info -> {
                        boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                || info.getState() == WorkInfo.State.FAILED;
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            progress.dismiss();
                            callback.finished(processed);
                        } else if (ended) {
                            progress.dismiss();
                        }
                    });
        } else {
            wm.enqueue(request1);
            wm.getWorkInfoByIdLiveData(request1.getId())
                    .observe(getViewLifecycleOwner(), info -> {
                        boolean ended = info.getState() == WorkInfo.State.CANCELLED
                                || info.getState() == WorkInfo.State.FAILED;
                        if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            progress.dismiss();
                            callback.finished(original);
                        } else if (ended) {
                            progress.dismiss();
                        }
                    });
        }
    }

    public static PlayerFragment newInstance(VideoDataSource.Video video) {
        PlayerFragment fragment = new PlayerFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_VIDEO, video);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void refreshLike() {
        if (mVideo.isLiked) {
            mLikeButton.setImageResource(R.drawable.ic_baseline_favorite_24_color);
        } else {
            mLikeButton.setImageResource(R.drawable.ic_baseline_favorite_24_shadow);
        }
    }

    private void revokeLike() {
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/videos/" + mVideo.id + "/like")
                .delete()
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to revoke existing like on video.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Revoking like for video returned " + code + '.');
            }
        });
        mVideo.isLiked = false;
    }

    private void shareVideo(Context context, File file) {
        Log.v(TAG, "Showing sharing options for " + file);
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName(), file);
        Intent intent = ShareCompat.IntentBuilder.from(requireActivity())
                .setStream(uri)
                .setText(getString(R.string.share_video_text, context.getPackageName()))
                .setType("video/mp4")
                .setChooserTitle(getString(R.string.share_video_title))
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void showComments(int video) {
        ((MainActivity)requireActivity()).showCommentsPage(video);
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void startPlayer() {
        HttpProxyCacheServer cache = MainApplication.getProxyServer(requireContext());
        DefaultDataSourceFactory factory =
                new DefaultDataSourceFactory(requireContext(), getString(R.string.app_name));
        ProgressiveMediaSource source = new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(cache.getProxyUrl(mVideo.video))));
        mPlayer.setPlayWhenReady(true);
        mPlayer.seekTo(mModel1.window, mModel1.position);
        mPlayer.prepare(new LoopingMediaSource(source), false, false);
    }

    private void stopPlayer() {
        mModel1.position = mPlayer.getCurrentPosition();
        mModel1.window = mPlayer.getCurrentWindowIndex();
        mPlayer.stop(true);
    }

    private void submitLike() {
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/videos/" + mVideo.id + "/like")
                .post(RequestBody.create(null, new byte[0]))
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to submit existing like on video.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Submitting like for video returned " + code + '.');
            }
        });
    }

    public static class PlayerFragmentViewModel extends ViewModel {

        public long position = 0;
        public int window = 0;
    }

    private interface OnFinish {

        void finished(File file);
    }
}
