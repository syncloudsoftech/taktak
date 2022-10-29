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

public class NotificationDataSource extends PageKeyedDataSource<Integer, NotificationDataSource.Notification> {

    private static final String TAG = "NotificationDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public NotificationDataSource(String url, String token) {
        mServerToken = token;
        mServerUrl = url;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Notification> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching notifications has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Notification> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Notification> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching notifications has failed.", e);
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
                .url(mServerUrl + "api/notifications?page=" + page + "&count=" + count)
                .get()
                .header("Authorization", "Bearer " + mServerToken)
                .build();
    }

    private List<Notification> transformData(JSONArray data) throws JSONException {
        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Notification notification = Notification.transform(object);
            notifications.add(notification);
        }

        return notifications;
    }

    public static class Notification {

        public int id;
        public String content;
        public int sourceId;
        public String sourceUsername;
        public String sourcePhoto;
        public Integer videoId;
        public String videoScreenshot;
        public Date dateCreated;

        public static Notification transform(JSONObject object) throws JSONException {
            Notification notification = new Notification();
            notification.id = object.getInt("id");
            notification.content = object.getString("content");
            notification.sourceId = object.getInt("source_id");
            notification.sourceUsername = object.getString("source_username");
            notification.sourcePhoto = JsonUtil.optString(object, "source_photo");
            notification.videoId = object.optInt("video_id");
            if (notification.videoId <= 0) {
                notification.videoId = null;
            }

            notification.videoScreenshot = JsonUtil.optString(object, "video_screenshot");
            String date = object.getString("date_created");
            notification.dateCreated = TextFormatUtil.toDate(date);
            return notification;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, Notification> {

        private final String mServerToken;
        private final String mServerUrl;

        public MutableLiveData<NotificationDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token) {
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, Notification> create() {
            NotificationDataSource source = new NotificationDataSource(mServerUrl, mServerToken);
            this.source.postValue(source);
            return source;
        }
    }
}
