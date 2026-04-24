package com.piconsole.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piconsole.ui.theme.PiConsoleTheme

@Composable
fun WheelTimePicker(
    items: List<String>,
    initialIndex: Int = 0,
    onItemSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Empty buffering ensures start/end bounds hit the center
    val bufferedItems = listOf("", "") + items + listOf("", "")

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onItemSelected(listState.firstVisibleItemIndex)
    }

    Box(
        modifier = Modifier
            .height(150.dp)
            .width(70.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(bufferedItems.size) { index ->
                val isCenter = index == listState.firstVisibleItemIndex + 2
                val itemAlpha = if (isCenter) 1f else 0.4f
                val style = if (isCenter) MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                else MaterialTheme.typography.titleMedium

                Box(
                    modifier = Modifier.height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bufferedItems[index],
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WheelTimePickerPreview() {
    PiConsoleTheme {
        Surface {
            WheelTimePicker(
                items = (0..59).map { it.toString().padStart(2, '0') },
                initialIndex = 0,
                onItemSelected = {}
            )
        }
    }
}
