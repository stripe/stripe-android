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
        val equals: Double? = null,
        val greaterThan: Double? = null,
        val lessThan: Double? = null,
        val between: BetweenRangeJs? = null,
    )

    @Serializable
    data class BetweenRangeJs(
        val lowerBound: Double,
        val upperBound: Double,
    )

    @Serializable
    data class DateFilterJs(
        val before: Long? = null,
        val after: Long? = null,
        val between: BetweenDateRangeJs? = null,
    )

    @Serializable
    data class BetweenDateRangeJs(
        val start: Long,
        val end: Long,
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
                            equals = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.GreaterThan -> PaymentsPropsJs.AmountFilterJs(
                            greaterThan = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.LessThan -> PaymentsPropsJs.AmountFilterJs(
                            lessThan = amountFilter.value
                        )
                        is PaymentsProps.AmountFilter.Between -> PaymentsPropsJs.AmountFilterJs(
                            between = PaymentsPropsJs.BetweenRangeJs(
                                lowerBound = amountFilter.lowerBound,
                                upperBound = amountFilter.upperBound
                            )
                        )
                    }
                },
                date = filters.date?.let { dateFilter ->
                    when (dateFilter) {
                        is PaymentsProps.DateFilter.Before -> PaymentsPropsJs.DateFilterJs(
                            before = dateFilter.date.time
                        )
                        is PaymentsProps.DateFilter.After -> PaymentsPropsJs.DateFilterJs(
                            after = dateFilter.date.time
                        )
                        is PaymentsProps.DateFilter.Between -> PaymentsPropsJs.DateFilterJs(
                            between = PaymentsPropsJs.BetweenDateRangeJs(
                                start = dateFilter.start.time,
                                end = dateFilter.end.time
                            )
                        )
                    }
                },
                status = filters.status?.map { it.value },
                paymentMethod = filters.paymentMethod?.value,
            )
        }
    )
}
