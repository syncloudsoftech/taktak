package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.VideoDataSource;
import com.syncloudsoft.taktak.data.VideoSectionDataSource;
import com.syncloudsoft.taktak.utils.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DiscoverFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";

    private Call mCall;
    private final OkHttpClient mHttpClient = new OkHttpClient();
    private DiscoverFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        DiscoverFragmentViewModel.Factory factory =
                new DiscoverFragmentViewModel.Factory(getString(R.string.server_url), token);
        mModel = new ViewModelProvider(this, factory).get(DiscoverFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView sections = view.findViewById(R.id.sections);
        VerticalAdapter adapter = new VerticalAdapter();
        sections.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            VideoSectionDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        mModel.sections.observe(getViewLifecycleOwner(), adapter::submitList);
        View loading = view.findViewById(R.id.loading);
        mModel.state1.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        FloatingSearchView search = view.findViewById(R.id.search);
        search.setOnBindSuggestionCallback((v, icon, text, item, position) -> {
            Suggestion suggestion = (Suggestion)item;
            if (TextUtils.isEmpty(suggestion.photo)) {
                icon.setImageResource(R.drawable.photo_placeholder);
            } else {
                Glide.with(requireContext()).load(suggestion.photo).into(icon);
            }

            text.setText('@' + suggestion.username);
        });
        search.setOnQueryChangeListener((previous, now) -> {
            if (!TextUtils.isEmpty(previous) && TextUtils.isEmpty(now)) {
                search.clearSuggestions();
            } else {
                findSuggestions(now);
            }
        });
        search.setOnSearchListener(new FloatingSearchView.OnSearchListener() {

            @Override
            public void onSuggestionClicked(SearchSuggestion ss) {
                search.clearQuery();
                Suggestion suggestion = (Suggestion)ss;
                showProfile(suggestion.id);
            }

            @Override
            public void onSearchAction(String q) { }
        });
        mModel.state2.observe(getViewLifecycleOwner(), state -> {
            if (state == LoadingState.LOADING) {
                search.showProgress();
            } else {
                search.hideProgress();
            }
        });
        mModel.suggestions.observe(getViewLifecycleOwner(), search::swapSuggestions);
        if (getResources().getBoolean(R.bool.admob_discover_ad_enabled)) {
            AdView ad = new AdView(requireContext());
            ad.setAdSize(AdSize.BANNER);
            ad.setAdUnitId(getString(R.string.admob_discover_ad_id));
            ad.loadAd(new AdRequest.Builder().build());
            LinearLayout banner = view.findViewById(R.id.banner);
            banner.addView(ad);
        }
    }

    private void findSuggestions(String q) {
        if (mCall != null) {
            mCall.cancel();
        }

        mModel.state2.postValue(LoadingState.LOADING);
        Uri uri = Uri.parse(getString(R.string.server_url) + "api/users")
                .buildUpon()
                .appendQueryParameter("q", q)
                .build();
        Request.Builder request = new Request.Builder().get().url(uri.toString());

        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        if (!TextUtils.isEmpty(token)) {
            request.header("Authorization", "Bearer " + token);
        }

        mCall = mHttpClient.newCall(request.build());
        mCall.enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to load user suggestions from server.", e);
                mModel.state2.postValue(LoadingState.ERROR);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Loading suggestions for search returned " + code + '.');
                if (code == 200) {
                    try {
                        //noinspection ConstantConditions
                        String content = response.body().string();
                        JSONObject json = new JSONObject(content);
                        JSONArray data = json.getJSONArray("data");
                        List<Suggestion> suggestions = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject object = data.getJSONObject(i);
                            Suggestion suggestion = Suggestion.transform(object);
                            suggestions.add(suggestion);
                        }

                        Log.v(TAG, "Found " + suggestions.size() + " matching q=" + q);
                        mModel.suggestions.postValue(suggestions);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse response from server.", e);
                    }

                    mModel.state2.postValue(LoadingState.LOADED);
                } else {
                    mModel.state2.postValue(LoadingState.ERROR);
                }
            }
        });
    }

    public static DiscoverFragment newInstance() {
        return new DiscoverFragment();
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void showVideoPlayer(VideoDataSource.Video video) {
        ((MainActivity)requireActivity()).showVideoPlayer(video);
    }

    private class HorizontalAdapter extends PagedListAdapter<VideoDataSource.Video, HorizontalAdapter.HorizontalViewHolder> {

        protected HorizontalAdapter() {
            super(new DiffUtil.ItemCallback<VideoDataSource.Video>() {

                @Override
                public boolean areItemsTheSame(@NonNull VideoDataSource.Video a, @NonNull VideoDataSource.Video b) {
                    return areContentsTheSame(a, b);
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoDataSource.Video a, @NonNull VideoDataSource.Video b) {
                    return a.id == b.id;
                }
            });
        }

        @NonNull
        @Override
        public HorizontalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_discover_video, parent, false);
            return new HorizontalViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull HorizontalViewHolder holder, int position) {
            VideoDataSource.Video video = getItem(position);
            //noinspection unchecked
            Glide.with(requireContext())
                    .asGif()
                    .load(video.preview)
                    .thumbnail(new RequestBuilder[]{
                            Glide.with(requireContext()).load(video.screenshot).centerCrop()
                    })
                    .apply(RequestOptions.placeholderOf(R.drawable.image_placeholder).centerCrop())
                    .into(holder.preview);
            holder.itemView.setOnClickListener(v -> showVideoPlayer(video));
        }

        private class HorizontalViewHolder extends RecyclerView.ViewHolder {

            public ImageView preview;

            public HorizontalViewHolder(@NonNull View root) {
                super(root);
                preview = root.findViewById(R.id.preview);
            }
        }
    }

    private class VerticalAdapter extends PagedListAdapter<VideoSectionDataSource.VideoSection, VerticalAdapter.VerticalViewHolder> {

        protected VerticalAdapter() {
            super(new DiffUtil.ItemCallback<VideoSectionDataSource.VideoSection>() {

                @Override
                public boolean areItemsTheSame(@NonNull VideoSectionDataSource.VideoSection a, @NonNull VideoSectionDataSource.VideoSection b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoSectionDataSource.VideoSection a, @NonNull VideoSectionDataSource.VideoSection b) {
                    return TextUtils.equals(a.name, b.name);
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull VerticalViewHolder holder, int position) {
            VideoSectionDataSource.VideoSection section = getItem(position);
            //noinspection ConstantConditions
            holder.title.setText(section.name);
            holder.load(section.id);
        }

        @NonNull
        @Override
        public VerticalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_discover_section, parent, false);
            return new VerticalViewHolder(root);
        }

        private class VerticalViewHolder extends RecyclerView.ViewHolder {

            public TextView title;
            public ProgressBar loading;
            public RecyclerView videos;

            public LiveData<PagedList<VideoDataSource.Video>> items;
            public LiveData<LoadingState> state;

            public VerticalViewHolder(@NonNull View root) {
                super(root);
                title = root.findViewById(R.id.title);
                videos = root.findViewById(R.id.videos);
                loading = root.findViewById(R.id.loading);
                LinearLayoutManager llm =
                        new LinearLayoutManager(
                                requireContext(), LinearLayoutManager.HORIZONTAL, false);
                videos.setLayoutManager(llm);
                OverScrollDecoratorHelper.setUpOverScroll(
                        videos, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL);
            }

            public void load(int section) {
                HorizontalAdapter adapter = new HorizontalAdapter();
                videos.setAdapter(new SlideInBottomAnimationAdapter(adapter));
                PagedList.Config config = new PagedList.Config.Builder()
                        .setPageSize(10)
                        .build();
                String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(SharedConstants.PREF_SERVER_TOKEN, null);
                VideoDataSource.Factory factory =
                        new VideoDataSource.Factory(
                                getString(R.string.server_url), token, null, section, null, false);
                state = Transformations.switchMap(factory.source, input -> input.state);
                state.observe(getViewLifecycleOwner(), state ->
                        loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));
                items = new LivePagedListBuilder<>(factory, config).build();
                items.observe(getViewLifecycleOwner(), adapter::submitList);
            }
        }
    }

    public static class DiscoverFragmentViewModel extends ViewModel {

        public DiscoverFragmentViewModel(String url, String token) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new VideoSectionDataSource.Factory(url, token);
            state1 = Transformations.switchMap(factory.source, input -> input.state);
            sections = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<VideoSectionDataSource.VideoSection>> sections;
        public final VideoSectionDataSource.Factory factory;
        public final LiveData<LoadingState> state1;
        public final MutableLiveData<LoadingState> state2 = new MutableLiveData<>(LoadingState.IDLE);
        public final MutableLiveData<List<Suggestion>> suggestions = new MutableLiveData<>();

        private static class Factory implements ViewModelProvider.Factory {

            private final String mServerToken;
            private final String mServerUrl;

            public Factory(String url, String token) {
                mServerToken = token;
                mServerUrl = url;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new DiscoverFragmentViewModel(mServerUrl, mServerToken);
            }
        }
    }

    private static class Suggestion implements Parcelable, SearchSuggestion {

        public int id;
        public String name;
        public String username;
        public String photo;
        public boolean verified;

        public Suggestion() { }

        protected Suggestion(Parcel in) {
            id = in.readInt();
            name = in.readString();
            username = in.readString();
            photo = in.readString();
            verified = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeString(name);
            dest.writeString(username);
            dest.writeString(photo);
            dest.writeByte((byte) (verified ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Suggestion> CREATOR = new Creator<Suggestion>() {
            @Override
            public Suggestion createFromParcel(Parcel in) {
                return new Suggestion(in);
            }

            @Override
            public Suggestion[] newArray(int size) {
                return new Suggestion[size];
            }
        };

        public static Suggestion transform(JSONObject object) throws JSONException {
            Suggestion suggestion = new Suggestion();
            suggestion.id = object.getInt("id");
            suggestion.name = object.getString("name");
            suggestion.username = object.getString("username");
            suggestion.photo = JsonUtil.optString(object, "photo");
            suggestion.verified = object.getInt("verified") == 1;
            return suggestion;
        }

        @Override
        public String getBody() {
            return '@' + username;
        }
    }
}
