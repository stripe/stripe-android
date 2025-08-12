package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@PrivateBetaConnectSDK
class PaymentsView internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    embeddedComponentManager: EmbeddedComponentManager?,
    listener: PaymentsListener?,
    props: PaymentsProps?,
    cacheKey: String?,
) :
    StripeComponentView<PaymentsListener, PaymentsProps>(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponent = StripeEmbeddedComponent.PAYMENTS,
        embeddedComponentManager = embeddedComponentManager,
        listener = listener,
        props = props,
    ),
    StripeConnectWebViewContainer<PaymentsListener, PaymentsProps> {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : this(
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        embeddedComponentManager = null,
        listener = null,
        props = null,
        cacheKey = null,
    )

    init {
        var xmlCacheKey: String? = null
        context.withStyledAttributes(attrs, R.styleable.StripeConnectWebViewContainer, defStyleAttr, 0) {
            xmlCacheKey = getString(R.styleable.StripeConnectWebViewContainer_stripeWebViewCacheKey)
        }
        initializeView(cacheKey ?: xmlCacheKey)
    }
}

// props
@PrivateBetaConnectSDK
@Parcelize
@Poko
class PaymentsProps(
    /**
     * On load, show the payments matching the filter criteria.
     */
    val defaultFilters: PaymentsListDefaultFilters? = null,
): Parcelable {
    // defaultFilters
    @PrivateBetaConnectSDK
    @Parcelize
    sealed class AmountFilter: Parcelable {
        @Poko
        @Parcelize
        internal class Equals(val value: Int) : AmountFilter()
        
        @Poko
        @Parcelize
        internal class GreaterThan(val value: Int) : AmountFilter()
        
        @Poko
        @Parcelize
        internal class LessThan(val value: Int) : AmountFilter()
        
        @Poko
        @Parcelize
        internal class Between(val lowerBound: Int, val upperBound: Int) : AmountFilter()

        companion object {
            @JvmStatic
            fun equals(value: Int): AmountFilter = Equals(value)
            
            @JvmStatic
            fun greaterThan(value: Int): AmountFilter = GreaterThan(value)
            
            @JvmStatic
            fun lessThan(value: Int): AmountFilter = LessThan(value)
            
            @JvmStatic
            fun between(lowerBound: Int, upperBound: Int): AmountFilter = Between(lowerBound, upperBound)
        }
    }

    @PrivateBetaConnectSDK
    @Parcelize
    sealed class DateFilter: Parcelable {
        @Poko
        @Parcelize
        internal class Before(val date: Date) : DateFilter()
        
        @Poko
        @Parcelize
        internal class After(val date: Date) : DateFilter()
        
        @Poko
        @Parcelize
        internal class Between(val start: Date, val end: Date) : DateFilter()

        companion object {
            @JvmStatic
            fun before(date: Date): DateFilter = Before(date)
            
            @JvmStatic
            fun after(date: Date): DateFilter = After(date)
            
            @JvmStatic
            fun between(start: Date, end: Date): DateFilter = Between(start, end)
        }
    }

    @PrivateBetaConnectSDK
    enum class Status(internal val value: String) {
        BLOCKED("blocked"),
        CANCELED("canceled"),
        DISPUTED("disputed"),
        EARLY_FRAUD_WARNING("early_fraud_warning"),
        FAILED("failed"),
        INCOMPLETE("incomplete"),
        PARTIALLY_REFUNDED("partially_refunded"),
        PENDING("pending"),
        REFUND_PENDING("refund_pending"),
        REFUNDED("refunded"),
        SUCCESSFUL("successful"),
        UNCAPTURED("uncaptured"),
    }

    @PrivateBetaConnectSDK
    enum class PaymentMethod(internal val value: String) {
        ACH_CREDIT_TRANSFER("ach_credit_transfer"),
        ACH_DEBIT("ach_debit"),
        ACSS_DEBIT("acss_debit"),
        AFFIRM("affirm"),
        // ...TODO: add more more values here
    }

    @PrivateBetaConnectSDK
    @Parcelize
    @Poko
    class PaymentsListDefaultFilters(
        val amount: AmountFilter? = null,
        val date: DateFilter? = null,
        val status: List<Status>? = null,
        val paymentMethod: PaymentMethod? = null,
    ): Parcelable
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentsListener : StripeEmbeddedComponentListener
