package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.NameParam
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.core.R as CoreR

/**
 * Section to collect User's date of birth.
 */
@Composable
internal fun NameSection(
    enabled: Boolean,
    onNameCollected: (Resource<NameParam>) -> Unit
) {
    val firstNameController = remember {
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(R.string.stripe_first_name)
        )
    }
    val lastNameController = remember {
        SimpleTextFieldController(
            textFieldConfig = SimpleTextFieldConfig(R.string.stripe_last_name)
        )
    }
    val nameSectionElement = remember {
        SectionElement.wrap(
            sectionFieldElements = listOf(
                SimpleTextElement(
                    identifier = IdentifierSpec.Generic(FIRST_NAME_SPEC),
                    controller = firstNameController
                ),
                SimpleTextElement(
                    identifier = IdentifierSpec.Generic(LAST_NAME_SPEC),
                    controller = lastNameController
                )
            ),
            label = CoreR.string.stripe_address_label_name
        )
    }

    val firstName by firstNameController.fieldValue.collectAsState()
    val lastName by lastNameController.fieldValue.collectAsState()

    SectionElementUI(
        modifier = Modifier.padding(vertical = 8.dp),
        enabled = enabled,
        element = nameSectionElement,
        hiddenIdentifiers = setOf(),
        lastTextFieldIdentifier = null,
    )

    LaunchedEffect(firstName, lastName) {
        onNameCollected(
            if (firstName.isNotBlank() && lastName.isNotBlank()) {
                Resource.success(NameParam(firstName, lastName))
            } else {
                Resource.error()
            }
        )
    }
}

internal const val FIRST_NAME_SPEC = "FirstNameSpec"
internal const val LAST_NAME_SPEC = "LastNameSpec"
