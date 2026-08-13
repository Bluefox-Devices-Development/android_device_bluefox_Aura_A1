/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device.smartkey

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.SearchScaffold
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.LazyCategory
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lineageos.settings.device.R

private const val EMERGENCY_SETTINGS_ACTION = "android.settings.EMERGENCY_SETTINGS"

private data class ActionOption(
    val action: String,
    val title: Int,
    val summary: Int,
)

private data class AppEntry(
    val component: ComponentName,
    val label: CharSequence,
    val icon: Drawable,
)

private val QUICK_ACTIONS =
    listOf(
        ActionOption(
            SmartKeyContract.ACTION_FLASHLIGHT,
            R.string.action_flashlight,
            R.string.screen_off_available,
        ),
        ActionOption(
            SmartKeyContract.ACTION_CAMERA,
            R.string.action_camera,
            R.string.screen_on_only,
        ),
        ActionOption(
            SmartKeyContract.ACTION_SCREENSHOT,
            R.string.action_screenshot,
            R.string.screen_on_only,
        ),
        ActionOption(
            SmartKeyContract.ACTION_RECORDER,
            R.string.action_recorder,
            R.string.screen_on_only,
        ),
        ActionOption(
            SmartKeyContract.ACTION_MUTE_RING,
            R.string.action_mute_ring,
            R.string.screen_off_available,
        ),
    )

@Composable
internal fun SmartKeyMainScreen(
    resumeRevision: Int,
    onGestureSelected: (settingKey: String, gestureTitle: Int) -> Unit,
) {
  val context = LocalContext.current
  val singleAction =
      remember(resumeRevision) {
        SmartKeyUiUtils.getAction(context, SmartKeyContract.SETTING_SINGLE)
      }
  val doubleAction =
      remember(resumeRevision) {
        SmartKeyUiUtils.getAction(context, SmartKeyContract.SETTING_DOUBLE)
      }
  val longAction =
      remember(resumeRevision) { SmartKeyUiUtils.getAction(context, SmartKeyContract.SETTING_LONG) }

  SettingsScaffold(title = stringResource(R.string.smart_key_title)) { paddingValues ->
    Column(
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues),
    ) {
      Category(title = stringResource(R.string.gesture_category_title)) {
        SmartKeyPreference(
            title = stringResource(R.string.gesture_single),
            summary = SmartKeyUiUtils.getActionLabel(context, singleAction),
            clickAction = {
              onGestureSelected(
                  SmartKeyContract.SETTING_SINGLE,
                  R.string.gesture_single,
              )
            },
        )
        SmartKeyPreference(
            title = stringResource(R.string.gesture_double),
            summary = SmartKeyUiUtils.getActionLabel(context, doubleAction),
            clickAction = {
              onGestureSelected(
                  SmartKeyContract.SETTING_DOUBLE,
                  R.string.gesture_double,
              )
            },
        )
        SmartKeyPreference(
            title = stringResource(R.string.gesture_long),
            summary = SmartKeyUiUtils.getActionLabel(context, longAction),
            clickAction = {
              onGestureSelected(
                  SmartKeyContract.SETTING_LONG,
                  R.string.gesture_long,
              )
            },
        )
      }
      Category(title = stringResource(R.string.information_category_title)) {
        SmartKeyPreference(
            title = stringResource(R.string.double_press_delay_title),
            summary = stringResource(R.string.double_press_delay_summary),
        )
        SmartKeyPreference(
            title = stringResource(R.string.screen_off_title),
            summary = stringResource(R.string.screen_off_summary),
        )
      }
    }
  }
}

