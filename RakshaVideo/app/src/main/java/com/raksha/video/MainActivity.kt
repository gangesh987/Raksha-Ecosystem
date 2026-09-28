package com.raksha.video

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raksha.video.core.navigation.Routes
import com.raksha.video.core.theme.RakshaVideoTheme
import com.raksha.video.data.AppPreferences
import com.raksha.video.navigation.RakshaNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val darkTheme by preferences.darkTheme.collectAsState(initial = false)
            val loggedIn by preferences.loggedIn.collectAsState(initial = false)
            val onboardingSeen by preferences.onboardingSeen.collectAsState(initial = false)

            RakshaVideoTheme(darkTheme = darkTheme) {
                val start = when {
                    !loggedIn -> Routes.Auth
                    !onboardingSeen -> Routes.Permissions
                    else -> Routes.Home
                }
                RakshaNavHost(startDestination = start, preferences = preferences)
            }
        }
    }
}
