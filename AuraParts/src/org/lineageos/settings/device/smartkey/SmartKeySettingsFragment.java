/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.lineageos.settings.device.R;

public final class SmartKeySettingsFragment extends PreferenceFragmentCompat {
    private Preference mSinglePress;
    private Preference mDoublePress;
    private Preference mLongPress;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.smart_key_settings, rootKey);

        mSinglePress = findPreference("smart_key_single");
        mDoublePress = findPreference("smart_key_double");
        mLongPress = findPreference("smart_key_long");

        mSinglePress.setOnPreferenceClickListener(preference -> {
            showActionPicker(SmartKeyContract.SETTING_SINGLE, R.string.gesture_single);
            return true;
        });
        mDoublePress.setOnPreferenceClickListener(preference -> {
            showActionPicker(SmartKeyContract.SETTING_DOUBLE, R.string.gesture_double);
            return true;
        });
        mLongPress.setOnPreferenceClickListener(preference -> {
            showActionPicker(SmartKeyContract.SETTING_LONG, R.string.gesture_long);
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mSinglePress.setSummary(SmartKeyUiUtils.getActionLabel(requireContext(),
                SmartKeyUiUtils.getAction(requireContext(), SmartKeyContract.SETTING_SINGLE)));
        mDoublePress.setSummary(SmartKeyUiUtils.getActionLabel(requireContext(),
                SmartKeyUiUtils.getAction(requireContext(), SmartKeyContract.SETTING_DOUBLE)));
        mLongPress.setSummary(SmartKeyUiUtils.getActionLabel(requireContext(),
                SmartKeyUiUtils.getAction(requireContext(), SmartKeyContract.SETTING_LONG)));
    }

    private void showActionPicker(String settingKey, int gestureTitle) {
        ((SmartKeyActivity) requireActivity()).showActionPicker(settingKey, gestureTitle);
    }
}