@Composable
internal fun SmartKeyActionPickerScreen(
    settingKey: String,
    gestureTitle: Int,
    resumeRevision: Int,
    onChooseApplication: () -> Unit,
) {
  val context = LocalContext.current
  var selectedAction by
      remember(settingKey, resumeRevision) {
        mutableStateOf(SmartKeyUiUtils.getAction(context, settingKey))
      }
  val emergencyEnabled = remember(resumeRevision) { SmartKeyUiUtils.isEmergencyEnabled(context) }
  var showEmergencyDialog by rememberSaveable { mutableStateOf(false) }

  fun selectAction(action: String) {
    SmartKeyUiUtils.putAction(context, settingKey, action)
    selectedAction = SmartKeyUiUtils.getAction(context, settingKey)
  }

  if (showEmergencyDialog) {
    EmergencySetupDialog(
        onDismiss = { showEmergencyDialog = false },
        onOpenSettings = {
          showEmergencyDialog = false
          openEmergencySettings(context)
        },
    )
  }

  SettingsScaffold(title = stringResource(gestureTitle)) { paddingValues ->
    Column(
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues),
    ) {
      Category(modifier = Modifier.selectableGroup()) {
        SmartKeyRadioPreference(
            title = stringResource(R.string.action_none),
            selected = selectedAction == SmartKeyContract.ACTION_NONE,
            onClick = { selectAction(SmartKeyContract.ACTION_NONE) },
        )
      }
      Category(
          title = stringResource(R.string.quick_actions_category_title),
          modifier = Modifier.selectableGroup(),
      ) {
        QUICK_ACTIONS.forEach { option ->
          SmartKeyRadioPreference(
              title = stringResource(option.title),
              summary = stringResource(option.summary),
              selected = selectedAction == option.action,
              onClick = { selectAction(option.action) },
          )
        }
        SmartKeyRadioPreference(
            title = stringResource(R.string.action_emergency),
            summary =
                stringResource(
                    if (emergencyEnabled) {
                      R.string.screen_off_available
                    } else {
                      R.string.emergency_setup_required
                    }
                ),
            selected = selectedAction == SmartKeyContract.ACTION_EMERGENCY,
            onClick = {
              if (emergencyEnabled) {
                selectAction(SmartKeyContract.ACTION_EMERGENCY)
              } else {
                showEmergencyDialog = true
              }
            },
        )
      }
      Category(
          title = stringResource(R.string.applications_category_title),
          modifier = Modifier.selectableGroup(),
      ) {
        SmartKeyRadioPreference(
            title = stringResource(R.string.action_open_app),
            summary =
                if (SmartKeyContract.isAppAction(selectedAction)) {
                  SmartKeyUiUtils.getActionLabel(context, selectedAction)
                } else {
                  stringResource(R.string.screen_on_only)
                },
            selected = SmartKeyContract.isAppAction(selectedAction),
            onClick = onChooseApplication,
        )
      }
    }
  }
}

@Composable
internal fun SmartKeyAppPickerScreen(
    settingKey: String,
    resumeRevision: Int,
    onApplicationSelected: () -> Unit,
) {
  val context = LocalContext.current
  val packageManager = context.packageManager
  val apps by
      produceState<List<AppEntry>?>(
          initialValue = null,
          packageManager,
          resumeRevision,
      ) {
        value = withContext(Dispatchers.IO) { loadLauncherApps(packageManager) }
      }
  val selectedComponent =
      remember(settingKey, resumeRevision) {
        SmartKeyContract.getAppComponent(SmartKeyUiUtils.getAction(context, settingKey))
      }

  SearchScaffold(title = stringResource(R.string.choose_application_title)) {
      bottomPadding,
      searchQuery ->
    val query = searchQuery().trim()
    val visibleApps = remember(apps, query) { filterApps(apps.orEmpty(), query) }
    when {
      apps == null ->
          Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
      visibleApps.isEmpty() ->
          Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
          ) {
            Text(text = stringResource(R.string.no_apps_found))
          }
      else ->
          LazyCategory(
              count = visibleApps.size,
              key = { index -> visibleApps[index].component.flattenToString() },
              bottomPadding = bottomPadding,
          ) { index ->
            val entry = visibleApps[index]
            SmartKeyAppPreference(
                entry = entry,
                selected = entry.component == selectedComponent,
                onClick = {
                  SmartKeyUiUtils.putAction(
                      context,
                      settingKey,
                      SmartKeyContract.forApp(entry.component),
                  )
                  onApplicationSelected()
                },
            )
          }
    }
  }
}

