package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.Companion.KEY_NEXT_PANE_ON_DISABLE_NETWORKING
import com.stripe.android.financialconnections.ui.HandleClickableUrl.DeeplinkPayload
import com.stripe.android.uicore.navigation.NavigationManager

/**
 * Handles the deeplinks in the consent text, which is shared by the regular and streamlined consent panes.
 */
internal fun consentDeeplinkActions(
    pane: Pane,
    navigationManager: NavigationManager,
    onDataAccessClick: () -> Unit,
    onLegalDetailsClick: () -> Unit,
): Map<String, suspend (DeeplinkPayload) -> Unit> = mapOf(
    // Clicked on the "Data Access" link -> Open the Data Access bottom sheet
    ConsentClickableText.DATA.value to {
        onDataAccessClick()
    },
    // Clicked on the "Legal details" link -> Open the Legal Details bottom sheet
    ConsentClickableText.LEGAL_DETAILS.value to {
        onLegalDetailsClick()
    },
    // Clicked on the "Manual entry" link -> Navigate to the Manual Entry screen
    ConsentClickableText.MANUAL_ENTRY.value to {
        navigationManager.tryNavigateTo(Destination.ManualEntry(referrer = pane))
    },
    // Clicked on the "Manual entry" link on NME flows -> Navigate to the Link Login Warmup screen
    ConsentClickableText.LINK_LOGIN_WARMUP.value to {
        navigationManager.tryNavigateTo(
            Destination.NetworkingLinkLoginWarmup(
                referrer = pane,
                extraArgs = mapOf(KEY_NEXT_PANE_ON_DISABLE_NETWORKING to it.nextPaneOrDrawerOnSecondaryCta),
            )
        )
    },
    // Surfaces where user has signed in to Link and then launches the auth flow.
    ConsentClickableText.LINK_ACCOUNT_PICKER.value to {
        navigationManager.tryNavigateTo(Destination.LinkAccountPicker(referrer = pane))
    },
)
