package com.sliide.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sliide.app.core.common.DomainError

@Composable
fun ErrorScreen(
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, subtitle) = errorInfo(error)

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onRetry) { Text("Retry") }
    }
}

private fun errorInfo(error: DomainError): Triple<ImageVector, String, String> = when (error) {
    is DomainError.NetworkUnavailable -> Triple(
        Icons.Filled.Warning, "No Internet Connection", "Check your connection and try again"
    )
    is DomainError.Timeout -> Triple(
        Icons.Filled.Warning, "Request Timed Out", "The server took too long to respond"
    )
    is DomainError.Unauthorized -> Triple(
        Icons.Filled.Warning, "Unauthorized", "Please check your API token"
    )
    is DomainError.RateLimited -> Triple(
        Icons.Filled.Warning, "Too Many Requests", "Please wait a moment and try again"
    )
    else -> Triple(
        Icons.Filled.Warning, "Something Went Wrong", "An unexpected error occurred"
    )
}
