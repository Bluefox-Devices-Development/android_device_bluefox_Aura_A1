/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ScreenshotHelper;

import java.util.List;

/** Handles the Aura A1 GPIO smart key before the generic CAMERA-key policy. */
public final class SmartKeyHandler implements DeviceKeyHandler {
    private static final String TAG = "AuraSmartKey";

    private static final long GESTURE_TIMEOUT_MILLIS = 400;
    private static final long WAKE_LOCK_TIMEOUT_MILLIS = 1_500;
    private static final int SMART_KEY_SCAN_CODE = 212;
    private static final String SMART_KEY_INPUT_DEVICE = "gpio_keys";
    private static final String PREFERRED_EMERGENCY_PACKAGE =
            "com.google.android.apps.safetyhub";
    private static final String ACTION_LAUNCH_EMERGENCY =
            "com.android.systemui.action.LAUNCH_EMERGENCY";
    private static final String ACTION_SCREEN_CAMERA_GESTURE =
            "lineageos.intent.action.SCREEN_CAMERA_GESTURE";

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mWakeLock;
    private final AudioManager mAudioManager;
    private final CameraManager mCameraManager;
    private final ScreenshotHelper mScreenshotHelper;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mKeyDown;
    private boolean mLongPressTriggered;
    private int mClickCount;
    private long mFirstKeyUpTime;
    private int mActiveDeviceId = -1;
    private long mActiveDownTime;
    private int mGestureUserId = UserHandle.USER_NULL;
    private int mPendingClickUserId = UserHandle.USER_NULL;

    private String mRearCameraId;
    private boolean mTorchEnabled;

    private final Runnable mSinglePressRunnable = () -> {
        if (mClickCount != 1) {
            return;
        }
        mClickCount = 0;
        final int userId = mPendingClickUserId;
        mPendingClickUserId = UserHandle.USER_NULL;
        try {
            performAction(readAction(SmartKeyContract.SETTING_SINGLE, userId), userId);
        } finally {
            releaseWakeLock();
        }
    };

    private final Runnable mLongPressRunnable = () -> {
        if (!mKeyDown) {
            return;
        }

        mLongPressTriggered = true;
        mClickCount = 0;
        mPendingClickUserId = UserHandle.USER_NULL;
        mHandler.removeCallbacks(mSinglePressRunnable);
        final int userId = mGestureUserId;
        try {
            performAction(readAction(SmartKeyContract.SETTING_LONG, userId), userId);
        } finally {
            releaseWakeLock();
        }
    };

    public SmartKeyHandler(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mPowerManager = context.getSystemService(PowerManager.class);
        mAudioManager = context.getSystemService(AudioManager.class);
        mCameraManager = context.getSystemService(CameraManager.class);
        ScreenshotHelper screenshotHelper = null;
        try {
            screenshotHelper = new ScreenshotHelper(context);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to initialize screenshot helper", e);
        }
        mScreenshotHelper = screenshotHelper;

        PowerManager.WakeLock wakeLock = null;
        if (mPowerManager != null) {
            try {
                wakeLock = mPowerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "AuraParts:SmartKey");
                wakeLock.setReferenceCounted(false);
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to initialize smart-key wake lock", e);
            }
        }
        mWakeLock = wakeLock;

