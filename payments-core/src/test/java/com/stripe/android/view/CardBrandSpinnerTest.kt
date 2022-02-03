package com.stripe.android.view

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.databinding.CardBrandSpinnerDropdownBinding
import com.stripe.android.databinding.CardBrandSpinnerMainBinding
import com.stripe.android.ui.core.elements.CardBrand
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CardBrandSpinnerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val spinner = CardBrandSpinner(context)

    @Test
    fun setCardBrands_shouldPopulatesViews() {
        spinner.setCardBrands(
            listOf(
                com.stripe.android.ui.core.elements.CardBrand.Visa,
                com.stripe.android.ui.core.elements.CardBrand.MasterCard
            )
        )

        val parentView = FrameLayout(context)
        val arrayAdapter = spinner.adapter as ArrayAdapter<com.stripe.android.ui.core.elements.CardBrand>
        val viewBinding = CardBrandSpinnerMainBinding.bind(
            arrayAdapter.getView(0, null, parentView)
        )
        assertThat(viewBinding.root.contentDescription)
            .isEqualTo("Visa")

        assertThat(
            CardBrandSpinnerDropdownBinding.bind(
                arrayAdapter.getDropDownView(0, null, parentView)
            ).textView.text
        ).isEqualTo("Visa")

        assertThat(
            CardBrandSpinnerDropdownBinding.bind(
                arrayAdapter.getDropDownView(1, null, parentView)
            ).textView.text
        ).isEqualTo("Mastercard")
    }

    @Test
    fun cardBrand_shouldReturnSelectedCardBrand() {
        spinner.setCardBrands(
            listOf(
                com.stripe.android.ui.core.elements.CardBrand.Visa,
                com.stripe.android.ui.core.elements.CardBrand.MasterCard
            )
        )

        assertThat(spinner.cardBrand)
            .isEqualTo(com.stripe.android.ui.core.elements.CardBrand.Visa)
    }
}
