package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.VideoDataSource;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;

public class VideoGridFragment extends Fragment {

    private static final String ARG_LIKED = "liked";
    private static final String ARG_USER = "user";

    private boolean mLiked;
    private VideoGridFragmentViewModel mModel;
    private int mUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mLiked = arguments.getBoolean(ARG_LIKED);
            mUser = arguments.getInt(ARG_USER);
        }

        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        VideoGridFragmentViewModel.Factory factory =
                new VideoGridFragmentViewModel.Factory(
                        getString(R.string.server_url), token, mUser, mLiked);
        mModel = new ViewModelProvider(this, factory).get(VideoGridFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView videos = view.findViewById(R.id.videos);
        VideoGridAdapter adapter = new VideoGridAdapter();
        videos.setAdapter(new SlideInBottomAnimationAdapter(adapter));
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 3);
        videos.setLayoutManager(glm);
        mModel.videos.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            VideoDataSource source = mModel.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            List<?> list = mModel.videos.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static VideoGridFragment newInstance(int user, boolean liked) {
        VideoGridFragment fragment = new VideoGridFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_USER, user);
        arguments.putBoolean(ARG_LIKED, liked);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showVideoPlayer(VideoDataSource.Video video) {
        ((MainActivity)requireActivity()).showVideoPlayer(video);
    }

    private class VideoGridAdapter extends PagedListAdapter<VideoDataSource.Video, VideoGridAdapter.VideoGridViewHolder> {

        protected VideoGridAdapter() {
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
        public VideoGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_video, parent, false);
            return new VideoGridViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull VideoGridViewHolder holder, int position) {
            VideoDataSource.Video video = getItem(position);
            //noinspection ConstantConditions
            holder.likes.setText(video.likesCount + "");
            holder.preview.setImageURI(video.screenshot);
            holder.itemView.setOnClickListener(v -> showVideoPlayer(video));
        }

        private class VideoGridViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView preview;
            public TextView likes;

            public VideoGridViewHolder(@NonNull View root) {
                super(root);
                preview = root.findViewById(R.id.preview);
                likes = root.findViewById(R.id.likes);
            }
        }
    }

    public static class VideoGridFragmentViewModel extends ViewModel {

        public VideoGridFragmentViewModel(String url, String token, @Nullable Integer user, @Nullable Boolean liked) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new VideoDataSource.Factory(url, token, null, null, user, liked);
            state = Transformations.switchMap(factory.source, input -> input.state);
            videos = new LivePagedListBuilder<>(factory, config).build();
        }

        public final VideoDataSource.Factory factory;
        public final LiveData<PagedList<VideoDataSource.Video>> videos;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final Boolean mLiked;
            private final String mServerToken;
            private final String mServerUrl;
            private final Integer mUser;

            public Factory(String url, String token, @Nullable Integer user, @Nullable Boolean liked) {
                mServerUrl = url;
                mServerToken = token;
                mUser = user;
                mLiked = liked;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new VideoGridFragmentViewModel(mServerUrl, mServerToken, mUser, mLiked);
            }
        }
    }
}
