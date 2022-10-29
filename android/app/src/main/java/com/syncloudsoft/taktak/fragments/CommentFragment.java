package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.stfalcon.chatkit.messages.MessageInput;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.CommentDataSource;

import java.io.IOException;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommentFragment extends Fragment {

    public static final String ARG_VIDEO = "video";
    public static final String TAG = "CommentFragment";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private CommentFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private int mVideo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideo = requireArguments().getInt(ARG_VIDEO);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        CommentFragmentViewModel.Factory factory =
                new CommentFragmentViewModel.Factory(
                        getString(R.string.server_url), token, mVideo);
        mModel1 = new ViewModelProvider(this, factory).get(CommentFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.close)
                .setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack());
        RecyclerView comments = view.findViewById(R.id.comments);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true);
        lm.setStackFromEnd(true);
        comments.setLayoutManager(lm);
        CommentsAdapter adapter = new CommentsAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int last = lm.findLastCompletelyVisibleItemPosition();
                if (last == -1 || positionStart >= adapter.getItemCount() - 1 && last == positionStart - 1) {
                    comments.scrollToPosition(positionStart);
                }
            }
        });
        comments.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                comments, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel1.comments.observe(getViewLifecycleOwner(), adapter::submitList);
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.comments.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        MessageInput input = view.findViewById(R.id.input);
        input.setInputListener(message -> {
            if (mModel2.isLoggedIn) {
                submitComment(message);
                return true;
            }

            Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
            return false;
        });
    }

    public static CommentFragment newInstance(int video) {
        CommentFragment fragment = new CommentFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_VIDEO, video);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void submitComment(CharSequence text) {
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        FormBody body = new FormBody.Builder()
                .add("text", text.toString())
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/videos/" + mVideo + "/comments")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to submit comment on video.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Submitting comment on video returned " + code + '.');
                if (code == 200) {
                    CommentDataSource source = mModel1.factory.source.getValue();
                    if (source != null) {
                        source.invalidate();
                    }
                }
            }
        });
    }

    private class CommentsAdapter extends PagedListAdapter<CommentDataSource.Comment, CommentsAdapter.CommentViewHolder> {

        public CommentsAdapter() {
            super(new DiffUtil.ItemCallback<CommentDataSource.Comment>() {

                @Override
                public boolean areItemsTheSame(@NonNull CommentDataSource.Comment a, @NonNull CommentDataSource.Comment b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull CommentDataSource.Comment a, @NonNull CommentDataSource.Comment b) {
                    return TextUtils.equals(a.text, b.text);
                }
            });
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(root);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            CommentDataSource.Comment comment = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(comment.userPhoto)) {
                holder.photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                holder.photo.setImageURI(comment.userPhoto);
            }

            holder.username.setText('@' + comment.userUsername);
            holder.verified.setVisibility(comment.userVerified ? View.VISIBLE : View.GONE);
            holder.text.setText(comment.text);
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), comment.dateCreated.getTime(), true));
            holder.photo.setOnClickListener(v -> showProfile(comment.userId));
            holder.username.setOnClickListener(v -> showProfile(comment.userId));
        }

        private class CommentViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView photo;
            public TextView username;
            public ImageView verified;
            public TextView text;
            public TextView when;

            public CommentViewHolder(@NonNull View root) {
                super(root);
                username = root.findViewById(R.id.username);
                verified = root.findViewById(R.id.verified);
                photo = root.findViewById(R.id.photo);
                text = root.findViewById(R.id.text);
                when = root.findViewById(R.id.when);
            }
        }
    }

    public static class CommentFragmentViewModel extends ViewModel {

        public CommentFragmentViewModel(String url, String token, int video) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new CommentDataSource.Factory(url, token, video);
            state = Transformations.switchMap(factory.source, input -> input.state);
            comments = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<CommentDataSource.Comment>> comments;
        public final CommentDataSource.Factory factory;
        public final LiveData<LoadingState> state;

        private static class Factory implements ViewModelProvider.Factory {

            private final String mServerToken;
            private final String mServerUrl;
            private final int mVideo;

            public Factory(String url, String token, int video) {
                mServerToken = token;
                mServerUrl = url;
                mVideo = video;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new CommentFragmentViewModel(mServerUrl, mServerToken, mVideo);
            }
        }
    }
}
