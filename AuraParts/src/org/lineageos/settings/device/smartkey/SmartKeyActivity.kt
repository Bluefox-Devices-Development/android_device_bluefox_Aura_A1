/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.settings.device.R

private const val SCREEN_MAIN = 0
private const val SCREEN_ACTION = 1
private const val SCREEN_APP = 2

class SmartKeyActivity : ComponentActivity() {
  private var resumeRevision by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SettingsTheme {
        SmartKeyApp(
            resumeRevision = resumeRevision,
            onFinish = ::finishAfterTransition,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    resumeRevision++
  }
}

@Composable
private fun SmartKeyApp(
    resumeRevision: Int,
    onFinish: () -> Unit,
) {
  var screen by rememberSaveable { mutableIntStateOf(SCREEN_MAIN) }
  var settingKey by rememberSaveable { mutableStateOf(SmartKeyContract.SETTING_SINGLE) }
  var gestureTitle by rememberSaveable { mutableIntStateOf(R.string.gesture_single) }

  val currentBackAction =
      rememberUpdatedState<() -> Unit>(
          newValue = {
            when (screen) {
              SCREEN_APP -> screen = SCREEN_ACTION
              SCREEN_ACTION -> screen = SCREEN_MAIN
              else -> onFinish()
            }
          }
      )
  val screenNavController = remember {
    object : NavControllerWrapper {
      override fun navigate(route: String, popUpCurrent: Boolean) = Unit

      override fun navigateBack() = currentBackAction.value.invoke()
    }
  }

  CompositionLocalProvider(LocalNavController provides screenNavController) {
    BackHandler(enabled = screen != SCREEN_MAIN) { currentBackAction.value.invoke() }
    when (screen) {
      SCREEN_ACTION ->
          SmartKeyActionPickerScreen(
              settingKey = settingKey,
              gestureTitle = gestureTitle,
              resumeRevision = resumeRevision,
              onChooseApplication = { screen = SCREEN_APP },
          )
      SCREEN_APP ->
          SmartKeyAppPickerScreen(
              settingKey = settingKey,
              resumeRevision = resumeRevision,
              onApplicationSelected = { screen = SCREEN_ACTION },
          )
      else ->
          SmartKeyMainScreen(
              resumeRevision = resumeRevision,
              onGestureSelected = { key, title ->
                settingKey = key
                gestureTitle = title
                screen = SCREEN_ACTION
              },
          )
    }
  }
}
