package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PreviousNewSelections private constructor(
    private val selections: Map<PaymentMethodCode, PaymentSelection.New>,
) : Parcelable {
    operator fun get(code: PaymentMethodCode): PaymentSelection.New? = selections[code]

    fun updatedWith(selection: PaymentSelection?): PreviousNewSelections {
        return if (selection is PaymentSelection.New) {
            PreviousNewSelections(selections + (selection.paymentMethodType to selection))
        } else {
            this
        }
    }

    fun mergedWith(other: PreviousNewSelections): PreviousNewSelections {
        return PreviousNewSelections(selections + other.selections)
    }

    val isEmpty: Boolean
        get() = selections.isEmpty()

    companion object {
        val empty: PreviousNewSelections = PreviousNewSelections(emptyMap())
    }
}
