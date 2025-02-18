package com.stripe.form

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.input.TextFieldValue
import com.stripe.android.model.CardBrand
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.fields.card.CardDetailsSpec
import com.stripe.form.fields.card.CvcSpec
import com.stripe.form.fields.card.CvcValidator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FormScope(
    private val onValuesChanged: (Map<Any, Any?>) -> Unit
) {
    private val fields = mutableStateListOf<ContentSpec>()

    private var values = mapOf<Any, ValueChange<*>>()

    fun addSpec(
        onCreate: ((ValueChange<*>) -> Unit) -> ContentSpec
    ): ContentSpec {
        val spec = onCreate(::updateValue)
        fields.add(spec)
        return spec
    }

    fun textField(
        key: String,
        initialValue: String? = null,
        label: ContentSpec? = null,
    ): ContentSpec {
        val spec = TextFieldSpec(
            state = TextFieldSpec.TextFieldState(
                key = parcelableKey(key),
                label = label,
                initialValue = TextFieldValue(initialValue ?: ""),
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

    fun cvc(
        cardBrand: CardBrand,
        validator: Validator<String> = CvcValidator(cardBrand),
        initialValue: String = "",
    ): ContentSpec {
        val spec = CvcSpec(
            state = CvcSpec.State(
                cardBrand = cardBrand,
                initialValue = initialValue,
                validator = {
                    validator.validateResult(it)
                },
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

    fun cardDetails(
        cardNumber: String = "",
        expiryDate: String = "",
        cvc: String = "",
    ): ContentSpec {
        val spec = CardDetailsSpec(
            state = CardDetailsSpec.State(
                cardNumber = cardNumber,
                expiryDate = expiryDate,
                cvc = cvc,
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

//    fun checkboxField(
//        key: String,
//        initialValue: Boolean,
//        label: ContentSpec? = null,
//    ): ContentSpec {
//        val spec = CheckBoxSpec(
//            state = CheckBoxSpec.CheckBoxState(
//                key = parcelableKey(key),
//                checked = initialValue,
//                label = label,
//                onValueChange = ::updateValue
//            )
//        )
//        _fields.add(spec)
//        return spec
//    }
//
//    fun dropdownField(
//        key: String,
//        options: List<DropdownSpec.State.Option>,
//        initialIndex: Int? = null,
//        label: ContentSpec? = null,
//        placeholder: ContentSpec? = null,
//    ): ContentSpec {
//        val spec = DropdownSpec(
//            state = DropdownSpec.State(
//                key = parcelableKey(key),
//                options = options,
//                selectedIndex = initialIndex,
//                label = label,
//                placeholder = placeholder,
//                onValueChange = ::updateValue
//            )
//        )
//        _fields.add(spec)
//        return spec
//    }

    fun content(): ImmutableList<ContentSpec> {
        return fields.toImmutableList()
    }


    private fun updateValue(change: ValueChange<*>) {
        values = HashMap(values)
            .apply {
                put(change.key, change)
            }
        onValuesChanged(values)
    }
}

fun buildForm(
    onValuesChanged: (Map<Any, Any?>) -> Unit,
    block: FormScope.() -> Unit
): Form {
    val formScope = FormScope(onValuesChanged).apply(block)
    return Form(formScope.content())
}