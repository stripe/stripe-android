package com.stripe.android.taptoadd

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
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
    private val formElements: List<FormElement>,
    private val linkElements: List<FormElement>,
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

        val formElements = remember(collectedPaymentMethod) {
            collectedPaymentMethod.formElements()
        }

        collectedPaymentMethod?.let { pm ->
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = pm,
                isEnabled = true,
                isSelected = false,
            )

            Spacer(modifier = Modifier.requiredHeight(12.dp))
        } ?: run {
            TapToAddButton(enabled) {
                tapToAddHelper.collect()
            }

            Spacer(modifier = Modifier.requiredHeight(24.dp))

            WalletsDivider("or enter details manually")

            Spacer(modifier = Modifier.requiredHeight(24.dp))
        }

        FormUI(
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            enabled = enabled,
            elements = formElements,
        )
    }

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return tapToAddHelper.collectedPaymentMethod.flatMapLatestAsStateFlow { paymentMethod ->
            val entries = paymentMethod?.let {
                listOf(
                    TapToAddFormCollectedIdentifier to
                        FormFieldEntry(TapToAddFormCollectedValue, isComplete = true)
                )
            } ?: emptyList()

            combineAsStateFlow(
                paymentMethod.formElements().map { element ->
                    element.getFormFieldValueFlow()
                } + listOf(stateFlowOf(entries))
            ) {
                it.flatten()
            }
        }
    }

    private fun DisplayableSavedPaymentMethod?.formElements(): List<FormElement> {
        return this?.let {
            linkElements
        } ?: (formElements + linkElements)
    }
}
