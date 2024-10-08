package com.stripe.android.financialconnections.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import com.stripe.android.financialconnections.model.serializer.JsonAsStringSerializer
import com.stripe.android.financialconnections.model.serializer.PaymentAccountSerializer
import com.stripe.android.model.Token
import com.stripe.android.model.parsers.TokenJsonParser
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

/**
 * A Financial Connections Session is the secure way to programmatically launch the client-side
 * modal that lets your users link their accounts.
 *
 * @param accounts
 * @param clientSecret A value that will be passed to the client to launch the authentication flow.
 * @param id Unique identifier for the object.
 * @param livemode Has the value `true` if the object exists in live mode or the value `false` if
 * the object exists in test mode.
 * @param bankAccountToken
 * @param manualEntry
 * @param paymentAccount
 * @param returnUrl For webview integrations only. Upon completing OAuth login in the native browser,
 * the user will be redirected to this URL to return to your app.
 * @param status The current state of the session.
 * @param statusDetails
 */
@Parcelize
@Serializable
@Poko
class FinancialConnectionsSession internal constructor(
    @SerialName("client_secret")
    val clientSecret: String,

    @SerialName("id")
    val id: String,

    @SerialName("linked_accounts")
    internal val accountsOld: FinancialConnectionsAccountList? = null,

    @SerialName("accounts")
    internal val accountsNew: FinancialConnectionsAccountList? = null,

    @SerialName("livemode")
    val livemode: Boolean,

    @SerialName("payment_account")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val paymentAccount: PaymentAccount? = null,

    @SerialName("return_url")
    val returnUrl: String? = null,

    @SerialName("bank_account_token")
    @Serializable(with = JsonAsStringSerializer::class)
    internal val bankAccountToken: String? = null,

    @SerialName(value = "manual_entry")
    val manualEntry: ManualEntry? = null,

    @SerialName(value = "status")
    val status: Status? = null,

    @SerialName(value = "status_details")
    val statusDetails: StatusDetails? = null
) : Parcelable {

    val accounts: FinancialConnectionsAccountList
        get() = accountsNew ?: accountsOld!!

    internal val parsedToken: Token?
        get() = bankAccountToken?.let { TokenJsonParser().parse(JSONObject(it)) }

    /**
     * The current state of the session.
     *
     * Values: CANCELLED,FAILED,PENDING,SUCCEEDED
     */
    @Serializable(with = Status.Serializer::class)
    enum class Status(val value: String) {

        @SerialName(value = "pending")
        PENDING("pending"),

        @SerialName(value = "succeeded")
        SUCCEEDED("succeeded"),

        @SerialName(value = "canceled")
        CANCELED("canceled"),

        @SerialName(value = "failed")
        FAILED("failed"),

        @SerialName(value = "unknown")
        UNKNOWN("unknown");

        internal object Serializer :
            EnumIgnoreUnknownSerializer<Status>(
                entries.toTypedArray(),
                UNKNOWN
            )
    }

    @Serializable
    @Parcelize
    @Poko
    class StatusDetails(
        @SerialName(value = "cancelled") val cancelled: Cancelled? = null
    ) : Parcelable {
        @Serializable
        @Parcelize
        data class Cancelled(

            /* The reason for the Session being cancelled. */
            @SerialName(value = "reason") val reason: Reason

        ) : Parcelable {
            @Serializable(with = Reason.Serializer::class)
            enum class Reason(val value: String) {
                @SerialName(value = "custom_manual_entry")
                CUSTOM_MANUAL_ENTRY("custom_manual_entry"),

                @SerialName(value = "other")
                OTHER("other"),

                @SerialName(value = "unknown")
                UNKNOWN("unknown");

                internal object Serializer :
                    EnumIgnoreUnknownSerializer<Reason>(
                        entries.toTypedArray(),
                        UNKNOWN
                    )
            }
        }
    }
}

@Serializable(with = PaymentAccountSerializer::class)
sealed class PaymentAccount : Parcelable {
    abstract val id: String
}
