package com.stripe.android.connect

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import com.stripe.android.connect.webview.StripeConnectWebViewContainer
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import java.util.Date

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

@PrivateBetaConnectSDK
@Parcelize
@Poko
class PaymentsProps(
    /**
     * On load, show the payments matching the filter criteria.
     */
    internal val defaultFilters: PaymentsListDefaultFilters? = null,
) : Parcelable {
    @PrivateBetaConnectSDK
    @Parcelize
    sealed class AmountFilter : Parcelable {
        @Poko
        @Parcelize
        internal class Equals(val value: Double) : AmountFilter()

        @Poko
        @Parcelize
        internal class GreaterThan(val value: Double) : AmountFilter()

        @Poko
        @Parcelize
        internal class LessThan(val value: Double) : AmountFilter()

        @Parcelize
        internal class Between(val lowerBound: Double, val upperBound: Double) : AmountFilter()

        companion object {
            @JvmStatic
            fun equalTo(value: Double): AmountFilter = Equals(value)

            @JvmStatic
            fun greaterThan(value: Double): AmountFilter = GreaterThan(value)

            @JvmStatic
            fun lessThan(value: Double): AmountFilter = LessThan(value)

            @JvmStatic
            fun between(lowerBound: Double, upperBound: Double): AmountFilter = Between(lowerBound, upperBound)
        }
    }

    @PrivateBetaConnectSDK
    @Parcelize
    sealed class DateFilter : Parcelable {
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
        AFTERPAY_CLEARPAY("afterpay_clearpay"),
        ALIPAY("alipay"),
        ALMA("alma"),
        AMAZON_PAY("amazon_pay"),
        AMEX_EXPRESS_CHECKOUT("amex_express_checkout"),
        ANDROID_PAY("android_pay"),
        APPLE_PAY("apple_pay"),
        AU_BECS_DEBIT("au_becs_debit"),
        NZ_BANK_ACCOUNT("nz_bank_account"),
        BANCONTACT("bancontact"),
        BACS_DEBIT("bacs_debit"),
        BITCOIN_SOURCE("bitcoin_source"),
        BITCOIN("bitcoin"),
        BLIK("blik"),
        BOLETO("boleto"),
        BOLETO_PILOT("boleto_pilot"),
        CARD_PRESENT("card_present"),
        CARD("card"),
        CASHAPP("cashapp"),
        CRYPTO("crypto"),
        CUSTOMER_BALANCE("customer_balance"),
        DEMO_PAY("demo_pay"),
        DUMMY_PASSTHROUGH_CARD("dummy_passthrough_card"),
        GBP_CREDIT_TRANSFER("gbp_credit_transfer"),
        GOOGLE_PAY("google_pay"),
        EPS("eps"),
        FPX("fpx"),
        GIROPAY("giropay"),
        GRABPAY("grabpay"),
        IDEAL("ideal"),
        ID_BANK_TRANSFER("id_bank_transfer"),
        ID_CREDIT_TRANSFER("id_credit_transfer"),
        JP_CREDIT_TRANSFER("jp_credit_transfer"),
        INTERAC_PRESENT("interac_present"),
        KAKAO_PAY("kakao_pay"),
        KLARNA("klarna"),
        KONBINI("konbini"),
        KR_CARD("kr_card"),
        KR_MARKET("kr_market"),
        LINK("link"),
        MASTERPASS("masterpass"),
        MB_WAY("mb_way"),
        META_PAY("meta_pay"),
        MULTIBANCO("multibanco"),
        MOBILEPAY("mobilepay"),
        NAVER_PAY("naver_pay"),
        NETBANKING("netbanking"),
        NG_BANK("ng_bank"),
        NG_BANK_TRANSFER("ng_bank_transfer"),
        NG_CARD("ng_card"),
        NG_MARKET("ng_market"),
        NG_USSD("ng_ussd"),
        VIPPS("vipps"),
        OXXO("oxxo"),
        P24("p24"),
        PAYTO("payto"),
        PAY_BY_BANK("pay_by_bank"),
        PAPER_CHECK("paper_check"),
        PAYCO("payco"),
        PAYNOW("paynow"),
        PAYPAL("paypal"),
        PIX("pix"),
        PROMPTPAY("promptpay"),
        REVOLUT_PAY("revolut_pay"),
        SAMSUNG_PAY("samsung_pay"),
        SEPA_CREDIT_TRANSFER("sepa_credit_transfer"),
        SEPA_DEBIT("sepa_debit"),
        SOFORT("sofort"),
        SOUTH_KOREA_MARKET("south_korea_market"),
        SWISH("swish"),
        THREE_D_SECURE("three_d_secure"),
        THREE_D_SECURE_2("three_d_secure_2"),
        THREE_D_SECURE_2_EAP("three_d_secure_2_eap"),
        TWINT("twint"),
        UPI("upi"),
        US_BANK_ACCOUNT("us_bank_account"),
        VISA_CHECKOUT("visa_checkout"),
        WECHAT("wechat"),
        WECHAT_PAY("wechat_pay"),
        ZIP("zip"),
    }

    @PrivateBetaConnectSDK
    @Parcelize
    @Poko
    class PaymentsListDefaultFilters(
        val amount: AmountFilter? = null,
        val date: DateFilter? = null,
        val status: List<Status>? = null,
        val paymentMethod: PaymentMethod? = null,
    ) : Parcelable
}

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentsListener : StripeEmbeddedComponentListener
