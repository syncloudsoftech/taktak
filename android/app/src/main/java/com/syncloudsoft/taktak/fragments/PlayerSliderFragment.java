package com.syncloudsoft.taktak.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.AsyncPagedListDiffer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.VideoDataSource;

import java.util.concurrent.atomic.AtomicReference;

public class PlayerSliderFragment extends Fragment {

    private static final String ARG_LIKED = "liked";
    private static final String ARG_QUERY = "query";
    private static final String ARG_SECTION = "section";
    private static final String ARG_USER = "user";

    private static final String TAG = "PlayerSliderFragment";

    private Boolean mLiked;
    private PlayerSliderFragmentViewModel mModel;
    private String mQuery;
    private Integer mSection;
    private Integer mUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mLiked = arguments.getBoolean(ARG_LIKED);
            mQuery = arguments.getString(ARG_QUERY);
            mSection = arguments.getInt(ARG_SECTION);
            mUser = arguments.getInt(ARG_USER);
        }

        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        PlayerSliderFragmentViewModel.Factory factory =
                new PlayerSliderFragmentViewModel.Factory(
                        getString(R.string.server_url), token, mQuery, mSection, mUser, mLiked);
        mModel = new ViewModelProvider(this, factory).get(PlayerSliderFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player_slider, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PlayerSliderAdapter adapter = new PlayerSliderAdapter(this);
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        mModel.videos.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            VideoDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        final View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        if (getResources().getBoolean(R.bool.admob_player_ad_enabled)) {
            AtomicReference<InterstitialAd> ref = new AtomicReference<>();
            InterstitialAd.load(
                    requireContext(),
                    getString(R.string.admob_player_ad_id),
                    new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            Log.v(TAG, "Interstitial ad from AdMob was loaded.");
                            ref.set(interstitialAd);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            Log.e(TAG, "Interstitial ad from AdMob failed to load.\n" + error.toString());
                        }
                    });
            int interval = getResources().getInteger(R.integer.admob_player_ad_interval);
            pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

                @Override
                public void onPageSelected(int position) {
                    mModel.viewed++;
                    InterstitialAd ad = ref.get();
                    if (mModel.viewed >= interval && ad != null) {
                        ad.show(requireActivity());
                        mModel.viewed = 0;
                    }
                }
            });
        }
    }

    public static PlayerSliderFragment newInstance(@Nullable String query, int section, int user, boolean liked) {
        PlayerSliderFragment fragment = new PlayerSliderFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_QUERY, query);
        arguments.putInt(ARG_SECTION, section);
        arguments.putInt(ARG_USER, user);
        arguments.putBoolean(ARG_LIKED, liked);
        fragment.setArguments(arguments);
        return fragment;
    }

    private static class PlayerSliderAdapter extends FragmentStateAdapter {

        private AsyncPagedListDiffer<VideoDataSource.Video> mDiffer;

        public PlayerSliderAdapter(@NonNull Fragment fragment) {
            super(fragment);
            mDiffer = new AsyncPagedListDiffer<>(
                    new AdapterListUpdateCallback(this),
                    new AsyncDifferConfig.Builder<>(new DiffUtil.ItemCallback<VideoDataSource.Video>() {

                        @Override
                        public boolean areItemsTheSame(@NonNull VideoDataSource.Video a, @NonNull VideoDataSource.Video b) {
                            return areContentsTheSame(a, b);
                        }

                        @Override
                        public boolean areContentsTheSame(@NonNull VideoDataSource.Video a, @NonNull VideoDataSource.Video b) {
                            return a.id == b.id;
                        }
                    }).build()
            );
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            VideoDataSource.Video video = mDiffer.getItem(position);
            return PlayerFragment.newInstance(video);
        }

        @Override
        public int getItemCount() {
            return mDiffer.getItemCount();
        }

        public void submitList(PagedList<VideoDataSource.Video> list) {
            mDiffer.submitList(list);
        }
    }

    public static class PlayerSliderFragmentViewModel extends ViewModel {

        public PlayerSliderFragmentViewModel(String url, String token, @Nullable String query, @Nullable Integer section, @Nullable Integer user, @Nullable Boolean liked) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new VideoDataSource.Factory(url, token, query, section, user, liked);
            state = Transformations.switchMap(factory.source, input -> input.state);
            videos = new LivePagedListBuilder<>(factory, config).build();
        }

        public final VideoDataSource.Factory factory;
        public final LiveData<PagedList<VideoDataSource.Video>> videos;
        public int viewed = 0;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final Boolean mLiked;
            private final String mQuery;
            private final Integer mSection;
            private final String mServerToken;
            private final String mServerUrl;
            private final Integer mUser;

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
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new PlayerSliderFragmentViewModel(mServerUrl, mServerToken, mQuery, mSection, mUser, mLiked);
            }
        }
    }
}
