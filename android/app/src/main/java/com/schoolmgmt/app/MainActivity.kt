package com.schoolmgmt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.schoolmgmt.app.ui.navigation.SchoolManagementNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity app — all screens are Composables navigated via
 * SchoolManagementNavHost. @AndroidEntryPoint lets this Activity (and
 * everything composed inside it) participate in Hilt's dependency graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolManagementTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SchoolManagementNavHost()
                }
            }
        }
    }
}

@Composable
fun SchoolManagementTheme(content: @Composable () -> Unit) {
    // Minimal Material3 theme using dynamic defaults. Replace with a
    // proper color scheme / typography pass when you have branding
    // (school colors/logo) to work from — see /mnt/skills frontend-design
    // guidance if you want help making this feel distinctive rather than
    // a default Material template.
    MaterialTheme(content = content)
}
