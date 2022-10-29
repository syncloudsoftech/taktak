package com.syncloudsoft.taktak.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.data.ThreadDataSource;
import com.syncloudsoft.taktak.data.VideoDataSource;
import com.syncloudsoft.taktak.fragments.MessageFragment;
import com.syncloudsoft.taktak.fragments.CommentFragment;
import com.syncloudsoft.taktak.fragments.FollowerFollowingFragment;
import com.syncloudsoft.taktak.fragments.MainFragment;
import com.syncloudsoft.taktak.fragments.PlayerFragment;
import com.syncloudsoft.taktak.fragments.ProfileEditFragment;
import com.syncloudsoft.taktak.fragments.ProfileFragment;
import com.syncloudsoft.taktak.workers.DeviceTokenWorker;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CallbackManager mCallbackManager;
    private final Handler mHandler = new Handler();
    private final OkHttpClient mHttpClient = new OkHttpClient();
    private MainActivityViewModel mModel;
    private GoogleSignInClient mSignInClient;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == SharedConstants.REQUEST_CODE_LOGIN_GOOGLE && resultCode == RESULT_OK && data != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                loginWithGoogle(task.getResult(ApiException.class));
            } catch (ApiException e) {
                Log.e(TAG, "Unable to login with Google account.");
            }
        }

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token = prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
        mModel.isLoggedIn = !TextUtils.isEmpty(token);
        final BottomSheetBehavior<View> bsb =
                BottomSheetBehavior.from(findViewById(R.id.login_sheet));
        ImageButton close = findViewById(R.id.close);
        close.setOnClickListener(view -> bsb.setState(BottomSheetBehavior.STATE_COLLAPSED));
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance()
                .registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

                    @Override
                    public void onCancel() {
                        Log.w(TAG, "Login with Facebook was cancelled.");
                    }

                    @Override
                    public void onError(@NonNull FacebookException error) {
                        Log.e(TAG, "Login with Facebook returned error.", error);
                        Toast.makeText(MainActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(LoginResult result) {
                        loginWithFacebook(result);
                    }
                });
        ImageButton facebook = findViewById(R.id.facebook);
        facebook.setOnClickListener(view -> {
            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            LoginManager.getInstance()
                    .logInWithReadPermissions(
                            MainActivity.this, Collections.singletonList("email"));
        });
        GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(getString(R.string.google_client_id))
                        .requestProfile()
                        .build();
        mSignInClient = GoogleSignIn.getClient(this, options);
        ImageButton google = findViewById(R.id.google);
        google.setOnClickListener(view -> {
            bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            startActivityForResult(
                    mSignInClient.getSignInIntent(), SharedConstants.REQUEST_CODE_LOGIN_GOOGLE);
        });
        syncFcmToken();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, MainFragment.newInstance())
                .commit();
        boolean intro = prefs.getBoolean(SharedConstants.PREF_INTRO_SHOWN, false);
        if (!intro) {
            prefs.edit().putBoolean(SharedConstants.PREF_INTRO_SHOWN, true).apply();
            startActivity(new Intent(this, FirstLaunchActivity.class));
        }
    }

    private void loginWithFacebook(LoginResult result) {
        Log.d(TAG, "User logged in Facebook ID " + result.getAccessToken().getUserId() + '.');
        RequestBody form = new FormBody.Builder()
                .add("access_token", result.getAccessToken().getToken())
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/login/facebook")
                .post(form)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Login request with Facebook has failed.", e);
                Toast.makeText(MainActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
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
                        String jwt = json.getString("jwt");
                        mHandler.post(() -> updateLoginState(jwt));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response from server.", e);
                    Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loginWithGoogle(@Nullable GoogleSignInAccount account) {
        if (account == null) {
            Log.v(TAG, "Could not retrieve a Google account after login.");
            return;
        }

        Log.d(TAG, "User logged in Google ID " + account.getId() + '.');
        //noinspection ConstantConditions
        RequestBody form = new FormBody.Builder()
                .add("id_token", account.getIdToken())
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.server_url) + "api/login/google")
                .post(form)
                .build();
        mHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nullable Call call, @Nullable IOException e) {
                Log.e(TAG, "Login request with Google has failed.", e);
                Toast.makeText(MainActivity.this, R.string.error_internet, Toast.LENGTH_SHORT).show();
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
                        String jwt = json.getString("jwt");
                        mHandler.post(() -> updateLoginState(jwt));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse response from server.", e);
                    Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void logout() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .remove(SharedConstants.PREF_SERVER_TOKEN)
                .remove(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT)
                .apply();
        mModel.isLoggedIn = false;
        restartActivity();
    }

    private void restartActivity() {
        startActivity(Intent.makeRestartActivityTask(getComponentName()));
    }

    public void showChatForThread(ThreadDataSource.Thread thread) {
        MessageFragment fragment = MessageFragment.newInstance(thread.id, thread.userUsername);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void showCommentsPage(int video) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, CommentFragment.newInstance(video))
                .addToBackStack(null)
                .commit();
    }

    public void showFollowerFollowing(int user, boolean following) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, FollowerFollowingFragment.newInstance(user, following))
                .addToBackStack(null)
                .commit();
    }

    public void showLoginSheet() {
        final BottomSheetBehavior<View> bsb =
                BottomSheetBehavior.from(findViewById(R.id.login_sheet));
        bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void showProfileEditor(ProfileFragment.User user) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, ProfileEditFragment.newInstance(user))
                .addToBackStack(null)
                .commit();
    }

    public void showProfilePage(int user) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, ProfileFragment.newInstance(user))
                .addToBackStack(null)
                .commit();
    }

    public void showVideoPlayer(VideoDataSource.Video video) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.host, PlayerFragment.newInstance(video))
                .addToBackStack(null)
                .commit();
    }

    private void syncFcmToken() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token1 = prefs.getString(SharedConstants.PREF_FCM_TOKEN, null);
        if (TextUtils.isEmpty(token1)) {
            return;
        }

        long synced = prefs.getLong(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT, 0);
        if (synced <= 0 || synced < (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15))) {
            prefs.edit().putLong(SharedConstants.PREF_FCM_TOKEN_SYNCED_AT, System.currentTimeMillis()).apply();
            String token2 = prefs.getString(SharedConstants.PREF_SERVER_TOKEN, null);
            Data input = new Data.Builder()
                    .putString(DeviceTokenWorker.KEY_FCM_TOKEN, token1)
                    .putString(DeviceTokenWorker.KEY_SERVER_TOKEN, token2)
                    .build();
            WorkRequest request = new OneTimeWorkRequest.Builder(DeviceTokenWorker.class)
                    .setInputData(input)
                    .build();
            WorkManager.getInstance(this).enqueue(request);
        }
    }

    private void updateLoginState(String jwt) {
        Log.v(TAG, "Received token from server i.e., " + jwt);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(SharedConstants.PREF_SERVER_TOKEN, jwt)
                .apply();
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        restartActivity();
    }

    public static class MainActivityViewModel extends ViewModel {

        public boolean areThreadsInvalid;
        public boolean isLoggedIn;
        public boolean isProfileInvalid;
    }
}
