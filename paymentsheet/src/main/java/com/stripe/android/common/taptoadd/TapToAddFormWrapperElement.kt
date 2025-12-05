package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.WalletsDivider
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class TapToAddFormWrapperElement(
    private val tapToAddHelper: TapToAddHelper,
    private val elements: List<FormElement>
) : RenderableFormElement(
    identifier = TapToAddFormIdentifier,
    allowsUserInteraction = true,
) {
    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
    ) {
        TapToAddButton(enabled) {
            tapToAddHelper.collect()
        }

        Spacer(modifier = Modifier.requiredHeight(24.dp))

        WalletsDivider(stringResource(R.string.stripe_or_enter_manually))

        Spacer(modifier = Modifier.requiredHeight(24.dp))

        FormUI(
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            enabled = enabled,
            elements = elements,
        )
    }

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return combineAsStateFlow(
            elements.map {
                it.getFormFieldValueFlow()
            }
        ) {
            it.flatten()
        }
    }
}
