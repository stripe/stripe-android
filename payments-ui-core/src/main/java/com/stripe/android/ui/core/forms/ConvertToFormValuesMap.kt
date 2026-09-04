package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.FormFieldId

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun convertToFormValuesMap(paramMap: Map<String, Any?>): Map<FormFieldId, String?> {
    val mutableMap = mutableMapOf<FormFieldId, String?>()
    addPath(paramMap, "", mutableMap)
    return mutableMap
}

@Suppress("UNCHECKED_CAST")
private fun addPath(
    paramMap: Map<String, Any?>,
    path: String,
    output: MutableMap<FormFieldId, String?>
) {
    for (entry in paramMap.entries) {
        when (entry.value) {
            null -> {
                output[FormFieldId.get(addPathKey(path, entry.key))] = null
            }
            is String -> {
                output[FormFieldId.get(addPathKey(path, entry.key))] = entry.value as String
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
