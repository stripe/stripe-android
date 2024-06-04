package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun convertToFormValuesMap(paramMap: Map<String, Any?>): Map<IdentifierSpec, String?> {
    val mutableMap = mutableMapOf<IdentifierSpec, String?>()
    addPath(paramMap, "", mutableMap)
    return mutableMap
}

@Suppress("UNCHECKED_CAST")
private fun addPath(
    paramMap: Map<String, Any?>,
    path: String,
    output: MutableMap<IdentifierSpec, String?>
) {
    for (entry in paramMap.entries) {
        when (entry.value) {
            null -> {
                output[IdentifierSpec.get(addPathKey(path, entry.key))] = null
            }
            is String -> {
                output[IdentifierSpec.get(addPathKey(path, entry.key))] = entry.value as String
            }
            is Map<*, *> -> {
                addPath(entry.value as Map<String, Any>, addPathKey(path, entry.key), output)
            }
        }
    }
}

private fun addPathKey(original: String, add: String) = if (original.isEmpty()) {
    add
} else {
    "$original[$add]"
}
