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
import com.syncloudsoft.taktak.data.MessageDataSource;
import com.syncloudsoft.taktak.events.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

public class MessageFragment extends Fragment {

    private static final String ARG_THREAD = "thread";
    private static final String ARG_USERNAME = "username";
    private static final String TAG = "MessageFragment";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private MessageFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private int mThread;
    private String mUsername;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThread = requireArguments().getInt(ARG_THREAD);
        mUsername = requireArguments().getString(ARG_USERNAME);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        MessageFragmentViewModel.Factory factory =
                new MessageFragmentViewModel.Factory(getString(R.string.server_url), token, mThread);
        mModel1 = new ViewModelProvider(this, factory).get(MessageFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.thread == mThread) {
            MessageDataSource source = mModel1.factory.source.getValue();
            if (source != null) {
                source.invalidate();
            }
        }

        mModel2.areThreadsInvalid = true;
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
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.close)
                .setOnClickListener(v -> requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack());
        RecyclerView messages = view.findViewById(R.id.messages);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true);
        lm.setStackFromEnd(true);
        messages.setLayoutManager(lm);
        MessageAdapter adapter = new MessageAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int last = lm.findLastCompletelyVisibleItemPosition();
                if (last == -1 || positionStart >= adapter.getItemCount() - 1 && last == positionStart - 1) {
                    messages.scrollToPosition(positionStart);
                }
            }
        });
        messages.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        OverScrollDecoratorHelper.setUpOverScroll(
                messages, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        TextView title = view.findViewById(R.id.title);
        title.setText('@' + mUsername);
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel1.messages.observe(getViewLifecycleOwner(), adapter::submitList);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel1.messages.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        MessageInput input = view.findViewById(R.id.input);
        input.setInputListener(message -> {
            submitMessage(message);
            return true;
        });
    }

    public static MessageFragment newInstance(int thread, String username) {
        MessageFragment fragment = new MessageFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_THREAD, thread);
        arguments.putString(ARG_USERNAME, username);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showProfile(int user) {
        ((MainActivity)requireActivity()).showProfilePage(user);
    }

    private void submitMessage(CharSequence text) {
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        FormBody body = new FormBody.Builder()
                .add("text", text.toString())
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/threads/" + mThread + "/messages")
                .post(body)
                .header("Authorization", "Bearer " + token)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to submit message on video.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Submitting message on thread returned " + code + '.');
                if (code == 200) {
                    MessageDataSource source = mModel1.factory.source.getValue();
                    if (source != null) {
                        source.invalidate();
                    }

                    mModel2.areThreadsInvalid = true;
                }
            }
        });
    }

    private class MessageAdapter extends PagedListAdapter<MessageDataSource.Message, MessageFragment.MessageAdapter.MessageViewHolder> {

        private static final int TYPE_INBOX = 100;
        private static final int TYPE_OUTBOX = 101;

        protected MessageAdapter() {
            super(new DiffUtil.ItemCallback<MessageDataSource.Message>() {

                @Override
                public boolean areItemsTheSame(@NonNull MessageDataSource.Message a, @NonNull MessageDataSource.Message b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull MessageDataSource.Message a, @NonNull MessageDataSource.Message b) {
                    return TextUtils.equals(a.text, b.text);
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            MessageDataSource.Message message = getItem(position);
            //noinspection ConstantConditions
            return message.inbox ? TYPE_INBOX : TYPE_OUTBOX;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(type == TYPE_INBOX ? R.layout.item_message_in : R.layout.item_message_out, parent, false);
            return new MessageFragment.MessageAdapter.MessageViewHolder(view);
        }

        @Override
        @SuppressLint("SetTextI18n")
        @SuppressWarnings("ConstantConditions")
        public void onBindViewHolder(@NonNull MessageFragment.MessageAdapter.MessageViewHolder holder, int position) {
            MessageDataSource.Message message = getItem(position);
            if (holder.photo != null) {
                if (TextUtils.isEmpty(message.userPhoto)) {
                    holder.photo.setActualImageResource(R.drawable.photo_placeholder);
                } else {
                    holder.photo.setImageURI(message.userPhoto);
                }

                holder.photo.setOnClickListener(v -> showProfile(message.userId));
            }

            if (holder.username != null && holder.verified != null) {
                holder.username.setText('@' + message.userUsername);
                holder.username.setOnClickListener(v -> showProfile(message.userId));
                holder.verified.setVisibility(message.userVerified ? View.VISIBLE : View.GONE);
            }

            holder.text.setText(message.text);
            holder.when.setText(
                    DateUtils.getRelativeTimeSpanString(
                            requireContext(), message.dateCreated.getTime(), true));
        }

        public class MessageViewHolder extends RecyclerView.ViewHolder {

            public SimpleDraweeView photo;
            public TextView username;
            public ImageView verified;
            public TextView text;
            public TextView when;

            public MessageViewHolder(@NonNull View root) {
                super(root);
                username = root.findViewById(R.id.username);
                verified = root.findViewById(R.id.verified);
                photo = root.findViewById(R.id.photo);
                text = root.findViewById(R.id.text);
                when = root.findViewById(R.id.when);
            }
        }
    }

    public static class MessageFragmentViewModel extends ViewModel {

        public MessageFragmentViewModel(String url, String token, int thread) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            factory = new MessageDataSource.Factory(url, token, thread);
            state = Transformations.switchMap(factory.source, input -> input.state);
            messages = new LivePagedListBuilder<>(factory, config).build();
        }

        public final MessageDataSource.Factory factory;
        public final LiveData<PagedList<MessageDataSource.Message>> messages;
        public final LiveData<LoadingState> state;

        public static class Factory implements ViewModelProvider.Factory {

            private final int mThread;
            private final String mServerToken;
            private final String mServerUrl;

            public Factory(String url, String token, int thread) {
                mThread = thread;
                mServerToken = token;
                mServerUrl = url;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new MessageFragment.MessageFragmentViewModel(mServerUrl, mServerToken, mThread);
            }
        }
    }
}
