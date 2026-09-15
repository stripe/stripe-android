package com.stripe.android.identity.networked

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

/**
 * Pixel 6 is the repository's supported Paparazzi device; fixed viewport height exercises centered
 * content, footer and scrolling.
 */
internal class NetworkedIdentityScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(SystemAppearance.entries, FontSize.entries)

    @Test
    fun saveFailed() = snapshot(
        NetworkedIdentityState.SaveFailed(details = "The request could not be completed. (resource_missing)"),
        mode = NetworkedIdentityMode.Save,
    )

    @Test
    fun savePrepared() = snapshot(NetworkedIdentityState.SavePrepared, mode = NetworkedIdentityMode.Save)

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

    private fun snapshot(
        state: NetworkedIdentityState,
        mode: NetworkedIdentityMode = NetworkedIdentityMode.Reuse,
    ) {
        paparazziRule.snapshot {
            Box(Modifier.height(800.dp)) {
                NetworkedIdentityScreen(
                    state = state,
                    mode = mode,
                    actions = NetworkedIdentityScreenActions(
                        onSelectDocument = {},
                        onShareDocument = {},
                        onContinue = {},
                        onManualCapture = {},
                        onCancel = {},
                    ),
                )
            }
        }
    }
}
