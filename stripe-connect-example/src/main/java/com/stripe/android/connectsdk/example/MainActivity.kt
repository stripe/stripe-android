package com.stripe.android.connectsdk.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.FetchClientSecretCallback
import com.stripe.android.connectsdk.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connectsdk.PrivateBetaConnectSDK

@OptIn(PrivateBetaConnectSDK::class)
class MainActivity : ComponentActivity() {

    private lateinit var embeddedComponentManager: EmbeddedComponentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        embeddedComponentManager = EmbeddedComponentManager(
            activity = this,
            configuration = EmbeddedComponentManager.Configuration(
                publishableKey = "<publishable key here>"
            ),
            fetchClientSecret = object : FetchClientSecretCallback {
                override fun fetchClientSecret(resultCallback: ClientSecretResultCallback) {
                    // Make a network call to fetch your client secret here
                    resultCallback.onResult("<client secret here>")
                }
            }
        )

        setContent {
            Text("Not yet built...")
        }
    }

    fun launchPayouts() {
        // launch the payouts activity
        embeddedComponentManager.presentPayouts()
    }

    fun launchAccountOnboardingActivity() {
        // launch the account onboarding activity
        embeddedComponentManager.presentAccountOnboarding()
    }

    fun updateAppearance() {
        // update the appearance
        embeddedComponentManager.update(
            EmbeddedComponentManager.Appearance(
                typography = EmbeddedComponentManager.Appearance.Typography(),
                colors = EmbeddedComponentManager.Appearance.Colors(),
                buttonPrimary = EmbeddedComponentManager.Appearance.Button(),
                buttonSecondary = EmbeddedComponentManager.Appearance.Button(),
                badgeNeutral = EmbeddedComponentManager.Appearance.Badge(),
                badgeSuccess = EmbeddedComponentManager.Appearance.Badge(),
                badgeWarning = EmbeddedComponentManager.Appearance.Badge(),
                badgeDanger = EmbeddedComponentManager.Appearance.Badge(),
                cornerRadius = EmbeddedComponentManager.Appearance.CornerRadius()
            )
        )
    }

    fun logout() {
        // log out the user
        embeddedComponentManager.logout()
    }
}
