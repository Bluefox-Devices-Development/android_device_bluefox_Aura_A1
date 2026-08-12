/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.lineageos.settings.device.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SmartKeyAppPickerFragment extends Fragment {
    private static final String ARG_SETTING_KEY = "setting_key";
    private static final String ARG_GESTURE_TITLE = "gesture_title";

    private String mSettingKey;
    private AppAdapter mAdapter;

    static SmartKeyAppPickerFragment newInstance(String settingKey, int gestureTitle) {
        final SmartKeyAppPickerFragment fragment = new SmartKeyAppPickerFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(ARG_SETTING_KEY, settingKey);
        arguments.putInt(ARG_GESTURE_TITLE, gestureTitle);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mSettingKey = requireArguments().getString(ARG_SETTING_KEY);
        if (!SmartKeyContract.isSettingKey(mSettingKey)) {
            requireActivity().finish();
        }

        final View view = inflater.inflate(R.layout.fragment_app_picker, container, false);
        final SearchView searchView = view.findViewById(R.id.app_search);
        final ListView listView = view.findViewById(R.id.app_list);
        final TextView emptyView = view.findViewById(R.id.empty_view);

        mAdapter = new AppAdapter(loadLauncherApps());
        listView.setAdapter(mAdapter);
        listView.setEmptyView(emptyView);
        listView.setOnItemClickListener((parent, row, position, id) -> {
            final AppEntry selected = mAdapter.getItem(position);
            SmartKeyUiUtils.putAction(requireContext(), mSettingKey,
                    SmartKeyContract.forApp(selected.component));
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.filter(newText);
                return true;
            }
        });
        return view;
    }

    private List<AppEntry> loadLauncherApps() {
        final PackageManager packageManager = requireContext().getPackageManager();
        final Intent launcherIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> resolveInfos = new ArrayList<>(
                packageManager.queryIntentActivitiesAsUser(
                        launcherIntent, 0, UserHandle.myUserId()));
        resolveInfos.sort(new ResolveInfo.DisplayNameComparator(packageManager));

        final ArrayList<AppEntry> entries = new ArrayList<>();
        final Set<ComponentName> seenComponents = new HashSet<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null
                    || !resolveInfo.activityInfo.exported
                    || !resolveInfo.activityInfo.enabled) {
                continue;
            }
            final ComponentName component = new ComponentName(
                    resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            if (!seenComponents.add(component)) {
                continue;
            }
            entries.add(new AppEntry(
                    component,
                    resolveInfo.loadLabel(packageManager),
                    resolveInfo.loadIcon(packageManager)));
        }
        return entries;
    }

    private final class AppAdapter extends BaseAdapter {
        private final List<AppEntry> mAllEntries;
        private final List<AppEntry> mVisibleEntries = new ArrayList<>();
        private final LayoutInflater mInflater = LayoutInflater.from(requireContext());

        AppAdapter(List<AppEntry> entries) {
            mAllEntries = entries;
            mVisibleEntries.addAll(entries);
        }

        void filter(String query) {
            final String normalizedQuery = query == null
                    ? "" : query.trim().toLowerCase(Locale.getDefault());
            mVisibleEntries.clear();
            for (AppEntry entry : mAllEntries) {
                if (normalizedQuery.isEmpty()
                        || entry.label.toString().toLowerCase(Locale.getDefault())
                                .contains(normalizedQuery)
                        || entry.component.getPackageName().toLowerCase(Locale.ROOT)
                                .contains(normalizedQuery)) {
                    mVisibleEntries.add(entry);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mVisibleEntries.size();
        }

        @Override
        public AppEntry getItem(int position) {
            return mVisibleEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).component.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.app_picker_row, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final AppEntry entry = getItem(position);
            holder.icon.setImageDrawable(entry.icon);
            holder.label.setText(entry.label);
            holder.packageName.setText(entry.component.getPackageName());
            holder.selected.setChecked(entry.component.equals(getSelectedComponent()));
            return convertView;
        }

        private ComponentName getSelectedComponent() {
            return SmartKeyContract.getAppComponent(
                    SmartKeyUiUtils.getAction(requireContext(), mSettingKey));
        }
    }

    private static final class ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView packageName;
        final RadioButton selected;

        ViewHolder(View view) {
            icon = view.findViewById(R.id.app_icon);
            label = view.findViewById(R.id.app_label);
            packageName = view.findViewById(R.id.app_package);
            selected = view.findViewById(R.id.app_selected);
        }
    }

    private static final class AppEntry {
        final ComponentName component;
        final CharSequence label;
        final Drawable icon;

        AppEntry(ComponentName component, CharSequence label, Drawable icon) {
            this.component = component;
            this.label = label;
            this.icon = icon;
        }
    }
}
