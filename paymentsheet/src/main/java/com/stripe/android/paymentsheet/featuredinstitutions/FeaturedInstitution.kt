package com.stripe.android.paymentsheet.featuredinstitutions

import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class FeaturedInstitution(
    val id: String,
    val name: String,
    val iconUrl: String?,
) : StripeModel

@Parcelize
internal data class FeaturedInstitutionList(
    val data: List<FeaturedInstitution>,
) : StripeModel

internal object FeaturedInstitutionListJsonParser : ModelJsonParser<FeaturedInstitutionList> {
    private const val FIELD_DATA = "data"
    private const val FIELD_ID = "id"
    private const val FIELD_NAME = "name"
    private const val FIELD_ICON = "icon"
    private const val FIELD_ICON_DEFAULT = "default"

    override fun parse(json: JSONObject): FeaturedInstitutionList {
        val data = json.optJSONArray(FIELD_DATA)
        val institutions = (0 until (data?.length() ?: 0)).mapNotNull { index ->
            data?.optJSONObject(index)?.let(::parseInstitution)
        }
        return FeaturedInstitutionList(data = institutions)
    }

    private fun parseInstitution(json: JSONObject): FeaturedInstitution? {
        val id = json.optString(FIELD_ID).takeIf { it.isNotEmpty() } ?: return null
        return FeaturedInstitution(
            id = id,
            name = json.optString(FIELD_NAME),
            iconUrl = json.optJSONObject(FIELD_ICON)
                ?.optString(FIELD_ICON_DEFAULT)
                ?.takeIf { it.isNotEmpty() },
        )
    }
}
