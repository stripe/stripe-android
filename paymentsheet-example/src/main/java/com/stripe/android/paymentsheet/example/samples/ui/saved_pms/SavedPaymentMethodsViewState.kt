package com.stripe.android.paymentsheet.example.samples.ui.saved_pms

data class SavedPaymentMethodsViewState(
    val customerType: CustomerType = CustomerType.New,
    val customerState: CustomerState? = null,
    val isProcessing: Boolean = false
) {
    data class CustomerState(
        val customerId: String,
    ) {
        companion object {
            fun empty(): CustomerState {
                return CustomerState("")
            }
        }
    }

    enum class CustomerType(val value: String) {
        New("new"),
        Returning("returning")
    }
}
