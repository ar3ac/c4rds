package io.github.ar3ac.c4rds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import io.github.ar3ac.c4rds.ui.navigation.NavGraph
import io.github.ar3ac.c4rds.ui.theme.MyCardsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            io.github.ar3ac.c4rds.utils.CloudBrandManager.sync(applicationContext)
        }

        setContent {
            MyCardsTheme {
                val navController = rememberNavController()
                val app = application as MyCardsApplication

                androidx.compose.runtime.LaunchedEffect(intent) {
                    if (intent?.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
                        val uri = intent.data.toString()
                        if (uri.startsWith("content://") || uri.endsWith(".c4rds")) {
                            navController.navigate(io.github.ar3ac.c4rds.ui.navigation.Screen.Settings.createRoute(uri))
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
}
