package com.stripe.android.crypto.onramp.example.model

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.crypto.onramp.example.network.OnrampSessionResponse
import com.stripe.android.crypto.onramp.example.network.SettlementSpeed
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

internal const val KEY_UI_STATE = "onramp_ui_state"

@Parcelize
internal data class OnrampUiState(
    val screen: Screen = Screen.Loading,
    val email: String = "",
    val linkAuthIntentId: String? = null,
    val consentedLinkAuthIntentIds: List<String> = emptyList(),
    @TypeParceler<PaymentMethodDisplayData?, NullPaymentMethodDisplayDataParceler>
    val selectedPaymentData: PaymentMethodDisplayData? = null,
    val cryptoPaymentToken: String? = null,
    val walletAddress: String? = null,
    val network: CryptoNetwork? = null,
    val authToken: String? = null,
    @TypeParceler<OnrampSessionResponse?, NullOnrampSessionResponseParceler>
    val onrampSession: OnrampSessionResponse? = null,
    val loadingMessage: String? = null,
    val settlementSpeed: SettlementSpeed = SettlementSpeed.INSTANT,
    val googlePayIsReady: Boolean = false,
    val kycFirstName: String = "",
    val kycLastName: String = "",
    val kycBirthCountry: String = "",
    val kycBirthCity: String = "",
    val kycNationalities: String = "",
    val kycAddress: PaymentSheet.Address = PaymentSheet.Address(),
    val identifierInputs: List<IdentifierInputEntry> = listOf(IdentifierInputEntry()),
    val missingIdentifiersSummary: String? = null,
    val submitIdentifiersSummary: String? = null,
) : Parcelable

@Parcelize
internal data class IdentifierInputEntry(
    val type: String = "",
    val value: String = "",
) : Parcelable

internal enum class Screen {
    SeamlessSignIn,
    LoginSignup,
    Loading,
    Registration,
    Authentication,
    AuthenticatedOperations,
}

private object NullPaymentMethodDisplayDataParceler : Parceler<PaymentMethodDisplayData?> {
    override fun create(parcel: Parcel): PaymentMethodDisplayData? = null

    override fun PaymentMethodDisplayData?.write(parcel: Parcel, flags: Int) {
        // No-op: this example only persists the presence of a selected payment method.
    }
}

private object NullOnrampSessionResponseParceler : Parceler<OnrampSessionResponse?> {
    override fun create(parcel: Parcel): OnrampSessionResponse? = null

    override fun OnrampSessionResponse?.write(parcel: Parcel, flags: Int) {
        // No-op: this example only persists the presence of a created session.
    }
}
