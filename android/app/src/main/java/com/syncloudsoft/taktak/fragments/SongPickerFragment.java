package com.syncloudsoft.taktak.fragments;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.syncloudsoft.taktak.R;
import com.syncloudsoft.taktak.SharedConstants;
import com.syncloudsoft.taktak.common.LoadingState;
import com.syncloudsoft.taktak.data.SongDataSource;

import java.util.List;

import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class SongPickerFragment extends Fragment {

    private static final String ARG_SECTION = "section";

    private OnSongSelectListener mListener;
    private SongPickerFragmentViewModel mModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int section = requireArguments().getInt(ARG_SECTION);
        String token = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(SharedConstants.PREF_SERVER_TOKEN, null);
        SongPickerFragmentViewModel.Factory factory =
                new SongPickerFragmentViewModel.Factory(
                        getString(R.string.server_url), token, section);
        mModel = new ViewModelProvider(this, factory)
                .get(SongPickerFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView songs = view.findViewById(R.id.songs);
        SongPickerAdapter adapter = new SongPickerAdapter();
        songs.setAdapter(new SlideInLeftAnimationAdapter(adapter));
        songs.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        OverScrollDecoratorHelper.setUpOverScroll(
                songs, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        mModel.songs.observe(getViewLifecycleOwner(), adapter::submitList);
        View empty = view.findViewById(R.id.empty);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            List<?> list = mModel.songs.getValue();
            if (state == LoadingState.LOADING) {
                empty.setVisibility(View.GONE);
            } else {
                empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
        });
    }

    public static SongPickerFragment newInstance(int section) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_SECTION, section);
        SongPickerFragment fragment = new SongPickerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public void setOnSongSelectListener(OnSongSelectListener listener) {
        mListener = listener;
    }

    public interface OnSongSelectListener {

        void onSongSelect(SongDataSource.Song song);
    }

    private class SongPickerAdapter extends PagedListAdapter<SongDataSource.Song, SongViewHolder> {

        public SongPickerAdapter() {
            super(new DiffUtil.ItemCallback<SongDataSource.Song>() {

                @Override
                public boolean areItemsTheSame(@NonNull SongDataSource.Song a, @NonNull SongDataSource.Song b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull SongDataSource.Song a, @NonNull SongDataSource.Song b) {
                    return a.name.equals(b.name);
                }
            });
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View root = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(root);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            SongDataSource.Song song = getItem(position);
            //noinspection ConstantConditions
            if (TextUtils.isEmpty(song.icon)) {
                holder.icon.setActualImageResource(R.drawable.image_placeholder);
            } else {
                holder.icon.setImageURI(song.icon);
            }

            holder.name.setText(song.name);
            holder.itemView.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onSongSelect(song);
                }
            });
        }
    }

    public static class SongPickerFragmentViewModel extends ViewModel {

        public SongPickerFragmentViewModel(String url, String token, int section) {
            PagedList.Config config = new PagedList.Config.Builder()
                    .setPageSize(10)
                    .build();
            SongDataSource.Factory factory = new SongDataSource.Factory(url, token, section);
            state = Transformations.switchMap(factory.source, input -> input.state);
            songs = new LivePagedListBuilder<>(factory, config).build();
        }

        public final LiveData<PagedList<SongDataSource.Song>> songs;
        public final LiveData<LoadingState> state;

        public static class Factory implements ViewModelProvider.Factory {

            private final int mSection;
            private final String mServerToken;
            private final String mServerUrl;

            public Factory(String url, String token, int section) {
                mSection = section;
                mServerToken = token;
                mServerUrl = url;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new SongPickerFragmentViewModel(mServerUrl, mServerToken, mSection);
            }
        }
    }

    private static class SongViewHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView icon;
        public TextView name;

        public SongViewHolder(@NonNull View root) {
            super(root);
            icon = root.findViewById(R.id.icon);
            name = root.findViewById(R.id.name);
        }
    }
}
