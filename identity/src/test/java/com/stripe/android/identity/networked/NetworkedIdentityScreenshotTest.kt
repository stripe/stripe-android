package com.stripe.android.identity.networked

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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
    fun saveOffer() {
        paparazziRule.snapshot {
            val identityViewModel = mock<IdentityViewModel> {
                on { verificationPageSubmit } doReturn MutableStateFlow(Resource.idle())
            }
            NetworkedIdentitySaveScreen(mock(), identityViewModel, niViewModel(includesSelfie = true))
        }
    }

    @Test
    fun savePrepared() = snapshot(
        NetworkedIdentityState.SavePrepared,
        mode = NetworkedIdentityMode.Save,
        includesSelfie = true,
    )

    @Test
    fun selectedSavedDocument() = snapshot(
        NetworkedIdentityState.SelectDocument(
            documents = listOf(
                NetworkedIdentityDocument(
                    id = "license",
                    documentType = NetworkedIdentityDocumentType.DRIVING_LICENSE,
                    redactedDocumentNumber = "•••• 4242",
                    expirationDate = null,
                    liveCaptured = true,
                ),
                NetworkedIdentityDocument(
                    id = "passport",
                    documentType = NetworkedIdentityDocumentType.PASSPORT,
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
        includesSelfie: Boolean = false,
    ) {
        paparazziRule.snapshot {
            Box(Modifier.height(800.dp)) {
                NetworkedIdentityScreen(
                    state = state,
                    mode = mode,
                    includesSelfie = includesSelfie,
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
