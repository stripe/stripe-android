package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.model.StripeJsonUtils.optInteger
import com.stripe.android.core.model.StripeJsonUtils.optLong
import com.stripe.android.core.model.StripeJsonUtils.optString
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeFileJsonParser : ModelJsonParser<StripeFile> {
    override fun parse(json: JSONObject): StripeFile {
        return StripeFile(
            id = optString(json, FIELD_ID),
            created = optLong(json, FIELD_CREATED),
            filename = optString(json, FIELD_FILENAME),
            purpose = StripeFilePurpose.fromCode(optString(json, FIELD_PURPOSE)),
            size = optInteger(json, FIELD_SIZE),
            title = optString(json, FIELD_TITLE),
            type = optString(json, FIELD_TYPE),
            url = optString(json, FIELD_URL)
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_CREATED = "created"
        private const val FIELD_FILENAME = "filename"
        private const val FIELD_PURPOSE = "purpose"
        private const val FIELD_SIZE = "size"
        private const val FIELD_TITLE = "title"
        private const val FIELD_TYPE = "type"
        private const val FIELD_URL = "url"
    }
}
