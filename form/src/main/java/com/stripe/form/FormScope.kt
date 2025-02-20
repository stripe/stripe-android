package com.stripe.form

import androidx.compose.ui.text.input.TextFieldValue
import com.stripe.android.model.CardBrand
import com.stripe.form.fields.CheckBoxSpec
import com.stripe.form.fields.DropdownSpec
import com.stripe.form.fields.TextFieldSpec
import com.stripe.form.fields.card.CardDetailsSpec
import com.stripe.form.fields.card.CvcSpec
import com.stripe.form.fields.card.CvcValidator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FormScope(
    private val onValuesChanged: (Map<Key<*>, ValueChange<*>?>) -> Unit
) {
    private val fields = mutableListOf<ContentSpec>()

    private var values = mapOf<Key<*>, ValueChange<*>>()

    fun addSpec(
        onCreate: ((ValueChange<*>) -> Unit) -> ContentSpec
    ): ContentSpec {
        val spec = onCreate(::updateValue)
        fields.add(spec)
        return spec
    }

    fun textField(
        key: Key<TextFieldValue>,
        initialValue: String? = null,
        label: ContentSpec? = null,
    ): ContentSpec {
        val spec = TextFieldSpec(
            state = TextFieldSpec.TextFieldState(
                key = key,
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
        key: Key<CardDetailsSpec.Output>,
        cardNumber: String = "",
        expiryDate: String = "",
        cvc: String = "",
    ): ContentSpec {
        val spec = CardDetailsSpec(
            state = CardDetailsSpec.State(
                key = key,
                cardNumber = cardNumber,
                expiryDate = expiryDate,
                cvc = cvc,
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

    fun checkbox(
        key: Key<Boolean>,
        initialValue: Boolean = false,
        label: ContentSpec? = null,
    ): ContentSpec {
        val spec = CheckBoxSpec(
            state = CheckBoxSpec.CheckBoxState(
                key = key,
                checked = initialValue,
                label = label,
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

    fun dropdown(
        key: Key<String>,
        options: ImmutableList<DropdownSpec.Option>,
        initialIndex: Int = 0,
    ): ContentSpec {
        val spec = DropdownSpec(
            state = DropdownSpec.State(
                key = key,
                options = options,
                initialIndex = initialIndex,
                onValueChange = ::updateValue
            )
        )
        fields.add(spec)
        return spec
    }

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
    onValuesChanged: (Map<Key<*>, ValueChange<*>?>) -> Unit,
    block: FormScope.() -> Unit
): Form {
    val formScope = FormScope(onValuesChanged).apply(block)
    return Form(formScope.content())
}