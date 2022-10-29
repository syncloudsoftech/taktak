package com.syncloudsoft.taktak.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.events.ShowNewsEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowNewsEvent(ShowNewsEvent event) {
        //noinspection ConstantConditions
        ViewPager2 pager = getView().findViewById(R.id.pager);
        pager.setCurrentItem(0);
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
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new HomePagerAdapter(this));
        pager.setCurrentItem(1, false);
        pager.setUserInputEnabled(false);
        TabLayout tabs = view.findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            int text = position == 0
                    ? R.string.news_label
                    : R.string.clips_label;
            tab.setText(text);
        }).attach();
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private static class HomePagerAdapter extends FragmentStateAdapter {

        public HomePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return NewsFragment.newInstance();
            }

            return PlayerSliderFragment.newInstance(null, 0, 0, false);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
