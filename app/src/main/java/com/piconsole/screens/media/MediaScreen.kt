package com.piconsole.screens.media

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piconsole.viewmodel.MediaViewModel

@Composable
fun MediaScreen(viewModel: MediaViewModel) {
    val mediaState by viewModel.mediaState.collectAsState()
    var volume by remember { mutableFloatStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mediaState?.currentTrack ?: "No Media Playing",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.sendMediaAction("prev") }) {
                Text("Prev")
            }
            FloatingActionButton(onClick = { viewModel.sendMediaAction("play") }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause")
            }
            Button(onClick = { viewModel.sendMediaAction("next") }) {
                Text("Next")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        
        Text("Volume")
        Slider(
            value = volume,
            onValueChange = { 
                volume = it 
                viewModel.sendMediaAction("volume", volume)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
