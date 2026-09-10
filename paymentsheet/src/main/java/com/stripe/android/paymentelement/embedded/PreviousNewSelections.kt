package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import kotlinx.parcelize.Parceler
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

    fun toBundle(): Bundle = Bundle().apply {
        selections.forEach { (code, selection) ->
            putParcelable(code, selection)
        }
    }

    companion object {
        val empty: PreviousNewSelections = PreviousNewSelections(emptyMap())

        fun fromBundle(bundle: Bundle): PreviousNewSelections {
            @Suppress("DEPRECATION")
            val selections = bundle.keySet().mapNotNull { code ->
                (bundle.getParcelable(code) as? PaymentSelection.New)?.let { selection ->
                    code to selection
                }
            }.toMap()
            return PreviousNewSelections(selections)
        }
    }
}

/** Preserves the Bundle encoding used before [PreviousNewSelections] became a typed value. */
internal object PreviousNewSelectionsParceler : Parceler<PreviousNewSelections> {
    override fun create(parcel: Parcel): PreviousNewSelections {
        val bundle = parcel.readBundle(PreviousNewSelections::class.java.classLoader) ?: Bundle()
        return PreviousNewSelections.fromBundle(bundle)
    }

    override fun PreviousNewSelections.write(parcel: Parcel, flags: Int) {
        parcel.writeBundle(toBundle())
    }
}
