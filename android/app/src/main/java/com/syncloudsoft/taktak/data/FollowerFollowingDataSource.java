package com.syncloudsoft.taktak.data;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.utils.JsonUtil;

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

@SuppressLint("LongLogTag")
public class FollowerFollowingDataSource extends PageKeyedDataSource<Integer, FollowerFollowingDataSource.Follower> {

    private static final String TAG = "FollowerFollowingDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mServerToken;
    private final String mServerUrl;
    private final int mUser;
    private final boolean mFollowing;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public FollowerFollowingDataSource(String url, String token, int user, boolean following) {
        mServerToken = token;
        mServerUrl = url;
        mUser = user;
        mFollowing = following;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Follower> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching followers has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Follower> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Follower> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching followers has failed.", e);
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
        String path = mFollowing ? "followings" : "followers";
        Request.Builder builder = new Request.Builder()
                .url(mServerUrl + "api/users/" + mUser + '/' + path + "?page=" + page + "&count=" + count)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder.header("Authorization", "Bearer " + mServerToken);
        }

        return builder.build();
    }

    private List<Follower> transformData(JSONArray data) throws JSONException {
        List<Follower> followers = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Follower follower = Follower.transform(object);
            followers.add(follower);
        }

        return followers;
    }

    public static class Follower {

        public int id;
        public String name;
        public String username;
        public String photo;
        public boolean verified;

        public static Follower transform(JSONObject object) throws JSONException {
            Follower follower = new Follower();
            follower.id = object.getInt("id");
            follower.name = object.getString("name");
            follower.username = object.getString("username");
            follower.photo = JsonUtil.optString(object, "photo");
            follower.verified = object.getInt("verified") == 1;
            return follower;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, Follower> {

        private final int mUser;
        private final String mServerToken;
        private final String mServerUrl;
        private final boolean mFollowing;

        public MutableLiveData<FollowerFollowingDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token, int user, boolean following) {
            mUser = user;
            mServerToken = token;
            mServerUrl = url;
            mFollowing = following;
        }

        @NonNull
        @Override
        public DataSource<Integer, Follower> create() {
            FollowerFollowingDataSource source =
                    new FollowerFollowingDataSource(mServerUrl, mServerToken, mUser, mFollowing);
            this.source.postValue(source);
            return source;
        }
    }
}
