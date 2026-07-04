package com.prolocity.patchtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
        enableEdgeToEdge()
        setContent {
            PatchTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PatchTrackerNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
