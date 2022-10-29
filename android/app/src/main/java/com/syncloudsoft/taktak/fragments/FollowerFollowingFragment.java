package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.FollowerFollowingDataSource;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class FollowerFollowingFragment extends Fragment {

    public static final String ARG_USER = "user";
    public static final String ARG_FOLLOWING = "following";

    private boolean mFollowing;
    private FollowerFollowingFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int user = requireArguments().getInt(ARG_USER);
        mFollowing = requireArguments().getBoolean(ARG_FOLLOWING);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        FollowerFollowingFragmentViewModel.Factory factory =
                new FollowerFollowingFragmentViewModel.Factory(
                        getString(R.string.server_url), token, user, mFollowing);
        mModel = new ViewModelProvider(this, factory)
                .get(FollowerFollowingFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_follower_following, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView title = view.findViewById(R.id.title);
        title.setText(mFollowing ? R.string.following_label : R.string.followers_label);
        view.findViewById(R.id.close)
                .setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack());
        FollowerAdapter adapter = new FollowerAdapter();
        RecyclerView followers = view.findViewById(R.id.followers);
        followers.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        followers.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                followers, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel.followers.observe(getViewLifecycleOwner(), adapter::submitList);
        TextView empty = view.findViewById(R.id.empty);
        empty.setText(mFollowing ? R.string.empty_followings : R.string.empty_followers);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel.followers.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private class FollowerAdapter extends PagedListAdapter<FollowerFollowingDataSource.Follower, FollowerAdapter.FollowerViewHolder> {

        protected FollowerAdapter() {
            super(new DiffUtil.ItemCallback<FollowerFollowingDataSource.Follower>() {

                @Override
                public boolean areItemsTheSame(@NonNull FollowerFollowingDataSource.Follower a, @NonNull FollowerFollowingDataSource.Follower b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull FollowerFollowingDataSource.Follower a, @NonNull FollowerFollowingDataSource.Follower b) {
                    return TextUtils.equals(a.name, b.name);
                }
            });
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull FollowerViewHolder holder, int position) {
            final FollowerFollowingDataSource.Follower follower = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(follower.photo)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(follower.photo);
            }

            holder.name.setText(follower.name);
            holder.username.setText('@' + follower.username);
            holder.verified.setVisibility(follower.verified ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> showProfile(follower.id));
        }

        @NonNull
        @Override
        public FollowerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_user, parent, false);
            return new FollowerViewHolder(root);
        }

        private class FollowerViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView photo;
            public TextView name;
            public TextView username;
            public ImageView verified;

            public FollowerViewHolder(@NonNull View root) {
                super(root);
                photo = root.findViewById(R.id.photo);
                name = root.findViewById(R.id.name);
                username = root.findViewById(R.id.username);
                verified = root.findViewById(R.id.verified);
            }
        }
    }

    public static FollowerFollowingFragment newInstance(int user, boolean following) {
        FollowerFollowingFragment fragment = new FollowerFollowingFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_USER, user);
        arguments.putBoolean(ARG_FOLLOWING, following);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static class FollowerFollowingFragmentViewModel extends ViewModel {

        public FollowerFollowingFragmentViewModel(String url, String token, int user, boolean following) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            FollowerFollowingDataSource.Factory factory =
                    new FollowerFollowingDataSource.Factory(url, token, user, following);
            state = Transformations.switchMap(factory.source, input -> input.state);
            followers = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<FollowerFollowingDataSource.Follower>> followers;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final String mServerToken;
            private final String mServerUrl;
            private final int mUser;
            private final boolean mFollowing;

            public Factory(String url, String token, int user, boolean following) {
                mServerToken = token;
                mServerUrl = url;
                mUser = user;
                mFollowing = following;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new FollowerFollowingFragmentViewModel(mServerUrl, mServerToken, mUser, mFollowing);
            }
        }
    }
}