@Composable
private fun SmartKeyPreference(
    title: String,
    summary: CharSequence,
    clickAction: (() -> Unit)? = null,
) {
  val modelTitle = title
  val modelSummary = summary
  val modelClickAction = clickAction
  Preference(
      model =
          object : PreferenceModel {
            override val title = modelTitle
            override val summary = { modelSummary }
            override val onClick = modelClickAction
          },
  )
}

@Composable
private fun SmartKeyRadioPreference(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    summary: CharSequence = "",
) {
  ListItem(
      modifier =
          Modifier.fillMaxWidth()
              .selectable(
                  selected = selected,
                  onClick = onClick,
                  role = Role.RadioButton,
              ),
      headlineContent = {
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
      },
      supportingContent =
          if (summary.isNotEmpty()) {
            {
              Text(
                  text = summary.toString(),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
              )
            }
          } else {
            null
          },
      trailingContent = {
        RadioButton(
            selected = selected,
            onClick = null,
        )
      },
      colors =
          ListItemDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceBright,
          ),
  )
}

@Composable
private fun SmartKeyAppPreference(
    entry: AppEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
  ListItem(
      modifier =
          Modifier.fillMaxWidth()
              .selectable(
                  selected = selected,
                  onClick = onClick,
                  role = Role.RadioButton,
              ),
      headlineContent = {
        Text(
            text = entry.label.toString(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      },
      supportingContent = {
        Text(
            text = entry.component.packageName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
      },
      leadingContent = {
        Image(
            painter = rememberDrawablePainter(entry.icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
      },
      trailingContent = {
        RadioButton(
            selected = selected,
            onClick = null,
        )
      },
      colors =
          ListItemDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceBright,
          ),
  )
}

@Composable
private fun EmergencySetupDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = stringResource(R.string.emergency_dialog_title)) },
      text = { Text(text = stringResource(R.string.emergency_dialog_message)) },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
      },
      confirmButton = {
        TextButton(onClick = onOpenSettings) {
          Text(text = stringResource(R.string.emergency_dialog_settings))
        }
      },
  )
}

private fun openEmergencySettings(context: Context) {
  val intent = Intent(EMERGENCY_SETTINGS_ACTION)
  try {
    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    }
  } catch (_: RuntimeException) {
    // The emergency provider may disappear while its settings activity is resolving.
  }
}

private fun loadLauncherApps(packageManager: PackageManager): List<AppEntry> {
  val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val resolveInfos =
      try {
        packageManager
            .queryIntentActivitiesAsUser(
                launcherIntent,
                0,
                UserHandle.myUserId(),
            )
            .toMutableList()
      } catch (_: RuntimeException) {
        return emptyList()
      }
  resolveInfos.sortWith(ResolveInfo.DisplayNameComparator(packageManager))

  val seenComponents = mutableSetOf<ComponentName>()
  return buildList {
    resolveInfos.forEach { resolveInfo ->
      val activityInfo = resolveInfo.activityInfo ?: return@forEach
      if (!activityInfo.exported || !activityInfo.enabled) return@forEach
      val component = ComponentName(activityInfo.packageName, activityInfo.name)
      if (!seenComponents.add(component)) return@forEach
      try {
        add(
            AppEntry(
                component = component,
                label = resolveInfo.loadLabel(packageManager),
                icon = resolveInfo.loadIcon(packageManager),
            )
        )
      } catch (_: RuntimeException) {
        // Skip packages that changed while their labels or icons were loading.
      }
    }
  }
}

private fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
  if (query.isBlank()) return apps
  val normalizedQuery = query.lowercase(Locale.getDefault())
  return apps.filter { entry ->
    entry.label.toString().lowercase(Locale.getDefault()).contains(normalizedQuery) ||
        entry.component.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
  }
}
