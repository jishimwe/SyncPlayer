package com.jpishimwe.syncplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.jpishimwe.syncplayer.data.sync.SyncOrchestrator
import com.jpishimwe.syncplayer.ui.SyncPlayerApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncPlayerApp()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            syncOrchestrator.syncIfSignedIn()
        }
    }
}
