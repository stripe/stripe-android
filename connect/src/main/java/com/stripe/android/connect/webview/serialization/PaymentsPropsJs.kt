package com.stripe.android.connect.webview.serialization

import com.stripe.android.connect.PaymentsProps
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentsPropsJs(
    val setDefaultFilters: PaymentsListDefaultFiltersJs?,
) {
    @Serializable
    data class PaymentsListDefaultFiltersJs(
        val amount: AmountFilterJs?,
        val date: DateFilterJs?,
        val status: List<String>?,
        val paymentMethod: String?,
    )

    @Serializable
    data class AmountFilterJs(
        val type: String,
        val value: Int? = null,
        val lowerBound: Int? = null,
        val upperBound: Int? = null,
    )

    @Serializable
    data class DateFilterJs(
        val type: String,
        val value: String? = null,
        val start: String? = null,
        val end: String? = null,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
internal fun PaymentsProps.toJs(): PaymentsPropsJs {
    return PaymentsPropsJs(
        setDefaultFilters = defaultFilters?.let { filters ->
            PaymentsPropsJs.PaymentsListDefaultFiltersJs(
                amount = filters.amount?.let { amountFilter ->
                    when (amountFilter) {
                        is PaymentsProps.AmountFilter.Equals -> PaymentsPropsJs.AmountFilterJs(
                            type = "equals",
                            value = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.GreaterThan -> PaymentsPropsJs.AmountFilterJs(
                            type = "greaterThan",
                            value = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.LessThan -> PaymentsPropsJs.AmountFilterJs(
                            type = "lessThan",
                            value = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.Between -> PaymentsPropsJs.AmountFilterJs(
                            type = "between",
                            lowerBound = amountFilter.lowerBound,
                            upperBound = amountFilter.upperBound
                        )
                    }
                },
                date = filters.date?.let { dateFilter ->
                    when (dateFilter) {
                        is PaymentsProps.DateFilter.Before -> PaymentsPropsJs.DateFilterJs(
                            type = "before",
                            value = dateFilter.date.toString()
                        )
                        is PaymentsProps.DateFilter.After -> PaymentsPropsJs.DateFilterJs(
                            type = "after",
                            value = dateFilter.date.toString()
                        )
                        is PaymentsProps.DateFilter.Between -> PaymentsPropsJs.DateFilterJs(
                            type = "between",
                            start = dateFilter.start.toString(),
                            end = dateFilter.end.toString()
                        )
                    }
                },
                status = filters.status?.map { it.value },
                paymentMethod = filters.paymentMethod?.value,
            )
        }
    )
}