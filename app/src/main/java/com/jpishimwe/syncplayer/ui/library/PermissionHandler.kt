package com.jpishimwe.syncplayer.ui.library

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class PermissionState {
    Granted,
    Denied,
    NotRequested,
    PermanentlyDenied,
}

@Composable
fun PermissionHandler(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionState by rememberSaveable {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                PermissionState.Granted
            } else {
                PermissionState.NotRequested
            },
        )
    }

    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            permissionState =
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionState.Granted
                } else {
                    PermissionState.PermanentlyDenied
                }
        }

    val activity = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            permissionState =
                if (granted) {
                    PermissionState.Granted
                } else {
                    val shouldShowRationale =
                        activity.let {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                it as Activity,
                                Manifest.permission.READ_MEDIA_AUDIO,
                            )
                        }

                    if (shouldShowRationale) {
                        PermissionState.Denied
                    } else {
                        PermissionState.PermanentlyDenied
                    }
                }
        }

    when (permissionState) {
        PermissionState.Granted -> {
            onPermissionGranted()
        }

        PermissionState.NotRequested -> {
            NotRequestedUI(onGrantClick = {
                launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            })

            LaunchedEffect(Unit) {
                launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        PermissionState.Denied -> {
            DeniedUI(onGrantClick = {
                launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            })
        }

        PermissionState.PermanentlyDenied -> {
            PermanentlyDeniedUI(onOpenSettingsClick = {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                settingsLauncher.launch(intent)
            })
        }
    }
}

@Composable
private fun NotRequestedUI(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Please grant permission to access audio files")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGrantClick) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun DeniedUI(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Music Library Access Required",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text =
                    "SyncPlayer scans your device for music files to build your library. " +
                        "Without this permission, the app cannot function.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGrantClick) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun PermanentlyDeniedUI(onOpenSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Music Library Access Required",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text =
                    "You've denied permission to access your music library. " +
                        "To use SyncPlayer, please enable the permission in your device settings.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onOpenSettingsClick) {
                Text("Open settings")
            }
        }
    }
}
