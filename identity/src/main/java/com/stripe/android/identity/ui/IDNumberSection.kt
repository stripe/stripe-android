package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stripe.android.core.model.Country
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.CountryNotListedDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.IdNumberParam
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.utils.collectAsState

/**
 * Section to collect User's ID number from different countries.
 * Only US, BR and SG are supported.
 */
@Composable
internal fun IDNumberSection(
    enabled: Boolean,
    idNumberCountries: List<Country>,
    countryNotListedText: String,
    navController: NavController,
    onIdNumberCollected: (Resource<IdNumberParam>) -> Unit
) {
    val controller = remember {
        DropdownFieldController(
            CountryConfig(
                onlyShowCountryCodes = idNumberCountries.map { it.code.value }.toSet(),
                disableDropdownWithSingleElement = true
            )
        )
    }
    val countryElement = remember { CountryElement(IdentifierSpec.Country, controller) }
    val usElement = remember {
        SimpleTextElement(
            identifier = US_SPEC,
            controller = SimpleTextFieldController(textFieldConfig = USIDConfig)
        )
    }
    val usId by usElement.controller.fieldValue.collectAsState()
    val sgElement = remember {
        SimpleTextElement(
            identifier = SINGAPORE_SPEC,
            controller = SimpleTextFieldController(textFieldConfig = SGIDConfig)
        )
    }
    val sgId by sgElement.controller.fieldValue.collectAsState()
    val brElement = remember {
        SimpleTextElement(
            identifier = BRAZIL_SPEC,
            controller = SimpleTextFieldController(textFieldConfig = BRIDConfig)
        )
    }
    val brId by brElement.controller.fieldValue.collectAsState()
    val selectedCountryCode by controller.rawFieldValue.collectAsState()
    val idNumberParam: IdNumberParam? by remember(usId, sgId, brId) {
        derivedStateOf {
            when (selectedCountryCode) {
                US_CODE -> {
                    if (usId.length == 4) {
                        IdNumberParam(country = US_CODE, partialValue = usId)
                    } else {
                        null
                    }
                }
                SINGAPORE_CODE -> {
                    if (sgId.isNotBlank()) {
                        IdNumberParam(country = SINGAPORE_CODE, value = sgId)
                    } else {
                        null
                    }
                }
                BRAZIL_CODE -> {
                    if (brId.length == 11) {
                        IdNumberParam(country = BRAZIL_CODE, value = brId)
                    } else {
                        null
                    }
                }
                else -> {
                    null
                }
            }
        }
    }
    IDNumberContent(
        enabled = enabled,
        navController = navController,
        countryElement = countryElement,
        usElement = usElement,
        sgElement = sgElement,
        brElement = brElement,
        idNumberParam = idNumberParam,
        selectedCountryCode = selectedCountryCode,
        countryNotListedText = countryNotListedText,
        onIdNumberCollected = onIdNumberCollected
    )
}

@Composable
private fun IDNumberContent(
    enabled: Boolean,
    navController: NavController,
    countryElement: CountryElement,
    usElement: SimpleTextElement,
    sgElement: SimpleTextElement,
    brElement: SimpleTextElement,
    idNumberParam: IdNumberParam?,
    selectedCountryCode: String?,
    countryNotListedText: String,
    onIdNumberCollected: (Resource<IdNumberParam>) -> Unit
) {
    val idNumberSectionElement = remember(selectedCountryCode) {
        SectionElement.wrap(
            sectionFieldElements = listOf(
                countryElement,
                when (selectedCountryCode) {
                    US_CODE -> usElement
                    SINGAPORE_CODE -> sgElement
                    BRAZIL_CODE -> brElement
                    else -> {
                        throw IllegalArgumentException("unexpected country code: $selectedCountryCode")
                    }
                }
            ),
            label = R.string.stripe_id_number
        )
    }
    val textIdentifiers by idNumberSectionElement.getTextFieldIdentifiers()
        .collectAsState()

    LaunchedEffect(idNumberParam) {
        onIdNumberCollected(
            idNumberParam?.let {
                Resource.success(it)
            } ?: run {
                Resource.error()
            }
        )
    }

    SectionElementUI(
        modifier = Modifier.padding(vertical = 8.dp),
        enabled = enabled,
        element = idNumberSectionElement,
        hiddenIdentifiers = emptySet(),
        lastTextFieldIdentifier = textIdentifiers.lastOrNull(),
    )

    TextButton(
        modifier = Modifier.testTag(ID_NUMBER_COUNTRY_NOT_LISTED_BUTTON_TAG),
        contentPadding = PaddingValues(0.dp),
        onClick = {
            navController.navigateTo(
                CountryNotListedDestination(
                    isMissingId = true
                )
            )
        }
    ) {
        Text(
            text = countryNotListedText,
            style = MaterialTheme.typography.h6
        )
    }
}

