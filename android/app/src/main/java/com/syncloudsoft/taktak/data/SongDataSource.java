package com.syncloudsoft.taktak.data;

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

public class SongDataSource extends PageKeyedDataSource<Integer, SongDataSource.Song> {

    private static final String TAG = "SongDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final int mSection;
    private final String mServerToken;
    private final String mServerUrl;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public SongDataSource(String url, String token, int section) {
        mSection = section;
        mServerToken = token;
        mServerUrl = url;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Song> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching songs has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Song> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Song> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching songs has failed.", e);
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
                .url(mServerUrl + "api/songs?section=" + mSection + "&page=" + page + "&count=" + count)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder.header("Authorization", "Bearer " + mServerToken);
        }

        return builder.build();
    }

    private List<Song> transformData(JSONArray data) throws JSONException {
        List<Song> songs = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Song song = Song.transform(object);
            songs.add(song);
        }

        return songs;
    }

    public static class Song implements Parcelable {

        public int id;
        public String name;
        public String icon;
        public String audio;

        public Song() { }

        protected Song(Parcel in) {
            id = in.readInt();
            name = in.readString();
            icon = in.readString();
            audio = in.readString();
        }

        public static final Creator<Song> CREATOR = new Creator<Song>() {

            @Override
            public Song createFromParcel(Parcel in) {
                return new Song(in);
            }

            @Override
            public Song[] newArray(int size) {
                return new Song[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        public static Song transform(JSONObject object) throws JSONException {
            Song song = new Song();
            song.id = object.getInt("id");
            song.name = object.getString("name");
            song.icon = JsonUtil.optString(object, "icon");
            song.audio = object.getString("audio");
            return song;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(id);
            parcel.writeString(name);
            parcel.writeString(icon);
            parcel.writeString(audio);
        }
    }

    public static class Factory extends DataSource.Factory<Integer, Song> {

        private final int mSection;
        private final String mServerToken;
        private final String mServerUrl;

        public MutableLiveData<SongDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token, int section) {
            mSection = section;
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, Song> create() {
            SongDataSource source = new SongDataSource(mServerUrl, mServerToken, mSection);
            this.source.postValue(source);
            return source;
        }
    }
}
