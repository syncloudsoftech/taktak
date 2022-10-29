package com.syncloudsoft.taktak.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.syncloudsoft.taktak.data.SongDataSource;
import com.syncloudsoft.taktak.workers.FileDownloadWorker;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.fragments.SongPickerFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SongPickerActivity extends AppCompatActivity implements SongPickerFragment.OnSongSelectListener {

    public static String EXTRA_SONG_FILE = "song_file";
    public static String EXTRA_SONG_ID = "song_id";
    public static String EXTRA_SONG_NAME = "song_name";
    private static final String TAG = "SongPickerActivity";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private SongPickerActivityViewModel mModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_picker);
        findViewById(R.id.close).setOnClickListener(v -> finish());
        final View loading = findViewById(R.id.loading);
        mModel = new ViewModelProvider(this).get(SongPickerActivityViewModel.class);
        mModel.state.observe(this, state -> {
            if (state == LoadingState.LOADED) {
                setupTabs();
            } else if (state == LoadingState.ERROR) {
                Toast.makeText(SongPickerActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadingState state = mModel.state.getValue();
        if (state != LoadingState.LOADING && state != LoadingState.LOADED) {
            fetchSections();
        }
    }

    @Override
    public void onSongSelect(final SongDataSource.Song song) {
        File songs = new File(getFilesDir(), "songs");
        if (!songs.exists() && !songs.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + songs);
        }

        final File audio = new File(songs, song.id + ".aac");
        if (audio.exists()) {
            closeWithSelection(song, Uri.fromFile(audio));
            return;
        }

        KProgressHUD progress = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        Data input = new Data.Builder()
                .putString(FileDownloadWorker.KEY_URL, song.audio)
                .putString(FileDownloadWorker.KEY_PATH, audio.getAbsolutePath())
                .build();
        WorkRequest request = new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
                .setInputData(input)
                .build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(this, info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED;
                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        progress.dismiss();
                        closeWithSelection(song, Uri.fromFile(audio));
                    } else if (ended) {
                        progress.dismiss();
                    }
                });
    }

    private void closeWithSelection(SongDataSource.Song song, Uri file) {
        Intent data = new Intent();
        data.putExtra(EXTRA_SONG_ID, song.id);
        data.putExtra(EXTRA_SONG_NAME, song.name);
        data.putExtra(EXTRA_SONG_FILE, file);
        setResult(RESULT_OK, data);
        finish();
    }

    private void fetchSections() {
        mModel.state.postValue(LoadingState.LOADING);
        String token = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/song-sections?count=100")
                .get()
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching song sections has failed.", e);
                mModel.state.postValue(LoadingState.ERROR);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                try {
                    //noinspection ConstantConditions
                    Log.v(TAG, "Server responded with " + response.code() + " status.");
                    if (response.isSuccessful()) {
                        //noinspection ConstantConditions
                        String content = response.body().string();
                        Log.v(TAG, "Server sent:\n" + content);
                        JSONObject json = new JSONObject(content);
                        JSONArray data = json.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject object = data.getJSONObject(i);
                            SongSection section = new SongSection();
                            section.id = object.getInt("id");
                            section.name = object.getString("name");
                            mModel.sections.add(section);
                        }

                        mModel.state.postValue(LoadingState.LOADED);
                    } else {
                        mModel.state.postValue(LoadingState.ERROR);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response from server.", e);
                    Toast.makeText(SongPickerActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                    mModel.state.postValue(LoadingState.ERROR);
                }
            }
        });
    }

    private void setupTabs() {
        ViewPager2 pager = findViewById(R.id.pager);
        pager.setAdapter(new SongPickerPagerAdapter(this));
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            SongSection section = mModel.sections.get(position);
            tab.setText(section.name);
        }).attach();
    }

    public static class SongPickerActivityViewModel extends ViewModel {

        public final List<SongSection> sections = new ArrayList<>();
        public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);
    }

    private class SongPickerPagerAdapter extends FragmentStateAdapter {

        public SongPickerPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            SongSection section = mModel.sections.get(position);
            SongPickerFragment fragment = SongPickerFragment.newInstance(section.id);
            fragment.setOnSongSelectListener(SongPickerActivity.this);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return mModel.sections.size();
        }
    }

    public static class SongSection {

        public int id;
        public String name;
    }
}
