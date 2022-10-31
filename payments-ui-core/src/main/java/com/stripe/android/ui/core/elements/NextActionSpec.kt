package com.stripe.android.ui.core.elements

import com.stripe.android.StripeIntentResult
import com.stripe.android.model.LuxePostConfirmActionCreator
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.StripeIntent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = ConfirmResponseStatusSpecsSerializer::class)
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

object ConfirmResponseStatusSpecsSerializer :
    JsonContentPolymorphicSerializer<ConfirmResponseStatusSpecs>(ConfirmResponseStatusSpecs::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ConfirmResponseStatusSpecs> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "finished" -> ConfirmResponseStatusSpecs.FinishedSpec.serializer()
            "canceled" -> ConfirmResponseStatusSpecs.CanceledSpec.serializer()
            "redirect_to_url" -> ConfirmResponseStatusSpecs.RedirectNextActionSpec.serializer()
            else -> ConfirmResponseStatusSpecs.CanceledSpec.serializer()
        }
    }
}

@Serializable(with = PostConfirmHandlingPiStatusSpecsSerializer::class)
sealed class PostConfirmHandlingPiStatusSpecs {

    @Serializable
    @SerialName("finished")
    object FinishedSpec : PostConfirmHandlingPiStatusSpecs()

    @Serializable
    @SerialName("canceled")
    object CanceledSpec : PostConfirmHandlingPiStatusSpecs()
}

object PostConfirmHandlingPiStatusSpecsSerializer :
    JsonContentPolymorphicSerializer<PostConfirmHandlingPiStatusSpecs>(
        PostConfirmHandlingPiStatusSpecs::class
    ) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out PostConfirmHandlingPiStatusSpecs> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "finished" -> PostConfirmHandlingPiStatusSpecs.FinishedSpec.serializer()
            "canceled" -> PostConfirmHandlingPiStatusSpecs.CanceledSpec.serializer()
            else -> PostConfirmHandlingPiStatusSpecs.CanceledSpec.serializer()
        }
    }
}

fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

@Serializable
data class ConfirmStatusSpecAssociation(
    @SerialName("requires_payment_method")
    val requiresPaymentMethod: ConfirmResponseStatusSpecs? = null,
    @SerialName("requires_confirmation")
    val requiresConfirmation: ConfirmResponseStatusSpecs? = null,
    @SerialName("requires_action")
    val requiresAction: ConfirmResponseStatusSpecs? = null,
    @SerialName("processing")
    val processing: ConfirmResponseStatusSpecs? = null,
    @SerialName("succeeded")
    val succeeded: ConfirmResponseStatusSpecs? = ConfirmResponseStatusSpecs.FinishedSpec,
    @SerialName("canceled")
    val canceled: ConfirmResponseStatusSpecs? = null
) {
    fun getMap() =
        mapOf(
            StripeIntent.Status.RequiresPaymentMethod to requiresPaymentMethod,
            StripeIntent.Status.RequiresConfirmation to requiresConfirmation,
            StripeIntent.Status.RequiresAction to requiresAction,
            StripeIntent.Status.Processing to processing,
            StripeIntent.Status.Succeeded to succeeded,
            StripeIntent.Status.Canceled to canceled
        ).filterNotNullValues()
}

@Serializable
data class PostConfirmStatusSpecAssociation(
    @SerialName("requires_payment_method")
    val requiresPaymentMethod: PostConfirmHandlingPiStatusSpecs? = null,
    @SerialName("requires_confirmation")
    val requiresConfirmation: PostConfirmHandlingPiStatusSpecs? = null,
    @SerialName("requires_action")
    val requiresAction: PostConfirmHandlingPiStatusSpecs? = null,
    @SerialName("processing")
    val processing: PostConfirmHandlingPiStatusSpecs? = null,
    @SerialName("succeeded")
    val succeeded: PostConfirmHandlingPiStatusSpecs? = null,
    @SerialName("canceled")
    val canceled: PostConfirmHandlingPiStatusSpecs? = null
) {
    fun getMap() = mapOf(
        StripeIntent.Status.RequiresPaymentMethod to requiresPaymentMethod,
        StripeIntent.Status.RequiresConfirmation to requiresConfirmation,
        StripeIntent.Status.RequiresAction to requiresAction,
        StripeIntent.Status.Processing to processing,
        StripeIntent.Status.Succeeded to succeeded,
        StripeIntent.Status.Canceled to canceled
    ).filterNotNullValues()
}

@Serializable
@SerialName("next_action_spec")
data class NextActionSpec(
    @SerialName("confirm_response_status_specs")
    val confirmResponseStatusSpecs: ConfirmStatusSpecAssociation? = null,

    @SerialName("post_confirm_handling_pi_status_specs")
    val postConfirmHandlingPiStatusSpecs: PostConfirmStatusSpecAssociation? = null
)

fun NextActionSpec?.transform() =
    if (this == null) {
        // LUXE does not support the next action, it should be entirely handled by the SDK both
        // the next action and the status to outcome check
        LuxePostConfirmActionRepository.LuxeAction(
            postConfirmStatusToAction = emptyMap(),
            postConfirmActionIntentStatus = emptyMap()
        )
    } else {
        val statusOutcomeMap = mutableMapOf<StripeIntent.Status, Int>()
        postConfirmHandlingPiStatusSpecs?.let {
            statusOutcomeMap.putAll(
                it.getMap()
                    .mapValues { entry -> mapToOutcome(entry.value) }
                    .filterNotNullValues()
            )
        }
        confirmResponseStatusSpecs?.let {
            statusOutcomeMap.putAll(
                it.getMap()
                    .mapValues { mapToOutcome(it.value) }
                    .filterNotNullValues()
            )
        }

        LuxePostConfirmActionRepository.LuxeAction(
            postConfirmStatusToAction = confirmResponseStatusSpecs?.let {
                it.getMap().mapValues { status -> getNextActionFromSpec(status.value) }
            } ?: emptyMap(),
            postConfirmActionIntentStatus = statusOutcomeMap
        )
    }

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

fun getNextActionFromSpec(confirmResponseStatusSpecs: ConfirmResponseStatusSpecs) =
    when (confirmResponseStatusSpecs) {
        is ConfirmResponseStatusSpecs.RedirectNextActionSpec -> {
            LuxePostConfirmActionCreator.RedirectActionCreator(
                confirmResponseStatusSpecs.urlPath,
                confirmResponseStatusSpecs.returnUrlPath
            )
        }
        is ConfirmResponseStatusSpecs.CanceledSpec -> {
            LuxePostConfirmActionCreator.NoActionCreator
        }
        is ConfirmResponseStatusSpecs.FinishedSpec -> {
            LuxePostConfirmActionCreator.NoActionCreator
        }
    }
