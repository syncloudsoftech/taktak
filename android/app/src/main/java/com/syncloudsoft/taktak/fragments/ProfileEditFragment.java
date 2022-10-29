package com.syncloudsoft.taktak.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.workers.UpdateProfileWorker;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProfileEditFragment extends Fragment {

    private static final String ARG_USER = "user";
    private static final String TAG = "ProfileEditFragment";

    private ProfileEditFragmentModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;
    private ProfileFragment.User mUser;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri uri = result.getUri();
            Log.v(TAG, "Copped image as saved to " + uri);
            mModel1.photo = uri.getPath();
            mModel1.remove = false;
            updatePhoto();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = requireArguments().getParcelable(ARG_USER);
        mModel1 = new ViewModelProvider(this).get(ProfileEditFragmentModel.class);
        mModel1.name = mUser.name;
        mModel1.username = mUser.username;
        mModel1.email = mUser.email;
        mModel1.bio = mUser.bio;
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton close = view.findViewById(R.id.close);
        close.setOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack());
        SimpleDraweeView photo = view.findViewById(R.id.photo);
        photo.setOnClickListener(v -> choosePhotoAction());
        TextInputLayout name = view.findViewById(R.id.name);
        //noinspection ConstantConditions
        name.getEditText().setText(mModel1.name);
        name.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel1.name = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout username = view.findViewById(R.id.username);
        //noinspection ConstantConditions
        username.getEditText().setText(mModel1.username);
        username.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel1.username = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout email = view.findViewById(R.id.email);
        //noinspection ConstantConditions
        email.getEditText().setText(mModel1.email);
        email.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel1.email = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout bio = view.findViewById(R.id.bio);
        //noinspection ConstantConditions
        bio.getEditText().setText(mModel1.bio);
        bio.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel1.bio = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        final Button save = view.findViewById(R.id.save);
        save.setOnClickListener(v -> saveProfile());
        mModel1.errors.observe(getViewLifecycleOwner(), errors -> {
            name.setError(null);
            username.setError(null);
            email.setError(null);
            bio.setError(null);
            if (errors == null) {
                return;
            }

            if (errors.containsKey("name")) {
                name.setError(errors.get("name"));
            }

            if (errors.containsKey("username")) {
                username.setError(errors.get("username"));
            }

            if (errors.containsKey("email")) {
                email.setError(errors.get("email"));
            }

            if (errors.containsKey("bio")) {
                bio.setError(errors.get("bio"));
            }
        });
        updatePhoto();
    }

    private void choosePhotoAction() {
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(R.array.photo_options, (dialogInterface, i) -> {
                    if (i == 0) {
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setMinCropResultSize(256,256)
                                .start(requireContext(), ProfileEditFragment.this);
                    } else {
                        mModel1.photo = null;
                        mModel1.remove = true;
                        updatePhoto();
                    }
                })
                .show();
    }

    private void saveProfile() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel1.errors.postValue(null);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        Data data = new Data.Builder()
                .putString(UpdateProfileWorker.KEY_BIO, mModel1.bio)
                .putString(UpdateProfileWorker.KEY_EMAIL, mModel1.email)
                .putString(UpdateProfileWorker.KEY_NAME, mModel1.name)
                .putString(UpdateProfileWorker.KEY_PHOTO, mModel1.photo)
                .putBoolean(UpdateProfileWorker.KEY_REMOVE, mModel1.remove)
                .putString(UpdateProfileWorker.KEY_TOKEN, token)
                .putString(UpdateProfileWorker.KEY_USERNAME, mModel1.username)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(UpdateProfileWorker.class)
                .setInputData(data)
                .build();
        WorkManager wm = WorkManager.getInstance(requireContext());
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), info -> {
                    boolean ended = info.getState() == WorkInfo.State.CANCELLED
                            || info.getState() == WorkInfo.State.FAILED;
                    if (info.getState() == WorkInfo.State.SUCCEEDED) {
                        progress.dismiss();
                        mModel2.isProfileInvalid = true;
                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    } else if (ended) {
                        Toast.makeText(requireContext(), R.string.profile_error_invalid, Toast.LENGTH_SHORT).show();
                        Data out = info.getOutputData();
                        String json = out.getString(UpdateProfileWorker.KEY_ERRORS);
                        if (json != null) {
                            try {
                                showErrors(new JSONObject(json));
                            } catch (Exception ignore) {
                            }
                        }

                        progress.dismiss();
                    }
                });
    }

    public static ProfileEditFragment newInstance(ProfileFragment.User user) {
        ProfileEditFragment fragment = new ProfileEditFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void showErrors(JSONObject errors) throws Exception {
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{"name", "username", "email", "bio"};
        for (String key : keys) {
            JSONObject failures = errors.optJSONObject(key);
            if (failures != null) {
                String first = failures.keys().next();
                messages.put(key, failures.getString(first));
            }
        }

        mModel1.errors.postValue(messages);
    }

    private void updatePhoto() {
        //noinspection ConstantConditions
        SimpleDraweeView photo = getView().findViewById(R.id.photo);
        if (mModel1.remove) {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        } else if (!TextUtils.isEmpty(mModel1.photo)) {
            photo.setImageURI(Uri.fromFile(new File(mModel1.photo)));
        } else if (!TextUtils.isEmpty(mUser.photo)) {
            photo.setImageURI(mUser.photo);
        } else {
            photo.setActualImageResource(R.drawable.photo_placeholder);
        }
    }

    public static class ProfileEditFragmentModel extends ViewModel {

        public String photo;
        public String name;
        public String username;
        public String email;
        public String bio;
        public boolean remove;

        public final MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
    }
}
