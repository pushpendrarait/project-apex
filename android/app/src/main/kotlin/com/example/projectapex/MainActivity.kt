package com.example.projectapex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.projectapex.ui.screens.MainAppContainer
import com.example.projectapex.ui.theme.BgMidnight
import com.example.projectapex.ui.theme.ProjectApexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectApexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgMidnight
                ) {
                    MainAppContainer()
                }
            }
        }
    }
}
