package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun List<SectionFieldElement>.filterOutHiddenIdentifiers(
    hiddenIdentifiers: Set<FormFieldId>,
) = filterNot { field ->
    field.isHidden(hiddenIdentifiers)
}

private fun SectionFieldElement.isHidden(
    hiddenIdentifiers: Set<FormFieldId>,
): Boolean {
    return when (this) {
        is RowElement -> fields.all { field ->
            field.isHidden(hiddenIdentifiers)
        }
        else -> hiddenIdentifiers.contains(identifier)
    }
}
