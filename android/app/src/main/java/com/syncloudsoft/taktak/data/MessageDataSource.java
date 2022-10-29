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

public class MessageDataSource extends PageKeyedDataSource<Integer, MessageDataSource.Message> {

    private static final String TAG = "MessageDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;
    private final int mThread;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public MessageDataSource(String url, String token, int thread) {
        mServerToken = token;
        mServerUrl = url;
        mThread = thread;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Message> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching messages has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Message> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Message> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching messages has failed.", e);
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
                .url(mServerUrl + "api/threads/" + mThread + "/messages?page=" + page + "&count=" + count)
                .get()
                .header("Authorization", "Bearer " + mServerToken)
                .build();
    }

    private List<Message> transformData(JSONArray data) throws JSONException {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Message message = Message.transform(object);
            messages.add(message);
        }

        return messages;
    }

    public static class Factory extends DataSource.Factory<Integer, Message> {

        private final String mServerToken;
        private final String mServerUrl;
        private final int mThread;

        public MutableLiveData<MessageDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token, int thread) {
            mServerToken = token;
            mServerUrl = url;
            mThread = thread;
        }

        @NonNull
        @Override
        public DataSource<Integer, Message> create() {
            MessageDataSource source = new MessageDataSource(mServerUrl, mServerToken, mThread);
            this.source.postValue(source);
            return source;
        }
    }

    public static class Message {

        public int id;
        public int userId;
        public String userUsername;
        @Nullable public String userPhoto;
        public boolean userVerified;
        public boolean inbox;
        public String text;
        public Date dateCreated;

        public static Message transform(JSONObject object) throws JSONException {
            Message message = new Message();
            message.id = object.getInt("id");
            message.userId = object.getInt("user_id");
            message.userUsername = object.getString("user_username");
            message.userPhoto = JsonUtil.optString(object, "user_photo");
            message.userVerified = object.getInt("user_verified") == 1;
            message.text = object.getString("text");
            message.inbox = object.getInt("inbox") == 1;
            String date = object.getString("date_created");
            message.dateCreated = TextFormatUtil.toDate(date);
            return message;
        }
    }
}
