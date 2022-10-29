package com.syncloudsoft.taktak.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.syncloudsoft.taktak.R;

public class InboxFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout tabs = view.findViewById(R.id.tabs);
        ViewPager2 pager = view.findViewById(R.id.pager);
        pager.setAdapter(new NotificationTabPagerAdapter(this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.notification_label);
            } else {
                tab.setText(R.string.message_label);
            }
        }).attach();
        if (getResources().getBoolean(R.bool.admob_notifications_ad_enabled)) {
            AdView ad = new AdView(requireContext());
            ad.setAdSize(AdSize.BANNER);
            ad.setAdUnitId(getString(R.string.admob_notifications_ad_id));
            ad.loadAd(new AdRequest.Builder().build());
            LinearLayout banner = view.findViewById(R.id.banner);
            banner.addView(ad);
        }
    }

    public static InboxFragment newInstance() {
        return new InboxFragment();
    }

    private static class NotificationTabPagerAdapter extends FragmentStateAdapter {

        public NotificationTabPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return NotificationFragment.newInstance();
            }

            return ThreadFragment.newInstance();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
