package com.stripe.android.ui.core.elements

import com.stripe.android.StripeIntentResult
import com.stripe.android.model.ActionCreator
import com.stripe.android.model.LuxeNextActionRepository
import com.stripe.android.model.StripeIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ConfirmResponseStatusSpecs {
    @Serializable
    @SerialName("redirect_to_url")
    data class RedirectNextActionSpec(
        @SerialName("url_path")
        val urlPath: String = "next_action[redirect_to_url][url]",
        @SerialName("return_url_path")
        val returnUrlPath: String = "next_action[redirect_to_url][return_url]"
    ) : ConfirmResponseStatusSpecs()

    @Serializable
    @SerialName("finished")
    object FinishedSpec : ConfirmResponseStatusSpecs()

    @Serializable
    @SerialName("canceled")
    object CanceledSpec : ConfirmResponseStatusSpecs()
}

@Serializable
sealed class PostConfirmHandlingPiStatusSpecs {

    @Serializable
    @SerialName("finished")
    object FinishedSpec : PostConfirmHandlingPiStatusSpecs()

    @Serializable
    @SerialName("canceled")
    object CanceledSpec : PostConfirmHandlingPiStatusSpecs()
}

fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

@Serializable
data class ConfirmStatusSpecAssociation(
    @SerialName("requires_payment_method")
    val requires_payment_method: ConfirmResponseStatusSpecs?,
    @SerialName("requires_confirmation")
    val requires_confirmation: ConfirmResponseStatusSpecs?,
    @SerialName("requires_action")
    val requires_action: ConfirmResponseStatusSpecs?,
    @SerialName("processing")
    val processing: ConfirmResponseStatusSpecs?,
    @SerialName("succeeded")
    val succeeded: ConfirmResponseStatusSpecs?,
    @SerialName("canceled")
    val canceled: ConfirmResponseStatusSpecs?
) {
    fun getMap() =
        mapOf(
            StripeIntent.Status.RequiresPaymentMethod to requires_payment_method,
            StripeIntent.Status.RequiresConfirmation to requires_confirmation,
            StripeIntent.Status.RequiresAction to requires_action,
            StripeIntent.Status.Processing to processing,
            StripeIntent.Status.Succeeded to succeeded,
            StripeIntent.Status.Canceled to canceled,
        ).filterNotNullValues()
}

@Serializable
data class PostConfirmStatusSpecAssociation(
    @SerialName("requires_payment_method")
    val requires_payment_method: PostConfirmHandlingPiStatusSpecs?,
    @SerialName("requires_confirmation")
    val requires_confirmation: PostConfirmHandlingPiStatusSpecs?,
    @SerialName("requires_action")
    val requires_action: PostConfirmHandlingPiStatusSpecs?,
    @SerialName("processing")
    val processing: PostConfirmHandlingPiStatusSpecs?,
    @SerialName("succeeded")
    val succeeded: PostConfirmHandlingPiStatusSpecs?,
    @SerialName("canceled")
    val canceled: PostConfirmHandlingPiStatusSpecs?
) {
    fun getMap() = mapOf(
        StripeIntent.Status.RequiresPaymentMethod to requires_payment_method,
        StripeIntent.Status.RequiresConfirmation to requires_confirmation,
        StripeIntent.Status.RequiresAction to requires_action,
        StripeIntent.Status.Processing to processing,
        StripeIntent.Status.Succeeded to succeeded,
        StripeIntent.Status.Canceled to canceled,
    ).filterNotNullValues()
}

@Serializable
@SerialName("next_action_spec")
data class NextActionSpec(
    @SerialName("confirm_response_status_specs")
    val confirmResponseStatusSpecs: ConfirmStatusSpecAssociation?,

    @SerialName("post_confirm_handling_pi_status_specs")
    val postConfirmHandlingPiStatusSpecs: PostConfirmStatusSpecAssociation?
)

fun mapToOutcome(spec: PostConfirmHandlingPiStatusSpecs?) = when (spec) {
    PostConfirmHandlingPiStatusSpecs.CanceledSpec -> StripeIntentResult.Outcome.CANCELED
    PostConfirmHandlingPiStatusSpecs.FinishedSpec -> StripeIntentResult.Outcome.SUCCEEDED
    null -> null
}

fun mapToOutcome(spec: ConfirmResponseStatusSpecs?) = when (spec) {
    ConfirmResponseStatusSpecs.CanceledSpec -> StripeIntentResult.Outcome.CANCELED
    ConfirmResponseStatusSpecs.FinishedSpec -> StripeIntentResult.Outcome.SUCCEEDED
    is ConfirmResponseStatusSpecs.RedirectNextActionSpec -> null
    null -> null
}

fun getNextAction(confirmResponseStatusSpecs: ConfirmResponseStatusSpecs) =
    when (confirmResponseStatusSpecs) {
        is ConfirmResponseStatusSpecs.RedirectNextActionSpec -> {
            ActionCreator.RedirectActionCreator(
                confirmResponseStatusSpecs.urlPath,
                confirmResponseStatusSpecs.returnUrlPath
            )
        }
        is ConfirmResponseStatusSpecs.CanceledSpec -> {
            ActionCreator.NoActionCreator
        }
        is ConfirmResponseStatusSpecs.FinishedSpec -> {
            ActionCreator.NoActionCreator
        }
    }

fun transform(spec: NextActionSpec?) {
    if (spec == null) {
        // LUXE does not support the next action, it should be entirely handled by the SDK both
        // the next action and the status to outcome check
        LuxeNextActionRepository.LuxeAction(
            postConfirmStatusNextStatus = emptyMap(),
            postAuthorizeIntentStatus = emptyMap()
        )
    } else {
        val statusOutcomeMap = mutableMapOf<StripeIntent.Status, Int>()
        spec.postConfirmHandlingPiStatusSpecs?.let {
            statusOutcomeMap.plus(
                it.getMap()
                    .mapValues { entry -> mapToOutcome(entry.value) }
                    .filterNotNullValues()
            )
        }
        spec.confirmResponseStatusSpecs?.let {
            statusOutcomeMap.plus(
                it.getMap()
                    .mapValues {
                        mapToOutcome(it.value)
                    }
            )
        }

        LuxeNextActionRepository.LuxeAction(
            postConfirmStatusNextStatus = spec.confirmResponseStatusSpecs?.let {
                it.getMap().mapValues { status -> getNextAction(status.value) }
            } ?: emptyMap(),
            postAuthorizeIntentStatus = statusOutcomeMap
        )
    }
}
