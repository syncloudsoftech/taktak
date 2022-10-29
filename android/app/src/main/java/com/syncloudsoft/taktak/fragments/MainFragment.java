package com.syncloudsoft.taktak.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.activities.MainActivity;
import com.syncloudsoft.taktak.activities.RecorderActivity;
import com.syncloudsoft.taktak.events.ShowNewsEvent;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";

    private MainFragmentViewModel mModel1;
    private MainActivity.MainActivityViewModel mModel2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel1 = new ViewModelProvider(this).get(MainFragmentViewModel.class);
        mModel2 = new ViewModelProvider(requireActivity())
                .get(MainActivity.MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //noinspection ConstantConditions
        ViewPager2 pager = getView().findViewById(R.id.pager);
        AnimatedBottomBar toolbar = getView().findViewById(R.id.toolbar);
        toolbar.selectTabAt(pager.getCurrentItem(), false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new MainAdapter(this));
        pager.setUserInputEnabled(false);
        AnimatedBottomBar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {

            @Override
            public void onTabSelected(
                    int i,
                    @Nullable AnimatedBottomBar.Tab previous,
                    int j,
                    @NotNull AnimatedBottomBar.Tab current
            ) {
                Log.v(TAG, "Previous tab: " + i + "; new tab: " + j);
                if (j >= 3 && !mModel2.isLoggedIn) {
                    toolbar.selectTabAt(i, false);
                    ((MainActivity)requireActivity()).showLoginSheet();
                } else {
                    pager.setCurrentItem(j, false);
                }
            }

            @Override
            public void onTabReselected(int i, @NotNull AnimatedBottomBar.Tab tab) { }
        });
        View create = view.findViewById(R.id.create);
        create.setOnClickListener(v -> {
            if (mModel2.isLoggedIn) {
                startActivity(new Intent(requireContext(), RecorderActivity.class));
            } else {
                ((MainActivity)requireActivity()).showLoginSheet();
            }
        });
        if (!mModel1.isGreetingShown) {
            mModel1.isGreetingShown = true;
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int greeting;
            if (hour >= 1 && hour <= 11){
                greeting = R.string.greeting_morning;
            } else if (hour <= 15){
                greeting = R.string.greeting_afternoon;
            } else {
                greeting = R.string.greeting_evening;
            }

            Snackbar.make(view, greeting, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.yes_label, v -> EventBus.getDefault().post(new ShowNewsEvent()))
                    .show();
        }
    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    private static class MainAdapter extends FragmentStateAdapter {

        public MainAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return HomeFragment.newInstance();
                case 1:
                    return DiscoverFragment.newInstance();
                case 3:
                    return InboxFragment.newInstance();
                case 4:
                    return ProfileFragment.newInstance(null);
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    public static class MainFragmentViewModel extends ViewModel {

        public boolean isGreetingShown;
    }
}
