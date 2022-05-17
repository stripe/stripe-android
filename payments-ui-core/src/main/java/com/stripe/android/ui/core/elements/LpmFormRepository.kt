package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SharedSpec(
    val type: String,
    val async: Boolean,
    val fields: List<FormItemSpec>,
)

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LpmFormRepository @Inject constructor(
    val resources: Resources?
) {
    private lateinit var codeToForm: Map<String, LayoutSpec>
    private val format = Json { ignoreUnknownKeys = true }

    fun get(code: String) = requireNotNull(codeToForm[code])

    init {
        initialize(
            resources?.assets?.open("lpms.json")
        )
    }

    @VisibleForTesting
    fun initialize(inputStream: InputStream?) {
        codeToForm = parseLpms(inputStream)?.associate {
            it.type to LayoutSpec(it.fields)
        } ?: emptyMap()
    }

    private fun parseLpms(inputStream: InputStream?) =
        getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<List<SharedSpec>>(
                it
            )
        }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }
}
