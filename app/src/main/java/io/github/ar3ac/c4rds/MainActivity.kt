package io.github.ar3ac.c4rds

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import io.github.ar3ac.c4rds.ui.navigation.NavGraph
import io.github.ar3ac.c4rds.ui.navigation.Screen
import io.github.ar3ac.c4rds.ui.theme.MyCardsTheme
import io.github.ar3ac.c4rds.utils.CloudBrandManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var activeIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeIntent = intent

        lifecycleScope.launch {
            CloudBrandManager.sync(applicationContext)
        }

        setContent {
            MyCardsTheme {
                val navController = rememberNavController()
                val app = application as MyCardsApplication

                LaunchedEffect(activeIntent) {
                    val current = activeIntent
                    if (current?.action == Intent.ACTION_VIEW && current.data != null) {
                        val uri = current.data.toString()
                        if (uri.startsWith("content://") || uri.endsWith(".c4rds")) {
                            navController.navigate(Screen.Settings.createRoute(uri))
                        }
                    }
                }

                NavGraph(
                    navController = navController,
                    application = app
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeIntent = intent
    }
}
