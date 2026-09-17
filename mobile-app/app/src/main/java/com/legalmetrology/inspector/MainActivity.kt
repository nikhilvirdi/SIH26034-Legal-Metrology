package com.legalmetrology.inspector

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.legalmetrology.inspector.ui.navigation.AppNavGraph
import com.legalmetrology.inspector.ui.theme.LegalMetrologyTheme
import dagger.hilt.android.AndroidEntryPoint
import org.opencv.android.OpenCVLoader

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super.onCreate
        // Themed with @style/Theme.LegalMetrology.SplashScreen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize OpenCV for ArUco marker detection
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Unable to load OpenCV")
        } else {
            Log.d(TAG, "OpenCV loaded successfully")
        }

        setContent {
            LegalMetrologyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
