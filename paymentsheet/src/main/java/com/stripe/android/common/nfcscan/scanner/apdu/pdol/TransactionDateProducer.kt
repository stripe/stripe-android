package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import java.util.Calendar
import java.util.TimeZone

internal object TransactionDateProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        val calendar = Calendar.getInstance(UTC_TIME_ZONE).apply {
            timeInMillis = paymentMethodMetadata.stripeIntent.created * MILLIS_PER_SECOND
        }

        val year = calendar.get(Calendar.YEAR) % YEARS_IN_CENTURY
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val transactionDate = year * MONTH_AND_DAY_MULTIPLIER + month * DAY_MULTIPLIER + day

        return TagValueProducer.Result(
            tag = TAG_TRANSACTION_DATE,
            value = BcdEncoding.encode(transactionDate.toLong(), TRANSACTION_DATE_LENGTH),
        )
    }

    private const val TAG_TRANSACTION_DATE = "9A"
    private const val TRANSACTION_DATE_LENGTH = 3

    private const val YEARS_IN_CENTURY = 100
    private const val MILLIS_PER_SECOND = 1_000L

    private const val MONTH_AND_DAY_MULTIPLIER = 10_000
    private const val DAY_MULTIPLIER = 100

    private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")
}
