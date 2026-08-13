/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.settingslib.spa.framework.compose.localNavController
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.settings.device.R

private const val ROUTE_MAIN = "main"
private const val ROUTE_ACTION = "action"
private const val ROUTE_APP = "app"

private const val FADE_OUT_MILLIS = 75
private const val FADE_IN_MILLIS = 300

private val slideInEffect =
    tween<IntOffset>(
        durationMillis = FADE_IN_MILLIS,
        delayMillis = FADE_OUT_MILLIS,
        easing = LinearOutSlowInEasing,
    )
private val slideOutEffect = tween<IntOffset>(durationMillis = FADE_IN_MILLIS)
private val fadeOutEffect =
    tween<Float>(
        durationMillis = FADE_OUT_MILLIS,
        easing = FastOutLinearInEasing,
    )
private val fadeInEffect =
    tween<Float>(
        durationMillis = FADE_IN_MILLIS,
        delayMillis = FADE_OUT_MILLIS,
        easing = LinearOutSlowInEasing,
    )
private val offsetFunc: (offsetForFullSlide: Int) -> Int = { it / 5 }

class SmartKeyActivity : ComponentActivity() {
  private var resumeRevision by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { SettingsTheme { SmartKeyApp(resumeRevision = resumeRevision) } }
  }

  override fun onResume() {
    super.onResume()
    resumeRevision++
  }
}

@Composable
private fun SmartKeyApp(
    resumeRevision: Int,
) {
  var settingKey by rememberSaveable { mutableStateOf(SmartKeyContract.SETTING_SINGLE) }
  var gestureTitle by rememberSaveable { mutableIntStateOf(R.string.gesture_single) }
  val navController = rememberNavController()

  CompositionLocalProvider(navController.localNavController()) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN,
        modifier = Modifier.fillMaxSize(),
    ) {
      smartKeyDestination(ROUTE_MAIN) {
        SmartKeyMainScreen(
            resumeRevision = resumeRevision,
            onGestureSelected = { key, title ->
              settingKey = key
              gestureTitle = title
              navController.navigate(ROUTE_ACTION) { launchSingleTop = true }
            },
        )
      }
      smartKeyDestination(ROUTE_ACTION) {
        SmartKeyActionPickerScreen(
            settingKey = settingKey,
            gestureTitle = gestureTitle,
            resumeRevision = resumeRevision,
            onChooseApplication = { navController.navigate(ROUTE_APP) { launchSingleTop = true } },
        )
      }
      smartKeyDestination(ROUTE_APP) {
        SmartKeyAppPickerScreen(
            settingKey = settingKey,
            resumeRevision = resumeRevision,
            onApplicationSelected = { navController.popBackStack() },
        )
      }
    }
  }
}

private fun NavGraphBuilder.smartKeyDestination(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        enterTransition = {
          slideIntoContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.Start,
              animationSpec = slideInEffect,
              initialOffset = offsetFunc,
          ) + fadeIn(animationSpec = fadeInEffect)
        },
        exitTransition = {
          slideOutOfContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.Start,
              animationSpec = slideOutEffect,
              targetOffset = offsetFunc,
          ) + fadeOut(animationSpec = fadeOutEffect)
        },
        popEnterTransition = {
          slideIntoContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.End,
              animationSpec = slideInEffect,
              initialOffset = offsetFunc,
          ) + fadeIn(animationSpec = fadeInEffect)
        },
        popExitTransition = {
          slideOutOfContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.End,
              animationSpec = slideOutEffect,
              targetOffset = offsetFunc,
          ) + fadeOut(animationSpec = fadeOutEffect)
        },
        content = content,
    )
