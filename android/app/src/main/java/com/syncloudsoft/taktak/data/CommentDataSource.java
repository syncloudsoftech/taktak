package com.syncloudsoft.taktak.data;

import android.text.TextUtils;
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

public class CommentDataSource extends PageKeyedDataSource<Integer, CommentDataSource.Comment> {

    private static final String TAG = "CommentDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;
    private final int mVideo;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public CommentDataSource(String url, String token, int video) {
        mServerToken = token;
        mServerUrl = url;
        mVideo = video;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Comment> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching comments has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Comment> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Comment> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching comments has failed.", e);
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
                .url(mServerUrl + "api/videos/" + mVideo + "/comments?page=" + page + "&count=" + count)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder.header("Authorization", "Bearer " + mServerToken);
        }

        return builder.build();
    }

    private List<Comment> transformData(JSONArray data) throws JSONException {
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Comment comment = Comment.transform(object);
            comments.add(comment);
        }

        return comments;
    }

    public static class Comment {

        public int id;
        public String text;
        public int userId;
        public String userUsername;
        public String userPhoto;
        public boolean userVerified;
        public Date dateCreated;

        public static Comment transform(JSONObject object) throws JSONException {
            Comment comment = new Comment();
            comment.id = object.getInt("id");
            comment.text = object.getString("text");
            comment.userId = object.getInt("user_id");
            comment.userUsername = object.getString("user_username");
            comment.userPhoto = JsonUtil.optString(object, "user_photo");
            comment.userVerified = object.getInt("user_verified") == 1;
            String date = object.getString("date_created");
            comment.dateCreated = TextFormatUtil.toDate(date);
            return comment;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, Comment> {

        private final int mVideo;
        private final String mServerToken;
        private final String mServerUrl;

        public MutableLiveData<CommentDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token, int video) {
            mVideo = video;
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, Comment> create() {
            CommentDataSource source = new CommentDataSource(mServerUrl, mServerToken, mVideo);
            this.source.postValue(source);
            return source;
        }
    }
}