        if (mCameraManager != null) {
            findRearFlashCamera();
            try {
                mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                        if (cameraId.equals(mRearCameraId)) {
                            mTorchEnabled = enabled;
                        }
                    }

                    @Override
                    public void onTorchModeUnavailable(String cameraId) {
                        if (cameraId.equals(mRearCameraId)) {
                            mTorchEnabled = false;
                        }
                    }
                }, mHandler);
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to register flashlight callback", e);
            }
        }
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        final InputDevice device = event.getDevice();
        if (event.getKeyCode() != KeyEvent.KEYCODE_CAMERA
                || event.getScanCode() != SMART_KEY_SCAN_CODE
                || event.getDeviceId() < 0
                || device == null
                || device.isExternal()
                || !SMART_KEY_INPUT_DEVICE.equals(device.getName())
                || (event.getSource() & InputDevice.SOURCE_CLASS_BUTTON) == 0) {
            return event;
        }

        final int action = event.getAction();
        final int repeatCount = event.getRepeatCount();
        final long eventTime = event.getEventTime();
        final long downTime = event.getDownTime();
        final int deviceId = event.getDeviceId();
        final boolean canceled = event.isCanceled();
        if (action == KeyEvent.ACTION_DOWN && repeatCount == 0) {
            holdWakeLock();
        }
        mHandler.post(() -> {
            try {
                handleSmartKeyEvent(
                        action, repeatCount, eventTime, downTime, deviceId, canceled);
            } catch (RuntimeException e) {
                // PhoneWindowManager's dispatch guard only covers this method's synchronous
                // return. Never let an asynchronous smart-key failure escape the main looper.
                Log.e(TAG, "Unable to process smart-key event", e);
                resetGestureSafely();
            }
        });

        // Consume both DOWN and UP so the generic CAMERA-key policy never races this handler.
        return null;
    }

    private void handleSmartKeyEvent(int action, int repeatCount, long eventTime,
            long downTime, int deviceId, boolean canceled) {
        if (canceled) {
            resetGesture(true);
            return;
        }

        if (action == KeyEvent.ACTION_DOWN) {
            if (repeatCount != 0 || mKeyDown) {
                return;
            }
            final int currentUserId = ActivityManager.getCurrentUser();
            if (mClickCount == 1 && currentUserId != mPendingClickUserId) {
                mClickCount = 0;
                mPendingClickUserId = UserHandle.USER_NULL;
                mHandler.removeCallbacks(mSinglePressRunnable);
            }
            if (mClickCount == 1
                    && eventTime - mFirstKeyUpTime < GESTURE_TIMEOUT_MILLIS) {
                // A possible second click has started. Do not let the pending single-click
                // runnable fire while the key is held; UP decides double-click vs. long press.
                mHandler.removeCallbacks(mSinglePressRunnable);
            }
            mKeyDown = true;
            mLongPressTriggered = false;
            mActiveDeviceId = deviceId;
            mActiveDownTime = downTime;
            mGestureUserId = currentUserId;
            mHandler.postAtTime(mLongPressRunnable,
                    downTime + GESTURE_TIMEOUT_MILLIS);
            return;
        }

        if (action != KeyEvent.ACTION_UP || !mKeyDown) {
            return;
        }
        if (deviceId != mActiveDeviceId || downTime != mActiveDownTime) {
            resetGesture(true);
            return;
        }

        mKeyDown = false;
        mHandler.removeCallbacks(mLongPressRunnable);
        if (mLongPressTriggered) {
            mLongPressTriggered = false;
            mClickCount = 0;
            mPendingClickUserId = UserHandle.USER_NULL;
            releaseWakeLock();
            return;
        }

        if (mClickCount == 1) {
            if (eventTime - mFirstKeyUpTime < GESTURE_TIMEOUT_MILLIS) {
                mHandler.removeCallbacks(mSinglePressRunnable);
                mClickCount = 0;
                mPendingClickUserId = UserHandle.USER_NULL;
                final int userId = mGestureUserId;
                try {
                    performAction(readAction(SmartKeyContract.SETTING_DOUBLE, userId), userId);
                } finally {
                    releaseWakeLock();
                }
                return;
            }

            // The main looper was busy past the timeout. Complete the first click before
            // treating this release as a new sequence.
            mHandler.removeCallbacks(mSinglePressRunnable);
            mClickCount = 0;
            final int firstClickUserId = mPendingClickUserId;
            mPendingClickUserId = UserHandle.USER_NULL;
            try {
                performAction(readAction(
                        SmartKeyContract.SETTING_SINGLE, firstClickUserId), firstClickUserId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to complete delayed smart-key single press", e);
            }
        }

        mClickCount = 1;
        mFirstKeyUpTime = eventTime;
        mPendingClickUserId = mGestureUserId;
        mHandler.postDelayed(mSinglePressRunnable, GESTURE_TIMEOUT_MILLIS);
    }

    private String readAction(String settingKey, int userId) {
        if (userId == UserHandle.USER_NULL) {
            return SmartKeyContract.ACTION_NONE;
        }
        try {
            final String action = Settings.Secure.getStringForUser(
                    mContext.getContentResolver(), settingKey, userId);
            return SmartKeyContract.isKnownAction(action)
                    ? action : SmartKeyContract.ACTION_NONE;
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to read smart-key setting " + settingKey, e);
            return SmartKeyContract.ACTION_NONE;
        }
    }

    private void performAction(String action, int userId) {
        try {
            if (userId == UserHandle.USER_NULL
                    || userId != ActivityManager.getCurrentUser()
                    || SmartKeyContract.ACTION_NONE.equals(action)
                    || !hasSetupCompleted(userId)) {
                return;
            }
            if (mPowerManager != null && !mPowerManager.isInteractive()
                    && !isAllowedWhileScreenOff(action)) {
                return;
            }

            switch (action) {
                case SmartKeyContract.ACTION_RECORDER:
                    launchSystemActivity(
                            new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION), userId);
                    break;
                case SmartKeyContract.ACTION_FLASHLIGHT:
                    toggleFlashlight();
                    break;
                case SmartKeyContract.ACTION_SCREENSHOT:
                    if (mScreenshotHelper != null) {
                        mScreenshotHelper.takeScreenshot(
                                WindowManager.TAKE_SCREENSHOT_FULLSCREEN,
                                WindowManager.ScreenshotSource.SCREENSHOT_VENDOR_GESTURE,
                                mHandler,
                                null);
                    }
                    break;
                case SmartKeyContract.ACTION_CAMERA:
                    mContext.sendBroadcastAsUser(
                            new Intent(ACTION_SCREEN_CAMERA_GESTURE),
                            UserHandle.CURRENT,
                            Manifest.permission.STATUS_BAR_SERVICE);
                    break;
                case SmartKeyContract.ACTION_EMERGENCY:
                    launchEmergency(userId);
                    break;
                case SmartKeyContract.ACTION_MUTE_RING:
                    toggleMuteRing();
                    break;
                default:
                    if (SmartKeyContract.isAppAction(action)) {
                        launchSelectedApp(action, userId);
                    }
                    break;
            }
        } catch (RuntimeException e) {
            // This code runs on system_server's main looper; action failures must be contained.
            Log.e(TAG, "Unable to perform smart-key action " + action, e);
        }
    }

    private boolean hasSetupCompleted(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0, userId) != 0;
    }

    private static boolean isAllowedWhileScreenOff(String action) {
        return SmartKeyContract.ACTION_FLASHLIGHT.equals(action)
                || SmartKeyContract.ACTION_EMERGENCY.equals(action)
                || SmartKeyContract.ACTION_MUTE_RING.equals(action);
    }

    private void findRearFlashCamera() {
        if (mCameraManager == null) {
            return;
        }
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                        mCameraManager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                final Boolean hasFlash = characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK
                        && Boolean.TRUE.equals(hasFlash)) {
                    mRearCameraId = cameraId;
                    return;
                }
            }
        } catch (CameraAccessException e) {
            Log.w(TAG, "Unable to find rear flash camera", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to inspect camera characteristics", e);
        }
    }

    private void toggleFlashlight() {
        if (mCameraManager == null) {
            return;
        }
        if (mRearCameraId == null) {
            findRearFlashCamera();
        }
        if (mRearCameraId == null) {
            Log.w(TAG, "No rear flash camera is available");
            return;
        }
        try {
            mCameraManager.setTorchMode(mRearCameraId, !mTorchEnabled);
        } catch (CameraAccessException | SecurityException e) {
            Log.w(TAG, "Unable to toggle flashlight", e);
        }
    }

    private void toggleMuteRing() {
        if (mAudioManager == null) {
            return;
        }
        final int mode = mAudioManager.getRingerModeInternal();
        mAudioManager.setRingerModeInternal(mode == AudioManager.RINGER_MODE_NORMAL
                ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
    }

    private void launchEmergency(int userId) {
        if (Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.EMERGENCY_GESTURE_ENABLED, 0, userId) != 1) {
            return;
        }
        final Intent intent = new Intent(ACTION_LAUNCH_EMERGENCY);
        final List<ResolveInfo> candidates = mPackageManager.queryIntentActivitiesAsUser(
                intent, PackageManager.MATCH_SYSTEM_ONLY, userId);
        ResolveInfo selected = null;
        for (ResolveInfo candidate : candidates) {
            if (!isLaunchable(candidate)) {
                continue;
            }
            if (selected == null) {
                selected = candidate;
            }
            if (PREFERRED_EMERGENCY_PACKAGE.equals(candidate.activityInfo.packageName)) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            Log.w(TAG, "No exported system activity handles Emergency SOS");
            return;
        }
        intent.setComponent(new ComponentName(selected.activityInfo.packageName,
                selected.activityInfo.name));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.of(userId));
    }

    private void launchSystemActivity(Intent intent, int userId) {
        final ResolveInfo resolveInfo = mPackageManager.resolveActivityAsUser(
                intent, PackageManager.MATCH_SYSTEM_ONLY, userId);
        if (!isLaunchable(resolveInfo)) {
            Log.w(TAG, "No exported system activity handles " + intent.getAction());
            return;
        }
        intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.of(userId));
    }

    private void launchSelectedApp(String action, int userId) {
        final ComponentName component = SmartKeyContract.getAppComponent(action);
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        final ResolveInfo resolveInfo = mPackageManager.resolveActivityAsUser(intent, 0, userId);
        if (!isLaunchable(resolveInfo)) {
            clearRemovedAppAction(action, userId);
            Log.w(TAG, "Selected smart-key app is no longer launchable: " + component);
            return;
        }
        mContext.startActivityAsUser(intent, UserHandle.of(userId));
    }

    private void clearRemovedAppAction(String action, int userId) {
        for (String settingKey : new String[] {
                SmartKeyContract.SETTING_SINGLE,
                SmartKeyContract.SETTING_DOUBLE,
                SmartKeyContract.SETTING_LONG}) {
            if (action.equals(Settings.Secure.getStringForUser(
                    mContext.getContentResolver(), settingKey, userId))) {
                Settings.Secure.putStringForUser(mContext.getContentResolver(),
                        settingKey, SmartKeyContract.ACTION_NONE, userId);
            }
        }
    }

    private static boolean isLaunchable(ResolveInfo resolveInfo) {
        final ActivityInfo activityInfo = resolveInfo == null ? null : resolveInfo.activityInfo;
        return activityInfo != null && activityInfo.exported && activityInfo.enabled;
    }

    private void holdWakeLock() {
        if (mWakeLock == null) {
            return;
        }
        try {
            mWakeLock.acquire(WAKE_LOCK_TIMEOUT_MILLIS);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to acquire smart-key wake lock", e);
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock == null) {
            return;
        }
        try {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to release smart-key wake lock", e);
        }
    }

    private void resetGesture(boolean cancelPendingClick) {
        mKeyDown = false;
        mLongPressTriggered = false;
        mActiveDeviceId = -1;
        mActiveDownTime = 0;
        mHandler.removeCallbacks(mLongPressRunnable);
        if (cancelPendingClick) {
            mClickCount = 0;
            mPendingClickUserId = UserHandle.USER_NULL;
            mHandler.removeCallbacks(mSinglePressRunnable);
        }
        releaseWakeLock();
    }

    private void resetGestureSafely() {
        try {
            resetGesture(true);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to reset smart-key gesture", e);
            releaseWakeLock();
        }
    }
}
