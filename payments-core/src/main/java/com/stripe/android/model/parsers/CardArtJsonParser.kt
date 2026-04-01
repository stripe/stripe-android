package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CardArtJsonParser {
    fun parse(json: JSONObject): PaymentMethod.Card.CardArt {
        return PaymentMethod.Card.CardArt(
            artImage = json.optJSONObject(FIELD_ART_IMAGE)?.let {
                ArtImageJsonParser().parse(it)
            },
            programName = StripeJsonUtils.optString(json, FIELD_PROGRAM_NAME)
        )
    }

    internal class ArtImageJsonParser {
        fun parse(json: JSONObject): PaymentMethod.Card.CardArt.ArtImage? {
            val format = StripeJsonUtils.optString(json, FIELD_FORMAT) ?: return null
            val url = StripeJsonUtils.optString(json, FIELD_URL) ?: return null
            return PaymentMethod.Card.CardArt.ArtImage(
                format = format,
                url = url
            )
        }

        private companion object {
            private const val FIELD_FORMAT = "format"
            private const val FIELD_URL = "url"
        }
    }

    private companion object {
        private const val FIELD_ART_IMAGE = "art_image"
        private const val FIELD_PROGRAM_NAME = "program_name"
    }
}
