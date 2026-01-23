package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.WalletsDivider
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodRowButton
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
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
        val collectedPaymentMethod by tapToAddHelper.collectedPaymentMethod.collectAsState()

        collectedPaymentMethod?.let { pm ->
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = pm,
                isEnabled = true,
                isSelected = true,
            )
        } ?: run {
            TapToAddButton(enabled) {
                tapToAddHelper.startPaymentMethodCollection()
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
    }

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return tapToAddHelper.collectedPaymentMethod.flatMapLatestAsStateFlow { collectedPaymentMethod ->
            if (collectedPaymentMethod != null) {
                stateFlowOf(emptyList())
            } else {
                combineAsStateFlow(
                    elements.map {
                        it.getFormFieldValueFlow()
                    }
                ) {
                    it.flatten()
                }
            }
        }
    }
}
