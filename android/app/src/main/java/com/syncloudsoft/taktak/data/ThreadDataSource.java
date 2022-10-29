package com.syncloudsoft.taktak.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.utils.JsonUtil;
import com.syncloudsoft.taktak.utils.TextFormatUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ThreadDataSource extends PageKeyedDataSource<Integer, ThreadDataSource.Thread> {

    private static final String TAG = "ThreadDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public ThreadDataSource(String url, String token) {
        mServerToken = token;
        mServerUrl = url;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Thread> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching threads has failed.", e);
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
                        callback.onResult(transformData(data),null, 2);
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

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Thread> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Thread> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching threads has failed.", e);
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
        return new Request.Builder()
                .url(mServerUrl + "api/threads?page=" + page + "&count=" + count)
                .get()
                .header("Authorization", "Bearer " + mServerToken)
                .build();
    }

    private List<Thread> transformData(JSONArray data) throws JSONException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Thread thread = Thread.transform(object);
            threads.add(thread);
        }

        return threads;
    }

    public static class Factory extends DataSource.Factory<Integer, Thread> {

        private final String mServerToken;
        private final String mServerUrl;

        public MutableLiveData<ThreadDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token) {
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, Thread> create() {
            ThreadDataSource source = new ThreadDataSource(mServerUrl, mServerToken);
            this.source.postValue(source);
            return source;
        }
    }

    public static class Thread {

        public int id;
        public int userId;
        public String userUsername;
        @Nullable public String userPhoto;
        @Nullable public String lastMessageText;
        @Nullable public Date lastMessageDateCreated;
        public Date dateCreated;

        public static Thread transform(JSONObject object) throws JSONException {
            Thread thread = new Thread();
            thread.id = object.getInt("id");
            thread.userId = object.getInt("user_id");
            thread.userUsername = object.getString("user_username");
            thread.userPhoto = JsonUtil.optString(object, "user_photo");
            thread.lastMessageText = JsonUtil.optString(object, "last_message_text");
            String date;
            date = JsonUtil.optString(object, "last_message_date_created");
            if (date != null) {
                thread.lastMessageDateCreated = TextFormatUtil.toDate(date);
            }

            date = object.getString("date_created");
            thread.dateCreated = TextFormatUtil.toDate(date);
            return thread;
        }
    }
}
