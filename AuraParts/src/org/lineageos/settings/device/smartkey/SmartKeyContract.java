/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.content.ComponentName;

import java.util.Set;

/** Shared storage contract between the settings UI and the system-server key handler. */
public final class SmartKeyContract {
    public static final String SETTING_SINGLE = "smart_key_single_action";
    public static final String SETTING_DOUBLE = "smart_key_double_action";
    public static final String SETTING_LONG = "smart_key_long_action";

    public static final String ACTION_NONE = "none";
    public static final String ACTION_RECORDER = "recorder";
    public static final String ACTION_FLASHLIGHT = "flashlight";
    public static final String ACTION_SCREENSHOT = "screenshot";
    public static final String ACTION_CAMERA = "camera";
    public static final String ACTION_EMERGENCY = "emergency";
    public static final String ACTION_MUTE_RING = "mute_ring";
    public static final String ACTION_APP_PREFIX = "app:";

    public static final Set<String> BUILT_IN_ACTIONS = Set.of(
            ACTION_NONE,
            ACTION_RECORDER,
            ACTION_FLASHLIGHT,
            ACTION_SCREENSHOT,
            ACTION_CAMERA,
            ACTION_EMERGENCY,
            ACTION_MUTE_RING);

    private SmartKeyContract() {}

    public static boolean isSettingKey(String key) {
        return SETTING_SINGLE.equals(key)
                || SETTING_DOUBLE.equals(key)
                || SETTING_LONG.equals(key);
    }

    public static boolean isAppAction(String action) {
        return action != null && action.startsWith(ACTION_APP_PREFIX);
    }

    public static ComponentName getAppComponent(String action) {
        if (!isAppAction(action)) {
            return null;
        }
        return ComponentName.unflattenFromString(action.substring(ACTION_APP_PREFIX.length()));
    }

    public static String forApp(ComponentName component) {
        return ACTION_APP_PREFIX + component.flattenToString();
    }

    public static boolean isKnownAction(String action) {
        return action != null
                && (BUILT_IN_ACTIONS.contains(action) || getAppComponent(action) != null);
    }
}
