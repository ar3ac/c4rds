package com.example.mycards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.mycards.ui.navigation.NavGraph
import com.example.mycards.ui.theme.MyCardsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyCardsTheme {
                val navController = rememberNavController()
                val app = application as MyCardsApplication

                NavGraph(
                    navController = navController,
                    application = app
                )
            }
        }
    }
}