private object USIDConfig : SimpleTextFieldConfig(
    label = R.string.stripe_last_4_of_ssn
) {
    override val placeHolder = US_ID_PLACEHOLDER
    override val keyboard = KeyboardType.Number
    override val visualTransformation = Last4SSNTransformation
    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = !hasFocus && input.length < 4

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getError(): FieldError = FieldError(R.string.stripe_incomplete_id_number)

        override fun isFull(): Boolean = input.length == 4

        override fun isBlank(): Boolean = input.isBlank()
    }
}

private object BRIDConfig : SimpleTextFieldConfig(
    label = R.string.stripe_individual_cpf
) {
    override val placeHolder = BRAZIL_ID_PLACEHOLDER
    override val keyboard = KeyboardType.Number
    override val visualTransformation = BRVisualTransformation
    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = !hasFocus && input.length < 11

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getError(): FieldError = FieldError(R.string.stripe_incomplete_id_number)

        override fun isFull(): Boolean = input.length == 11

        override fun isBlank(): Boolean = input.isBlank()
    }
}

private object SGIDConfig : SimpleTextFieldConfig(
    label = R.string.stripe_nric_or_fin
) {
    override val placeHolder = SINGAPORE_ID_PLACEHOLDER
    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = false

        override fun isValid(): Boolean = input.isNotBlank()

        override fun getError() = null

        override fun isFull(): Boolean = false

        override fun isBlank(): Boolean = input.isBlank()
    }
}

private object Last4SSNTransformation : VisualTransformation {
    private fun last4SSNTranslator() = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return offset + US_ID_PLACEHOLDER_PREFIX.length
        }

        override fun transformedToOriginal(offset: Int): Int {
            return if (offset >= US_ID_PLACEHOLDER_PREFIX.length) {
                offset - US_ID_PLACEHOLDER_PREFIX.length
            } else {
                0
            }
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        // prepend with US_ID_PLACEHOLDER_PREFIX
        return TransformedText(
            AnnotatedString(US_ID_PLACEHOLDER_PREFIX + text),
            last4SSNTranslator()
        )
    }
}

private object BRVisualTransformation : VisualTransformation {
    private fun sgTranslator() = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            if (offset <= 2) return offset
            if (offset <= 5) return offset + 1
            if (offset <= 8) return offset + 2
            if (offset <= 11) return offset + 3
            return 14
        }

        override fun transformedToOriginal(offset: Int): Int {
            if (offset <= 3) return offset
            if (offset <= 7) return offset - 1
            if (offset <= 11) return offset - 2
            if (offset <= 14) return offset - 3
            return 11
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        // transform to 000.000.000-00
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 5) {
                out += '.'
            }
            if (i == 8) {
                out += '-'
            }
        }
        return TransformedText(AnnotatedString(out), sgTranslator())
    }
}

internal const val US_CODE = "US"
internal val US_SPEC = IdentifierSpec.Generic("USSpec")
internal const val US_ID_PLACEHOLDER = "***-**-1234"
internal const val US_ID_PLACEHOLDER_PREFIX = "***-**-"
internal const val SINGAPORE_CODE = "SG"
internal const val BRAZIL_ID_PLACEHOLDER = "000.000.000-00"
internal const val BRAZIL_CODE = "BR"
internal val BRAZIL_SPEC = IdentifierSpec.Generic("BRSpec")
internal const val SINGAPORE_ID_PLACEHOLDER = "S1234567A"
internal val SINGAPORE_SPEC = IdentifierSpec.Generic("SingaporeSpec")
internal const val ID_NUMBER_COUNTRY_NOT_LISTED_BUTTON_TAG = "IdNumberSectionCountryNotListed"
