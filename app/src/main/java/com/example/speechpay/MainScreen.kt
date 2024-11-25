package com.example.speechpay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    isVoiceCommandListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    recognizedText: String,
    messageText: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { if (!isVoiceCommandListening) onStartListening() else onStopListening() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isVoiceCommandListening) "Stop Listening" else "Start Listening")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Распознанный текст: $recognizedText",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.onSurface)
                .padding(16.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Текст сообщения: $messageText",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            modifier = Modifier
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.onSurface)
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Visible
        )

    }
}
