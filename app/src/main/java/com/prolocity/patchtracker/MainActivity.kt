package com.prolocity.patchtracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.prolocity.patchtracker.ui.PatchTrackerViewModel
import com.prolocity.patchtracker.ui.PatchTrackerViewModelFactory
import com.prolocity.patchtracker.ui.navigation.PatchTrackerNavHost
import com.prolocity.patchtracker.ui.theme.PatchTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PatchTrackerViewModel by viewModels {
        PatchTrackerViewModelFactory((application as PatchTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            // Draw edge-to-edge with transparent system bars so the app bar's league-blue bleeds
            // under the status bar. The status bar sits over the top app bar, whose color inverts
            // by theme (dark blue in light theme, light blue in dark theme), so its icon contrast
            // is the opposite of the usual auto() behavior: light icons in light theme, dark icons
            // in dark theme. The navigation bar sits over the (surface-colored) NavigationBar, so
            // auto() — dark icons on light, light icons on dark — is correct there. Re-applied on
            // theme change so the icon tint tracks day/night switches.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }
            PatchTrackerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PatchTrackerNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
