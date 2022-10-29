package com.syncloudsoft.taktak.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
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

public class VideoDataSource extends PageKeyedDataSource<Integer, VideoDataSource.Video> {

    private static final String TAG = "VideoDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final String mQuery;
    private final Integer mSection;
    private final String mServerToken;
    private final String mServerUrl;
    private final Integer mUser;
    private final Boolean mLiked;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public VideoDataSource(String url, String token, @Nullable String query, @Nullable Integer section, @Nullable Integer user, @Nullable Boolean liked) {
        mServerUrl = url;
        mServerToken = token;
        mQuery = query;
        mSection = section;
        mUser = user;
        mLiked = liked;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Video> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching videos has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Video> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Video> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching videos has failed.", e);
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
                        callback.onResult(transformData(data),params.key + 1);
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
        Uri.Builder builder1 = Uri.parse(mServerUrl + "api/videos").buildUpon();
        builder1.appendQueryParameter("page", page + "");
        builder1.appendQueryParameter("count", count + "");
        if (!TextUtils.isEmpty(mQuery)) {
            builder1.appendQueryParameter("q", mQuery);
        }

        if (mSection != null && mSection > 0) {
            builder1.appendQueryParameter("section", mSection + "");
        }

        if (mUser != null && mUser > 0) {
            builder1.appendQueryParameter("user", mUser + "");
        }

        if (mLiked != null && mLiked) {
            builder1.appendQueryParameter("liked", "1");
        }

        String url = builder1.build().toString();
        Log.v(TAG, "Fetch URL is " + url);
        Request.Builder builder2 = new Request.Builder()
                .url(url)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder2.header("Authorization", "Bearer " + mServerToken);
        }

        return builder2.build();
    }

    private List<Video> transformData(JSONArray data) throws JSONException {
        List<Video> videos = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Video video = Video.transform(object);
            videos.add(video);
        }

        return videos;
    }

    public static class Factory extends DataSource.Factory<Integer, Video> {

        private final Boolean mLiked;
        private final String mQuery;
        private final Integer mSection;
        private final String mServerToken;
        private final String mServerUrl;
        private final Integer mUser;

        public MutableLiveData<VideoDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token, @Nullable String query, @Nullable Integer section, @Nullable Integer user, @Nullable Boolean liked) {
            mServerUrl = url;
            mServerToken = token;
            mQuery = query;
            mSection = section;
            mUser = user;
            mLiked = liked;
        }

        @NonNull
        @Override
        public DataSource<Integer, Video> create() {
            VideoDataSource source =
                    new VideoDataSource(mServerUrl, mServerToken, mQuery, mSection, mUser, mLiked);
            this.source.postValue(source);
            return source;
        }
    }

    public static class Video implements Parcelable {

        public int id;
        public String description;
        public String video;
        public String preview;
        public String screenshot;
        public boolean private2;
        public boolean comments;
        public boolean isLiked;
        public int likesCount;
        public int commentsCount;
        public Integer songId;
        public String songName;
        public int userId;
        public String userUsername;
        public String userPhoto;
        public boolean userVerified;

        public Video() { }

        protected Video(Parcel in) {
            id = in.readInt();
            description = in.readString();
            video = in.readString();
            preview = in.readString();
            screenshot = in.readString();
            private2 = in.readByte() != 0;
            comments = in.readByte() != 0;
            isLiked = in.readByte() != 0;
            likesCount = in.readInt();
            commentsCount = in.readInt();
            if (in.readByte() == 0) {
                songId = null;
            } else {
                songId = in.readInt();
            }

            songName = in.readString();
            userId = in.readInt();
            userUsername = in.readString();
            userPhoto = in.readString();
            userVerified = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeString(description);
            dest.writeString(video);
            dest.writeString(preview);
            dest.writeString(screenshot);
            dest.writeByte((byte) (private2 ? 1 : 0));
            dest.writeByte((byte) (comments ? 1 : 0));
            dest.writeByte((byte) (isLiked ? 1 : 0));
            dest.writeInt(likesCount);
            dest.writeInt(commentsCount);
            if (songId == null) {
                dest.writeByte((byte) 0);
            } else {
                dest.writeByte((byte) 1);
                dest.writeInt(songId);
            }

            dest.writeString(songName);
            dest.writeInt(userId);
            dest.writeString(userUsername);
            dest.writeString(userPhoto);
            dest.writeByte((byte) (userVerified ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static Video transform(JSONObject object) throws JSONException {
            Video video = new Video();
            video.id = object.getInt("id");
            video.description = JsonUtil.optString(object, "description");
            video.video = object.getString("video");
            video.preview = object.getString("preview");
            video.screenshot = object.getString("screenshot");
            video.private2 = object.getInt("private") == 1;
            video.comments = object.getInt("comments") == 1;
            video.isLiked = object.getInt("is_liked") == 1;
            video.likesCount = object.getInt("likes_count");
            video.commentsCount = object.getInt("comments_count");
            video.songId = object.optInt("song_id", -1);
            if (video.songId <= 0) {
                video.songId = null;
            }

            video.songName = object.optString("song_name");
            video.userId = object.getInt("user_id");
            video.userUsername = object.getString("user_username");
            video.userPhoto = object.optString("user_photo");
            video.userVerified = object.getInt("user_verified") == 1;
            return video;
        }

        public static final Creator<Video> CREATOR = new Creator<Video>() {
            @Override
            public Video createFromParcel(Parcel in) {
                return new Video(in);
            }

            @Override
            public Video[] newArray(int size) {
                return new Video[size];
            }
        };
    }
}
