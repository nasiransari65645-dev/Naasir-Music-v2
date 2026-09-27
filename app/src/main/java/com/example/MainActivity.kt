package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
  private val musicViewModel: MusicViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.audio.MediaNotificationManager.createNotificationChannel(this)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    enableEdgeToEdge()
    setContent {
      val appThemeMode by musicViewModel.appThemeMode.collectAsStateWithLifecycle()
      val uiState by musicViewModel.uiState.collectAsStateWithLifecycle()
      val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
      val isDarkTheme = when (appThemeMode) {
        com.example.model.AppThemeMode.DARK_OLED -> true
        com.example.model.AppThemeMode.LIGHT_WHITE -> false
        com.example.model.AppThemeMode.SYSTEM_DEFAULT -> isSystemDark
      }

      MyApplicationTheme(
        naturalTheme = uiState.selectedTheme
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          var isLoading by remember { mutableStateOf(true) }

          Box(modifier = Modifier.fillMaxSize()) {
            MainScreen(viewModel = musicViewModel)

            AnimatedVisibility(
              visible = isLoading,
              enter = fadeIn(animationSpec = tween(200)),
              exit = fadeOut(animationSpec = tween(300))
            ) {
              LoadingScreen(
                onLoadingComplete = { isLoading = false }
              )
            }
          }
        }
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> {
        val intercepted = musicViewModel.onVolumeKeyChanged(isUp = true)
        if (intercepted) true else super.onKeyDown(keyCode, event)
      }
      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        val intercepted = musicViewModel.onVolumeKeyChanged(isUp = false)
        if (intercepted) true else super.onKeyDown(keyCode, event)
      }
      else -> super.onKeyDown(keyCode, event)
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Naasir Music") }
}

