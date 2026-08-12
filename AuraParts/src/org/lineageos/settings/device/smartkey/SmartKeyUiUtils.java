/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;

import org.lineageos.settings.device.R;

final class SmartKeyUiUtils {
    private SmartKeyUiUtils() {}

    static String getAction(Context context, String settingKey) {
        if (!SmartKeyContract.isSettingKey(settingKey)) {
            return SmartKeyContract.ACTION_NONE;
        }
        final int userId = UserHandle.myUserId();
        final String action = Settings.Secure.getStringForUser(
                context.getContentResolver(), settingKey, userId);
        if (!SmartKeyContract.isKnownAction(action)
                || (SmartKeyContract.isAppAction(action)
                        && resolveSelectedApp(context, action) == null)) {
            putAction(context, settingKey, SmartKeyContract.ACTION_NONE);
            return SmartKeyContract.ACTION_NONE;
        }
        return action;
    }

    static void putAction(Context context, String settingKey, String action) {
        if (!SmartKeyContract.isSettingKey(settingKey)
                || !SmartKeyContract.isKnownAction(action)) {
            return;
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), settingKey, action,
                UserHandle.myUserId());
    }

    static CharSequence getActionLabel(Context context, String action) {
        switch (action) {
            case SmartKeyContract.ACTION_RECORDER:
                return context.getText(R.string.action_recorder);
            case SmartKeyContract.ACTION_FLASHLIGHT:
                return context.getText(R.string.action_flashlight);
            case SmartKeyContract.ACTION_SCREENSHOT:
                return context.getText(R.string.action_screenshot);
            case SmartKeyContract.ACTION_CAMERA:
                return context.getText(R.string.action_camera);
            case SmartKeyContract.ACTION_EMERGENCY:
                return context.getText(R.string.action_emergency);
            case SmartKeyContract.ACTION_MUTE_RING:
                return context.getText(R.string.action_mute_ring);
            case SmartKeyContract.ACTION_NONE:
                return context.getText(R.string.action_none);
            default:
                final ResolveInfo resolveInfo = resolveSelectedApp(context, action);
                return resolveInfo == null
                        ? context.getText(R.string.action_none)
                        : resolveInfo.loadLabel(context.getPackageManager());
        }
    }

    static ResolveInfo resolveSelectedApp(Context context, String action) {
        final ComponentName component = SmartKeyContract.getAppComponent(action);
        if (component == null) {
            return null;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component);
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivityAsUser(
                intent, 0, UserHandle.myUserId());
        final ActivityInfo activityInfo = resolveInfo == null ? null : resolveInfo.activityInfo;
        return activityInfo != null && activityInfo.exported && activityInfo.enabled
                ? resolveInfo : null;
    }

    static boolean isEmergencyEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.EMERGENCY_GESTURE_ENABLED, 0, UserHandle.myUserId()) == 1;
    }

    static boolean isAvailableWhileScreenOff(String action) {
        return SmartKeyContract.ACTION_FLASHLIGHT.equals(action)
                || SmartKeyContract.ACTION_EMERGENCY.equals(action)
                || SmartKeyContract.ACTION_MUTE_RING.equals(action);
    }
}
