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

import org.apache.commons.lang3.StringUtils;
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

public class ArticleDataSource extends PageKeyedDataSource<Integer, ArticleDataSource.Article> {

    private static final String TAG = "ArticleDataSource";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    @Nullable private final Integer[] mSections;
    private final String mServerToken;
    private final String mServerUrl;

    public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);

    public ArticleDataSource(String url, String token, @Nullable Integer[] sections) {
        mSections = sections;
        mServerToken = token;
        mServerUrl = url;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Article> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(1, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching articles has failed.", e);
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
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Article> callback) { }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Article> callback) {
        state.postValue(LoadingState.LOADING);
        Request request = createRequest(params.key, params.requestedLoadSize);
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Fetching articles has failed.", e);
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
        String sections = "";
        if (mSections != null && mSections.length > 0) {
            String[] ids = new String[mSections.length];
            for (int i = 0; i < mSections.length; i++) {
                ids[i] = String.valueOf(mSections[i]);
            }

            sections = StringUtils.join(ids, ',');
        }

        Request.Builder builder = new Request.Builder()
                .url(mServerUrl + "api/articles?sections=" + sections + "&page=" + page + "&count=" + count)
                .get();
        if (!TextUtils.isEmpty(mServerToken)) {
            builder.header("Authorization", "Bearer " + mServerToken);
        }

        return builder.build();
    }

    private List<Article> transformData(JSONArray data) throws JSONException {
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);
            Article article = Article.transform(object);
            articles.add(article);
        }

        return articles;
    }

    public static class Article {

        public int id;
        public String title;
        public String snippet;
        public String image;
        public String url;
        @Nullable public String publisher;
        public int sectionId;
        public String sectionName;
        public Date dateReported;
        public Date dateCreated;

        public static Article transform(JSONObject object) throws JSONException {
            Article article = new Article();
            article.id = object.getInt("id");
            article.title = object.getString("title");
            article.snippet = object.getString("snippet");
            article.image = object.getString("image");
            article.url = object.getString("url");
            article.publisher = JsonUtil.optString(object, "publisher");
            article.sectionId = object.getInt("section_id");
            article.sectionName = object.getString("section_name");
            String date;
            date = object.getString("date_reported");
            article.dateReported = TextFormatUtil.toDate(date);
            date = object.getString("date_created");
            article.dateCreated = TextFormatUtil.toDate(date);
            return article;
        }
    }

    public static class Factory extends DataSource.Factory<Integer, Article> {

        private final String mServerToken;
        private final String mServerUrl;

        @Nullable public Integer[] sections;

        public MutableLiveData<ArticleDataSource> source = new MutableLiveData<>();

        public Factory(String url, String token) {
            mServerToken = token;
            mServerUrl = url;
        }

        @NonNull
        @Override
        public DataSource<Integer, Article> create() {
            ArticleDataSource source = new ArticleDataSource(mServerUrl, mServerToken, sections);
            this.source.postValue(source);
            return source;
        }
    }
}
