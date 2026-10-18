/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.settings.device.R

private const val EXTRA_PAGE = "org.lineageos.settings.device.smartkey.PAGE"
private const val EXTRA_SETTING_KEY = "org.lineageos.settings.device.smartkey.SETTING_KEY"
private const val PAGE_ACTION = "action"
private const val PAGE_APP = "app"

class SmartKeyActivity : ComponentActivity() {
  private var resumeRevision by mutableIntStateOf(0)

  private val settingKey: String
    get() =
        intent.getStringExtra(EXTRA_SETTING_KEY)
            ?.takeIf { SmartKeyContract.isSettingKey(it) }
            ?: SmartKeyContract.SETTING_SINGLE

  private val navController =
      object : NavControllerWrapper {
        override fun navigate(route: String, popUpCurrent: Boolean) {
          startPage(route)
          if (popUpCurrent) finish()
        }

        override fun navigateBack() {
          finish()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SettingsTheme {
        CompositionLocalProvider(LocalNavController provides navController) { SmartKeyPage() }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    resumeRevision++
  }

  private fun startPage(page: String, key: String = settingKey) {
    // Each page gets an activity instance so WindowManager owns its transitions.
    startActivity(
        Intent(this, SmartKeyActivity::class.java)
            .putExtra(EXTRA_PAGE, page)
            .putExtra(EXTRA_SETTING_KEY, key)
    )
  }

  @Composable
  private fun SmartKeyPage() {
    when (intent.getStringExtra(EXTRA_PAGE)) {
      PAGE_ACTION ->
          SmartKeyActionPickerScreen(
              settingKey = settingKey,
              gestureTitle =
                  when (settingKey) {
                    SmartKeyContract.SETTING_DOUBLE -> R.string.gesture_double
                    SmartKeyContract.SETTING_LONG -> R.string.gesture_long
                    else -> R.string.gesture_single
                  },
              resumeRevision = resumeRevision,
              onChooseApplication = { startPage(PAGE_APP) },
          )
      PAGE_APP ->
          SmartKeyAppPickerScreen(
              settingKey = settingKey,
              resumeRevision = resumeRevision,
              onApplicationSelected = { finish() },
          )
      else ->
          SmartKeyMainScreen(
              resumeRevision = resumeRevision,
              onGestureSelected = { key, _ -> startPage(PAGE_ACTION, key) },
          )
    }
  }
}
