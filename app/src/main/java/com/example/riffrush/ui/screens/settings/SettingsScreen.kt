package com.example.riffrush.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.riffrush.R
import com.example.riffrush.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val darkMode by settingsViewModel.darkMode.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsCategory(title = stringResource(R.string.settings_category_general))

            var notificationsEnabled by remember { mutableStateOf(true) }
            ToggleSetting(
                title = stringResource(R.string.setting_notifications),
                subtitle = stringResource(R.string.setting_notifications_desc),
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            ToggleSetting(
                title = stringResource(R.string.setting_dark_mode),
                subtitle = stringResource(R.string.setting_dark_mode_desc),
                checked = darkMode,
                onCheckedChange = { settingsViewModel.setDarkMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory(title = stringResource(R.string.settings_category_audio))

            SettingItem(
                title = stringResource(R.string.setting_streaming_quality),
                subtitle = stringResource(R.string.setting_streaming_quality_desc),
                onClick = { /* Abrir diálogo de selección */ }
            )

            SettingItem(
                title = stringResource(R.string.setting_downloads),
                subtitle = stringResource(R.string.setting_downloads_desc),
                onClick = { /* Abrir diálogo */ }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory(title = stringResource(R.string.title_privacy))

            SettingItem(
                title = stringResource(R.string.setting_search_history),
                subtitle = stringResource(R.string.setting_search_history_desc),
                onClick = { /* Borrar */ }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "RiffRush v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
