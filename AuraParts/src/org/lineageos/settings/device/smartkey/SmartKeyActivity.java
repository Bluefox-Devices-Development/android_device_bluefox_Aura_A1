/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.lineageos.settings.device.R;

public final class SmartKeyActivity extends CollapsingToolbarBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_key);

        getSupportFragmentManager().addOnBackStackChangedListener(this::updateTitle);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SmartKeySettingsFragment())
                    .commit();
        }
        updateTitle();
    }

    void showActionPicker(String settingKey, @StringRes int gestureTitle) {
        showFragment(SmartKeyActionPickerFragment.newInstance(settingKey, gestureTitle));
    }

    void showAppPicker(String settingKey, @StringRes int gestureTitle) {
        showFragment(SmartKeyAppPickerFragment.newInstance(settingKey, gestureTitle));
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            finishAfterTransition();
        }
        return true;
    }

    private void updateTitle() {
        final Fragment fragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (fragment instanceof SmartKeyAppPickerFragment) {
            setTitle(R.string.choose_application_title);
        } else if (fragment instanceof SmartKeyActionPickerFragment) {
            setTitle(((SmartKeyActionPickerFragment) fragment).getGestureTitle());
        } else {
            setTitle(R.string.smart_key_title);
        }
    }
}
