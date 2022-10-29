package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.ThreadDataSource;
import com.syncloudsoft.taktak.events.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class ThreadFragment extends Fragment {

    private ThreadFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        ThreadFragmentViewModel.Factory factory =
                new ThreadFragmentViewModel.Factory(getString(R.string.server_url), token);
        mModel1 = new ViewModelProvider(this, factory).get(ThreadFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_thread, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        ThreadDataSource source = mModel1.factory.source.getValue();
        if (source != null) {
            source.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mModel2.areThreadsInvalid) {
            mModel2.areThreadsInvalid = false;
            ThreadDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ThreadAdapter adapter = new ThreadAdapter();
        RecyclerView threads = view.findViewById(R.id.threads);
        threads.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                threads, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            ThreadDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        });
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.threads.observe(getViewLifecycleOwner(), adapter::submitList);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            if (state != LoadingState.LOADING) {
                swipe.setRefreshing(false);
            }

            List<?> list = mModel1.threads.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static ThreadFragment newInstance() {
        return new ThreadFragment();
    }

    private void showChatForThread(ThreadDataSource.Thread thread) {
        ((MainActivity)requireActivity()).showChatForThread(thread);
    }

    private class ThreadAdapter extends PagedListAdapter<ThreadDataSource.Thread, ThreadAdapter.ThreadViewHolder> {

        protected ThreadAdapter() {
            super(new DiffUtil.ItemCallback<ThreadDataSource.Thread>() {

                @Override
                public boolean areItemsTheSame(@NonNull ThreadDataSource.Thread a, @NonNull ThreadDataSource.Thread b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ThreadDataSource.Thread a, @NonNull ThreadDataSource.Thread b) {
                    return TextUtils.equals(a.userUsername, b.userUsername);
                }
            });
        }

        @NonNull
        @Override
        public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_thread, parent, false);
            return new ThreadViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
            ThreadDataSource.Thread thread = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(thread.userPhoto)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(thread.userPhoto);
            }

            holder.username.setText('@' + thread.userUsername);
            if (thread.lastMessageText == null || thread.lastMessageDateCreated == null) {
                holder.text.setVisibility(View.GONE);
                holder.when.setVisibility(View.GONE);
            } else {
                holder.text.setText(thread.lastMessageText);
                holder.text.setVisibility(View.VISIBLE);
                holder.when.setText(
                        DateUtils.getRelativeTimeSpanString(
                                requireContext(), thread.lastMessageDateCreated.getTime(), true));
                holder.when.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(v -> showChatForThread(thread));
        }

        public class ThreadViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView photo;
            public TextView username;
            public TextView text;
            public TextView when;

            public ThreadViewHolder(@NonNull View root) {
                super(root);
                photo = root.findViewById(R.id.photo);
                username = root.findViewById(R.id.username);
                text = root.findViewById(R.id.text);
                when = root.findViewById(R.id.when);
            }
        }
    }

    public static class ThreadFragmentViewModel extends ViewModel {

        public ThreadFragmentViewModel(String url, String token) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new ThreadDataSource.Factory(url, token);
            state = Transformations.switchMap(factory.source, input -> input.state);
            threads = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<ThreadDataSource.Thread>> threads;
        public final ThreadDataSource.Factory factory;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final String mServerUrl;
            private final String mServerToken;

            public Factory(String url, String token) {
                mServerUrl = url;
                mServerToken = token;
            }

            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T)new ThreadFragment.ThreadFragmentViewModel(mServerUrl, mServerToken);
            }
        }
    }
}
