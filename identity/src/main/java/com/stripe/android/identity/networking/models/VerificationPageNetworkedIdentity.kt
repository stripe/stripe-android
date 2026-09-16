package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

// #TODO - Networked Identity: Confirm the draft v8 bootstrap contract before production routing.
@Serializable
@Parcelize
internal data class VerificationPageNetworkedIdentity(
    @SerialName("save_available")
    val saveAvailable: Boolean?,
    @SerialName("reuse_available")
    val reuseAvailable: Boolean?,
    val email: String?,
    @SerialName("phone_number")
    val phoneNumber: String?,
    val state: State?,
) : Parcelable {
    val route: NetworkedIdentityRoute
        get() = when {
            state?.skipped != false || state.consented == null -> NetworkedIdentityRoute.OrdinaryIdentity
            state.consented -> when (state.direction) {
                Direction.ConsumerToMerchant -> NetworkedIdentityRoute.ResumeReuse
                Direction.MerchantToConsumer -> NetworkedIdentityRoute.ResumeSave
                Direction.Unknown, null -> NetworkedIdentityRoute.OrdinaryIdentity
            }
            state.direction != null -> NetworkedIdentityRoute.OrdinaryIdentity
            reuseAvailable == true -> NetworkedIdentityRoute.Reuse
            saveAvailable == true -> NetworkedIdentityRoute.Save
            else -> NetworkedIdentityRoute.OrdinaryIdentity
        }

    override fun toString(): String = "VerificationPageNetworkedIdentity([redacted])"

    @Serializable
    @Parcelize
    data class State(
        val consented: Boolean?,
        val skipped: Boolean?,
        val direction: Direction?,
    ) : Parcelable

    @Serializable(with = NetworkedIdentityDirectionSerializer::class)
    enum class Direction(val value: String) {
        ConsumerToMerchant("consumer_to_merchant"),
        MerchantToConsumer("merchant_to_consumer"),
        Unknown("unknown"),
    }
}

internal object NetworkedIdentityDirectionSerializer : KSerializer<VerificationPageNetworkedIdentity.Direction> {
    override val descriptor = PrimitiveSerialDescriptor("NetworkedIdentityDirection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): VerificationPageNetworkedIdentity.Direction {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        return VerificationPageNetworkedIdentity.Direction.entries.firstOrNull { it.value == value }
            ?: VerificationPageNetworkedIdentity.Direction.Unknown
    }

    override fun serialize(encoder: Encoder, value: VerificationPageNetworkedIdentity.Direction) {
        encoder.encodeString(value.value)
    }
}
