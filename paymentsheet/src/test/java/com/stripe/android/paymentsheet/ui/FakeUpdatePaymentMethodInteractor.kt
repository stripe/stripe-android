package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope

internal class FakeUpdatePaymentMethodInteractor(
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFactory.visaCard()
        .toDisplayableSavedPaymentMethod(),
    override val canRemove: Boolean = true,
    override val isExpiredCard: Boolean = false,
    override val isModifiablePaymentMethod: Boolean = false,
    override val hasValidBrandChoices: Boolean = true,
    override val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    override val shouldShowSetAsDefaultCheckbox: Boolean = false,
    override val shouldShowSaveButton: Boolean = false,
    override val addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
    val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>? = ViewActionRecorder(),
    initialState: UpdatePaymentMethodInteractor.State = UpdatePaymentMethodInteractor.State(
        error = null,
        status = UpdatePaymentMethodInteractor.Status.Idle,
        setAsDefaultCheckboxChecked = false,
        isSaveButtonEnabled = false,
    ),
    override val setAsDefaultCheckboxEnabled: Boolean = true,
    override val canUpdateFullPaymentMethodDetails: Boolean = false,
    private val editCardDetailsInteractorFactory: EditCardDetailsInteractor.Factory = DefaultEditCardDetailsInteractor
        .Factory(),
) : UpdatePaymentMethodInteractor {
    override val state: StateFlow<UpdatePaymentMethodInteractor.State> = MutableStateFlow(initialState)
    override val screenTitle: ResolvableString? = UpdatePaymentMethodInteractor.screenTitle(
        displayableSavedPaymentMethod
    )
    override val editCardDetailsInteractor: EditCardDetailsInteractor by lazy {
        val isModifiable =
            displayableSavedPaymentMethod.isModifiable(canUpdateFullPaymentMethodDetails)
        editCardDetailsInteractorFactory.create(
            coroutineScope = TestScope(),
            cardEditConfiguration = CardEditConfiguration(
                cardBrandFilter = cardBrandFilter,
                isCbcModifiable = isModifiable && displayableSavedPaymentMethod.canChangeCbc(),
                areExpiryDateAndAddressModificationSupported = isModifiable && canUpdateFullPaymentMethodDetails
            ),
            requiresModification = true,
            payload = EditCardPayload.create(
                card = displayableSavedPaymentMethod.paymentMethod.card!!,
                billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = addressCollectionMode
            ),
            onBrandChoiceChanged = {},
            onCardUpdateParamsChanged = {},
        )
    }

    override val topBarState: PaymentSheetTopBarState = PaymentSheetTopBarStateFactory.create(
        isLiveMode = false,
        editable = PaymentSheetTopBarState.Editable.Never,
    )

    override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
        viewActionRecorder?.record(viewAction)
    }
}
