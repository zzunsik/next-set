package io.github.zzunsik.nextset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.zzunsik.nextset.ui.navigation.NextSetNavDisplay
import io.github.zzunsik.nextset.ui.theme.NextSetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NextSetApplication).container
        setContent {
            NextSetTheme {
                NextSetNavDisplay(exerciseRepository = container.exerciseRepository)
            }
        }
    }
}
