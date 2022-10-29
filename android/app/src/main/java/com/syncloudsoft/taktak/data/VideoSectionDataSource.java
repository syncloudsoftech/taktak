package com.syncloudsoft.taktak.data;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.syncloudsoft.taktak.common.LoadingState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoSectionDataSource extends PageKeyedDataSource<Integer, VideoSectionDataSource.VideoSection> {

    private static final String TAG = "VideoSectionDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public VideoSectionDataSource(String url, String token) {
        mServerToken = token;
        mServerUrl = url;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, VideoSection> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching video sections has failed.", e);
                state.postValue(LoadingState.ERROR);
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
                        state.postValue(LoadingState.LOADED);
                        callback.onResult(transformData(data),null, 2);
                    } else {
                        state.postValue(LoadingState.ERROR);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response from server.", e);
                    state.postValue(LoadingState.ERROR);
                }
            }
        });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, VideoSection> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, VideoSection> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching video sections has failed.", e);
                state.postValue(LoadingState.ERROR);
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
                        callback.onResult(transformData(data),params.key + 1);
                        state.postValue(LoadingState.LOADED);
                    } else {
                        state.postValue(LoadingState.ERROR);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response from server.", e);
                    state.postValue(LoadingState.ERROR);
                }
            }
        });
    }

    private Request createRequest(int page, int count) {
        Request.Builder builder = new Request.Builder()
                .url(mServerUrl + "api/video-sections?page=" + page + "&count=" + count)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder.header("Authorization", "Bearer " + mServerToken);
        }

        return builder.build();
    }

    private List<VideoSection> transformData(JSONArray data) throws JSONException {
        List<VideoSection> sections = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            VideoSection section = VideoSection.transform(object);
            sections.add(section);
        }

        return sections;
    }

    public static class VideoSection {

        public int id;
        public String name;

        public static VideoSection transform(JSONObject object) throws JSONException {
            VideoSection section = new VideoSection();
            section.id = object.getInt("id");
            section.name = object.getString("name");
            return section;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, VideoSection> {

        private final String mServerToken;
        private final String mServerUrl;

        public MutableLiveData<VideoSectionDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token) {
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, VideoSection> create() {
            VideoSectionDataSource source = new VideoSectionDataSource(mServerUrl, mServerToken);
            this.source.postValue(source);
            return source;
        }
    }
}
