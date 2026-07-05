package com.techgv.vitalcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.techgv.vitalcare.data.backup.AndroidDriveAuthorizer
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val driveAuthorizer: AndroidDriveAuthorizer by inject()

    // Google consent UI for Drive authorization (D-021).
    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        driveAuthorizer.onAuthorizationResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        driveAuthorizer.resolutionLauncher = { pendingIntent ->
            authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        driveAuthorizer.resolutionLauncher = null
        super.onDestroy()
    }
}
