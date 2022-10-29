package com.syncloudsoft.taktak.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.drawee.view.SimpleDraweeView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.NotificationDataSource;
import com.syncloudsoft.taktak.data.VideoDataSource;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private NotificationFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        NotificationFragmentViewModel.Factory factory = 
                new NotificationFragmentViewModel.Factory(getString(R.string.server_url), token);
        mModel = new ViewModelProvider(this, factory).get(NotificationFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NotificationAdapter adapter = new NotificationAdapter();
        RecyclerView notifications = view.findViewById(R.id.notifications);
        notifications.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        notifications.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        mModel.notifications.observe(getViewLifecycleOwner(), adapter::submitList);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe);
        swipe.setOnRefreshListener(() -> {
            NotificationDataSource source = mModel.factory.source.getValue();
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

            List<?> list = mModel.notifications.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    private void fetchAndPlay(int video) {
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request request = new Request.Builder()
                .get()
                .url(getString(R.string.server_url) + "api/videos/" + video)
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to get video information from server.", e);
                Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Fetching video information returned " + code + '.');
                if (code == 200 && isAdded()) {
                    try {
                        //noinspection ConstantConditions
                        String content = response.body().string();
                        VideoDataSource.Video item =
                                VideoDataSource.Video.transform(new JSONObject(content));
                        showVideoPlayer(item);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse response from server.", e);
                    }
                }
            }
        });
    }

    public static NotificationFragment newInstance() {
        return new NotificationFragment();
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void showVideoPlayer(VideoDataSource.Video video) {
        ((MainActivity)requireActivity()).showVideoPlayer(video);
    }
    
    private class NotificationAdapter extends PagedListAdapter<NotificationDataSource.Notification, NotificationAdapter.NotificationViewHolder> {

        protected NotificationAdapter() {
            super(new DiffUtil.ItemCallback<NotificationDataSource.Notification>() {

                @Override
                public boolean areItemsTheSame(@NonNull NotificationDataSource.Notification a, @NonNull NotificationDataSource.Notification b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull NotificationDataSource.Notification a, @NonNull NotificationDataSource.Notification b) {
                    return TextUtils.equals(a.content, b.content);
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationDataSource.Notification notification = getItem(position);
            //noinspection ConstantConditions
            holder.photo.setImageURI(notification.sourcePhoto);
            holder.photo.setOnClickListener(v -> showProfile(notification.sourceId));
            if (TextUtils.equals(notification.content, "followed_you")) {
                holder.content.setText(getString(R.string.notification_followed_you, notification.sourceUsername));
            } else if (TextUtils.equals(notification.content, "commented_on_video")) {
                holder.content.setText(getString(R.string.notification_commented_on_video, notification.sourceUsername));
            } else if (TextUtils.equals(notification.content, "liked_video")) {
                holder.content.setText(getString(R.string.notification_liked_video, notification.sourceUsername));
            } else {
                holder.content.setText(getString(R.string.notification_else));
            }

            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), notification.dateCreated.getTime(), true));
            holder.thumbnail.setVisibility(notification.videoId != null ? View.VISIBLE : View.GONE);
            if (notification.videoId != null) {
                holder.thumbnail.setImageURI(notification.videoScreenshot);
                holder.thumbnail.setOnClickListener(v -> fetchAndPlay(notification.videoId));
            }
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        private class NotificationViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView photo;
            public TextView content;
            public SimpleDraweeView thumbnail;
            public TextView when;

            public NotificationViewHolder(@NonNull View root) {
                super(root);
                photo = root.findViewById(R.id.photo);
                content = root.findViewById(R.id.content);
                thumbnail = root.findViewById(R.id.thumbnail);
                when = root.findViewById(R.id.when);
            }
        }
    }
    
    public static class NotificationFragmentViewModel extends ViewModel {

        public NotificationFragmentViewModel(String url, String token) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new NotificationDataSource.Factory(url, token);
            state = Transformations.switchMap(factory.source, input -> input.state);
            notifications = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<NotificationDataSource.Notification>> notifications;
        public final NotificationDataSource.Factory factory;
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
                return (T)new NotificationFragmentViewModel(mServerUrl, mServerToken);
            }
        }
    }
}
