package com.techgv.vitalcare

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import org.jetbrains.compose.resources.stringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.app_name

// Temporary shell — replaced by the themed Scaffold + NavHost in the
// navigation checkpoint (C5).
@Composable
fun App() {
    VitalCareTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
    }
}
