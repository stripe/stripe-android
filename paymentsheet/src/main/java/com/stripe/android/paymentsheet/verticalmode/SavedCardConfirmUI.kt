package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.inline.LinkElement
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets

@Composable
internal fun SavedCardConfirmUI(
    interactor: SavedCardConfirmInteractor,
    modifier: Modifier = Modifier,
) {
    // TODO: do we need to have the modifier passed into this? are we gonna use it for anything? idk.
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    Column(
        modifier = Modifier
            .padding(horizontalPadding)
            .testTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SavedPaymentMethodRowButton(
            displayableSavedPaymentMethod = interactor.paymentMethod,
            isEnabled = true, // TODO: we want it to look enabled but not actually be clickable.
            isSelected = true,
            onClick = { },
            trailingContent = { },
        )

        // TODO: we'd use the link form helper here, not this directly.
        interactor.linkConfiguration?.let {
            LinkElement(
                initialUserInput = null,
                linkConfigurationCoordinator = interactor.linkConfigurationCoordinator,
                configuration = it,
                linkSignupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                enabled = true,
                onLinkSignupStateChanged = {
                    interactor.handleViewAction(
                        SavedCardConfirmInteractor.ViewAction.CheckLinkInlineSignup(
                            it
                        )
                    )
                },
                modifier = Modifier,
                previousLinkSignupCheckboxSelection = null,
            )
        }
    }
}
