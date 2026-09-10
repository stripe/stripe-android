package com.stripe.android.identity.networked

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import org.junit.Rule
import org.junit.Test

/**
 * Android equivalents of the five iOS NI references. Pixel 6 is the repository's supported
 * Paparazzi device; fixed viewport height exercises centered content, footer and scrolling.
 */
internal class NetworkedIdentityScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(SystemAppearance.entries, FontSize.entries)

    @Test
    fun blankEmail() = snapshot(NetworkedIdentityState.CollectEmail)

    @Test
    fun validEmail() = snapshot(NetworkedIdentityState.CollectEmail, email = "jane.diaz@example.com")

    @Test
    fun awaitingOtp() = snapshot(
        NetworkedIdentityState.AwaitingOtp(
            redactedPhoneNumber = "(***) *** **34",
            invalidCode = false,
            otpGeneration = 1,
        )
    )

    @Test
    fun invalidOtp() = snapshot(
        NetworkedIdentityState.AwaitingOtp(
            redactedPhoneNumber = "(***) *** **34",
            invalidCode = true,
            otpGeneration = 1,
        )
    )

    @Test
    fun selectedSavedDocument() = snapshot(
        NetworkedIdentityState.SelectDocument(
            documents = listOf(
                NetworkedIdentityDocument(
                    id = "license",
                    documentType = NetworkedIdentityDocumentType.DRIVING_LICENSE,
                    created = 1,
                    country = "US",
                    region = "CA",
                    redactedDocumentNumber = "•••• 4242",
                    expirationDate = null,
                    liveCaptured = true,
                ),
                NetworkedIdentityDocument(
                    id = "passport",
                    documentType = NetworkedIdentityDocumentType.PASSPORT,
                    created = 1,
                    country = "US",
                    region = null,
                    redactedDocumentNumber = "•••• 6789",
                    expirationDate = null,
                    liveCaptured = true,
                ),
            ),
            selectedDocumentId = "passport",
        )
    )

    private fun snapshot(state: NetworkedIdentityState, email: String = "") {
        paparazziRule.snapshot {
            val emailController = remember { EmailConfig.createController(initialValue = email) }
            Box(Modifier.height(800.dp)) {
                NetworkedIdentityScreenContent(
                    state = state,
                    emailController = emailController,
                    onSubmitEmail = {},
                    onSubmitOtp = {},
                    onSelectDocument = {},
                    onManualCapture = {},
                    onCancel = {},
                )
            }
        }
    }
}
