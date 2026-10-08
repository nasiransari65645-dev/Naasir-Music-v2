package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.audio.MediaContentObserver
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
  private var mediaContentObserver: MediaContentObserver? = null

  override fun onStart() {
    super.onStart()
    try {
      if (mediaContentObserver == null) {
        mediaContentObserver = MediaContentObserver(
          context = applicationContext,
          coroutineScope = lifecycleScope
        ) {
          musicViewModel.syncNewSongsSilently()
        }
        mediaContentObserver?.register()
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Error registering MediaContentObserver: ${e.message}", e)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      mediaContentObserver?.unregister()
    } catch (_: Exception) {}
    mediaContentObserver = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      com.example.audio.MediaNotificationManager.createNotificationChannel(this)
    } catch (_: Exception) {}
    try {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } catch (_: Exception) {}
    enableEdgeToEdge()
    if (intent?.getBooleanExtra(com.example.audio.FloatingPlayerService.EXTRA_OPEN_NOW_PLAYING, false) == true) {
      musicViewModel.selectTab(com.example.viewmodel.AppTab.NOW_PLAYING)
    } else {
      musicViewModel.selectTab(com.example.viewmodel.AppTab.ALL_SONGS)
    }

    setContent {
      val activeTheme = com.example.model.AppNaturalTheme.BLUE
      val isDarkTheme = true

      MyApplicationTheme(
        naturalTheme = activeTheme,
        isDarkTheme = isDarkTheme
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScreen(viewModel = musicViewModel)
        }
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    val isPlaying = musicViewModel.uiState.value.playerState.isPlaying
    val prefs = com.example.storage.SettingsPreferencesManager(this)
    val isFloatingEnabled = prefs.loadFloatingPlayerEnabled()
    val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      android.provider.Settings.canDrawOverlays(this)
    } else true

    if (isFloatingEnabled && hasOverlayPermission && isPlaying) {
      com.example.audio.FloatingPlayerService.start(this)
    }
  }

  override fun onResume() {
    super.onResume()
    // When returning to MainActivity, dismiss floating desktop player so there is no duplicate UI
    com.example.audio.FloatingPlayerService.stop(this)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra(com.example.audio.FloatingPlayerService.EXTRA_OPEN_NOW_PLAYING, false)) {
      musicViewModel.selectTab(com.example.viewmodel.AppTab.NOW_PLAYING)
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

