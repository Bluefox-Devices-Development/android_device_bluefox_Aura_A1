/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.lineageos.settings.device.R;

public final class SmartKeyActionPickerFragment extends PreferenceFragmentCompat
        implements SelectorWithWidgetPreference.OnClickListener {
    private static final String ARG_SETTING_KEY = "setting_key";
    private static final String ARG_GESTURE_TITLE = "gesture_title";
    private static final String EMERGENCY_SETTINGS_ACTION =
            "android.settings.EMERGENCY_SETTINGS";

    private String mSettingKey;
    private int mGestureTitle;

    static SmartKeyActionPickerFragment newInstance(
            String settingKey, @StringRes int gestureTitle) {
        final SmartKeyActionPickerFragment fragment = new SmartKeyActionPickerFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(ARG_SETTING_KEY, settingKey);
        arguments.putInt(ARG_GESTURE_TITLE, gestureTitle);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Bundle arguments = requireArguments();
        mSettingKey = arguments.getString(ARG_SETTING_KEY);
        mGestureTitle = arguments.getInt(ARG_GESTURE_TITLE, R.string.smart_key_title);
        if (!SmartKeyContract.isSettingKey(mSettingKey)) {
            requireActivity().finish();
            return;
        }
        rebuildPreferences();
    }

    @StringRes
    int getGestureTitle() {
        if (mGestureTitle != 0) {
            return mGestureTitle;
        }
        final Bundle arguments = getArguments();
        return arguments == null
                ? R.string.smart_key_title
                : arguments.getInt(ARG_GESTURE_TITLE, R.string.smart_key_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SmartKeyContract.isSettingKey(mSettingKey)) {
            rebuildPreferences();
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final String action = preference.getKey();
        if (SmartKeyContract.ACTION_APP_PREFIX.equals(action)) {
            ((SmartKeyActivity) requireActivity()).showAppPicker(mSettingKey, mGestureTitle);
            return;
        }
        if (SmartKeyContract.ACTION_EMERGENCY.equals(action)
                && !SmartKeyUiUtils.isEmergencyEnabled(requireContext())) {
            showEmergencySetupDialog();
            rebuildPreferences();
            return;
        }
        SmartKeyUiUtils.putAction(requireContext(), mSettingKey, action);
        rebuildPreferences();
    }

    private void rebuildPreferences() {
        final PreferenceScreen screen = getPreferenceManager()
                .createPreferenceScreen(requireContext());
        setPreferenceScreen(screen);

        final String selectedAction = SmartKeyUiUtils.getAction(requireContext(), mSettingKey);
        addAction(screen, SmartKeyContract.ACTION_NONE, R.string.action_none, 0,
                selectedAction);

        final PreferenceCategory shortcuts = new PreferenceCategory(requireContext());
        shortcuts.setTitle(R.string.quick_actions_category_title);
        screen.addPreference(shortcuts);
        addAction(shortcuts, SmartKeyContract.ACTION_FLASHLIGHT, R.string.action_flashlight,
                R.string.screen_off_available, selectedAction);
        addAction(shortcuts, SmartKeyContract.ACTION_CAMERA, R.string.action_camera,
                R.string.screen_on_only, selectedAction);
        addAction(shortcuts, SmartKeyContract.ACTION_SCREENSHOT, R.string.action_screenshot,
                R.string.screen_on_only, selectedAction);
        addAction(shortcuts, SmartKeyContract.ACTION_RECORDER, R.string.action_recorder,
                R.string.screen_on_only, selectedAction);
        addAction(shortcuts, SmartKeyContract.ACTION_MUTE_RING, R.string.action_mute_ring,
                R.string.screen_off_available, selectedAction);
        addAction(shortcuts, SmartKeyContract.ACTION_EMERGENCY, R.string.action_emergency,
                SmartKeyUiUtils.isEmergencyEnabled(requireContext())
                        ? R.string.screen_off_available : R.string.emergency_setup_required,
                selectedAction);

        final PreferenceCategory applications = new PreferenceCategory(requireContext());
        applications.setTitle(R.string.applications_category_title);
        screen.addPreference(applications);
        final SelectorWithWidgetPreference appPreference = addAction(applications,
                SmartKeyContract.ACTION_APP_PREFIX, R.string.action_open_app,
                R.string.screen_on_only, selectedAction);
        if (SmartKeyContract.isAppAction(selectedAction)) {
            appPreference.setSummary(SmartKeyUiUtils.getActionLabel(
                    requireContext(), selectedAction));
        }
    }

    private SelectorWithWidgetPreference addAction(PreferenceCategory category, String action,
            @StringRes int title, @StringRes int summary, String selectedAction) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(
                requireContext());
        preference.setKey(action);
        preference.setTitle(title);
        if (summary != 0) {
            preference.setSummary(summary);
        }
        preference.setPersistent(false);
        preference.setChecked(SmartKeyContract.ACTION_APP_PREFIX.equals(action)
                ? SmartKeyContract.isAppAction(selectedAction)
                : action.equals(selectedAction));
        preference.setOnClickListener(this);
        category.addPreference(preference);
        return preference;
    }

    private SelectorWithWidgetPreference addAction(PreferenceScreen screen, String action,
            @StringRes int title, @StringRes int summary, String selectedAction) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(
                requireContext());
        preference.setKey(action);
        preference.setTitle(title);
        if (summary != 0) {
            preference.setSummary(summary);
        }
        preference.setPersistent(false);
        preference.setChecked(action.equals(selectedAction));
        preference.setOnClickListener(this);
        screen.addPreference(preference);
        return preference;
    }

    private void showEmergencySetupDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.emergency_dialog_title)
                .setMessage(R.string.emergency_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.emergency_dialog_settings, (dialog, which) -> {
                    final Intent intent = new Intent(EMERGENCY_SETTINGS_ACTION);
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                })
                .show();
    }
}
