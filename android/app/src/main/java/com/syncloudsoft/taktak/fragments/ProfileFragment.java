package com.syncloudsoft.taktak.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.ThreadDataSource;
import com.syncloudsoft.taktak.utils.JsonUtil;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private static final String ARG_USER = "user";
    private static final String TAG = "ProfileFragment";

    private final OkHttpClient mHttpClient = new OkHttpClient();
    private ProfileFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private Integer mUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = requireArguments().getInt(ARG_USER, 0);
        if (mUser <= 0) {
            mUser = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mModel1 = new ViewModelProvider(this).get(ProfileFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity()).get(MainActivity.MainActivityViewModel.class);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ViewPager2 pager = getView().findViewById(R.id.pager);
        LoadingState state = mModel1.state.getValue();
        User user = mModel1.user.getValue();
        if ((mModel2.isProfileInvalid || user == null) && state != LoadingState.LOADING) {
            loadUser();
        } else if (user != null && pager.getAdapter() == null) {
            showVideosGrid(user);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton close = view.findViewById(R.id.close);
        close.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack());
        close.setVisibility(mUser == null ? View.GONE : View.VISIBLE);
        ImageButton message = view.findViewById(R.id.message);
        message.setVisibility(mUser == null ? View.GONE : View.VISIBLE);
        message.setOnClickListener(v -> {
            if (mModel2.isLoggedIn) {
                User user = mModel1.user.getValue();
                if (user != null) {
                    if (user.thread != null) {
                        ThreadDataSource.Thread thread = new ThreadDataSource.Thread();
                        thread.id = user.thread;
                        thread.userId = user.id;
                        thread.userUsername = user.username;
                        thread.userPhoto = user.photo;
                        ((MainActivity)requireActivity()).showChatForThread(thread);
                    } else {
                        confirmNewThread();
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
            }
        });
        ImageButton menu = view.findViewById(R.id.menu);
        menu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.edit:
                        showEditor();
                        break;
                    case R.id.logout:
                        logoutUser();
                        break;
                }

                return true;
            });
            popup.show();
        });
        menu.setVisibility(mUser != null ? View.GONE : View.VISIBLE);
        MaterialButton follow = view.findViewById(R.id.follow);
        follow.setOnClickListener(v -> {
            if (mModel2.isLoggedIn) {
                followUnfollowUser();
            } else {
                Toast.makeText(requireContext(), R.string.login_required_message, Toast.LENGTH_SHORT).show();
            }
        });
        follow.setVisibility(mUser == null ? View.GONE : View.VISIBLE);
        View followers = view.findViewById(R.id.followers_count);
        followers.setOnClickListener(v -> showFollowerFollowing(false));
        View followings = view.findViewById(R.id.followings_count);
        followings.setOnClickListener(v -> showFollowerFollowing(true));
        SimpleDraweeView photo = view.findViewById(R.id.photo);
        TextView username = view.findViewById(R.id.username);
        TextView bio = view.findViewById(R.id.bio);
        ImageView verified = view.findViewById(R.id.verified);
        TextView videos = view.findViewById(R.id.videos);
        TextView followers2 = view.findViewById(R.id.followers);
        TextView followings2 = view.findViewById(R.id.followings);
        View loading = view.findViewById(R.id.loading);
        mModel1.state.observe(getViewLifecycleOwner(), state -> {
            if (state == LoadingState.ERROR) {
                Toast.makeText(requireContext(), R.string.error_internet, Toast.LENGTH_SHORT).show();
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
        mModel1.user.observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                return;
            }

            if (TextUtils.isEmpty(user.photo)) {
                photo.setActualImageResource(R.drawable.photo_placeholder);
            } else {
                photo.setImageURI(user.photo);
            }

            username.setText('@' + user.username);
            verified.setVisibility(user.verified ? View.VISIBLE : View.GONE);
            bio.setText(user.bio);
            bio.setVisibility(TextUtils.isEmpty(user.bio) ? View.GONE : View.VISIBLE);
            videos.setText(user.videos + "");
            followers2.setText(user.followers + "");
            followings2.setText(user.followings + "");
            follow.setIconResource(
                    user.following
                            ? R.drawable.ic_baseline_check_24
                            : R.drawable.ic_baseline_add_24);
            follow.setText(user.following ? R.string.unfollow_label : R.string.follow_label);
            follow.setVisibility(user.self ? View.GONE : View.VISIBLE);
            showVideosGrid(user);
        });
        if (getResources().getBoolean(R.bool.admob_profile_ad_enabled)) {
            AdView ad = new AdView(requireContext());
            ad.setAdSize(AdSize.BANNER);
            ad.setAdUnitId(getString(R.string.admob_profile_ad_id));
            ad.loadAd(new AdRequest.Builder().build());
            LinearLayout banner = view.findViewById(R.id.banner);
            banner.addView(ad);
        }
    }

    private void confirmNewThread() {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirmation_start_thread)
                .setNegativeButton(R.string.cancel_button, (dialog, i) -> dialog.cancel())
                .setPositiveButton(R.string.yes_button, (dialog, i) -> {
                    dialog.dismiss();
                    startAndOpenThread();
                })
                .show();
    }

    private void followUnfollowUser() {
        User user = mModel1.user.getValue();
        if (user == null) {
            return;
        }

        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request.Builder builder = new Request.Builder()
                .url(getString(R.string.server_url) + "api/users/" + user.id + "/follow")
                .header("Authorization", "Bearer " + token);
        if (user.following) {
            builder = builder.delete();
        } else {
            builder = builder.post(RequestBody.create(null, new byte[0]));
        }

        mHttpClient.newCall(builder.build()).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to update follow/unfollow user.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Updating follow/unfollow returned " + code + '.');
            }
        });

        if (user.following) {
            user.followers--;
        } else {
            user.followers++;
        }

        user.following = !user.following;
        mModel1.user.postValue(user);
    }

    private void loadUser() {
        mModel1.state.postValue(LoadingState.LOADING);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Request.Builder builder = new Request.Builder().get();
        if (mUser == null) {
            builder.url(getString(R.string.server_url) + "api/self");
        } else {
            builder.url(getString(R.string.server_url) + "api/users/" + mUser);
        }

        if (!TextUtils.isEmpty(token)) {
            builder.header("Authorization", "Bearer " + token);
        }

        mHttpClient.newCall(builder.build()).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed when trying to retrieve profile.", e);
                mModel1.state.postValue(LoadingState.ERROR);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Retrieving profile from user returned " + code + '.');
                if (code == 200) {
                    try {
                        //noinspection ConstantConditions
                        String data = response.body().string();
                        JSONObject json = new JSONObject(data);
                        mModel1.user.postValue(transformData(json));
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to read JSON response from server.", e);
                    }

                    mModel2.isProfileInvalid = false;
                    mModel1.state.postValue(LoadingState.LOADED);
                } else {
                    mModel1.state.postValue(LoadingState.ERROR);
                }
            }
        });
    }

    private void logoutUser() {
        ((MainActivity)requireActivity()).logout();
    }

    public static ProfileFragment newInstance(@Nullable Integer user) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle arguments = new Bundle();
        if (user != null) {
            arguments.putInt(ARG_USER, user);
        }

        fragment.setArguments(arguments);
        return fragment;
    }

    private void showChatForThread(ThreadDataSource.Thread thread) {
        ((MainActivity)requireActivity()).showChatForThread(thread);
    }

    private void showEditor() {
        User user = mModel1.user.getValue();
        if (user != null) {
            ((MainActivity)requireActivity()).showProfileEditor(user);
        }
    }

    private void showFollowerFollowing(boolean following) {
        User user = mModel1.user.getValue();
        if (user != null) {
            ((MainActivity)requireActivity()).showFollowerFollowing(user.id, following);
        }
    }

    private void showVideosGrid(User user) {
        //noinspection ConstantConditions
        TabLayout tabs = getView().findViewById(R.id.tabs);
        ViewPager2 pager = getView().findViewById(R.id.pager);
        pager.setAdapter(new ProfilePagerAdapter(user, this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            int icon = position == 0
                    ? R.drawable.ic_outline_ondemand_video_24
                    : R.drawable.ic_baseline_favorite_border_24;
            tab.setIcon(ContextCompat.getDrawable(requireContext(), icon));
        }).attach();
    }

    private void startAndOpenThread() {
        User user = mModel1.user.getValue();
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        //noinspection ConstantConditions
        FormBody body = new FormBody.Builder()
                .add("user_id", user.id + "")
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/threads")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Failed to start a new thread.", e);
            }

            @Override
            public void onResponse(@Nullable Call call, @Nullable Response response) {
                int code = response != null ? response.code() : -1;
                Log.v(TAG, "Starting a new thread returned " + code + '.');
                if (code == 200) {
                    try {
                        //noinspection ConstantConditions
                        String body = response.body().string();
                        JSONObject object = new JSONObject(body);
                        ThreadDataSource.Thread thread = ThreadDataSource.Thread.transform(object);
                        showChatForThread(thread);
                    } catch (Exception ignore) {
                    }
                }
            }
        });
    }

    private User transformData(JSONObject object) throws Exception {
        User user = new User();
        user.id = object.getInt("id");
        user.name = object.getString("name");
        user.username = object.getString("username");
        user.email = JsonUtil.optString(object, "email");
        user.photo = JsonUtil.optString(object, "photo");
        user.bio = JsonUtil.optString(object, "bio");
        user.verified = object.getInt("verified") == 1;
        user.self = object.getInt("self") == 1;
        user.following = object.getInt("following") == 1;
        user.videos = object.getInt("videos");
        user.followers = object.getInt("followers");
        user.followings = object.getInt("followings");
        user.thread = JsonUtil.optInt(object, "thread");
        return user;
    }

    public static class ProfileFragmentViewModel extends ViewModel {

        public final MutableLiveData<LoadingState> state = new MutableLiveData<>(LoadingState.IDLE);
        public final MutableLiveData<User> user = new MutableLiveData<>();
    }

    private static class ProfilePagerAdapter extends FragmentStateAdapter {

        private final User mUser;

        public ProfilePagerAdapter(User user, @NonNull Fragment fragment) {
            super(fragment);
            mUser = user;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return VideoGridFragment.newInstance(mUser.id, false);
            }

            return VideoGridFragment.newInstance(mUser.id, true);
        }

        @Override
        public int getItemCount() {
            return mUser.self ? 2 : 1;
        }
    }

    public static class User implements Parcelable {

        public int id;
        public String name;
        public String username;
        public String email;
        public String photo;
        public String bio;
        public boolean self;
        public boolean verified;
        public boolean following;
        public int videos;
        public int followers;
        public int followings;
        @Nullable public Integer thread;

        public User() { }

        protected User(Parcel in) {
            id = in.readInt();
            name = in.readString();
            username = in.readString();
            email = in.readString();
            photo = in.readString();
            bio = in.readString();
            self = in.readByte() != 0;
            verified = in.readByte() != 0;
            following = in.readByte() != 0;
            videos = in.readInt();
            followers = in.readInt();
            followings = in.readInt();
            if (in.readByte() == 0) {
                thread = null;
            } else {
                thread = in.readInt();
            }
        }

        public static final Creator<User> CREATOR = new Creator<User>() {
            @Override
            public User createFromParcel(Parcel in) {
                return new User(in);
            }

            @Override
            public User[] newArray(int size) {
                return new User[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(id);
            parcel.writeString(name);
            parcel.writeString(username);
            parcel.writeString(email);
            parcel.writeString(photo);
            parcel.writeString(bio);
            parcel.writeByte((byte) (self ? 1 : 0));
            parcel.writeByte((byte) (verified ? 1 : 0));
            parcel.writeByte((byte) (following ? 1 : 0));
            parcel.writeInt(videos);
            parcel.writeInt(followers);
            parcel.writeInt(followings);
            if (thread == null) {
                parcel.writeByte((byte) 0);
            } else {
                parcel.writeByte((byte) 1);
                parcel.writeInt(thread);
            }
        }
    }
}
