package com.piconsole.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piconsole.viewmodel.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel) {
    val output by viewModel.output.collectAsState()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Connect when screen opens
    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    // Scroll to bottom when output changes
    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Console", color = Color(0xFF4CAF50)) },
                actions = {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Terminal", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Terminal Output Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Strip ANSI escape sequences for cleaner text display
                val cleanOutput = output.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "")
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true
                ) {
                    item {
                        Text(
                            text = cleanOutput,
                            color = Color(0xFFE0E0E0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Modifier Keys Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModifierButton("Ctrl+C") { viewModel.sendControlCharacter("\u0003") }
                ModifierButton("Tab") { viewModel.sendControlCharacter("\t") }
                ModifierButton("Esc") { viewModel.sendControlCharacter("\u001B") }
                IconButton(onClick = { viewModel.sendControlCharacter("\u001B[A") }) { // Up Arrow
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color.White)
                }
                IconButton(onClick = { viewModel.sendControlCharacter("\u001B[B") }) { // Down Arrow
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color.White)
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$~ ",
                    color = Color(0xFF4CAF50),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Command...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color(0xFF4CAF50)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (currentInput.isNotEmpty()) {
                            viewModel.sendCommand(currentInput)
                            currentInput = ""
                        }
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(
                    onClick = {
                        if (currentInput.isNotEmpty()) {
                            viewModel.sendCommand(currentInput)
                            currentInput = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
fun ModifierButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
