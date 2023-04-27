package com.stripe.android.view

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.databinding.StripeCardBrandSpinnerDropdownBinding
import com.stripe.android.databinding.StripeCardBrandSpinnerMainBinding
import com.stripe.android.model.CardBrand
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
                CardBrand.Visa,
                CardBrand.MasterCard
            )
        )

        val parentView = FrameLayout(context)
        val arrayAdapter = spinner.adapter as ArrayAdapter<CardBrand>
        val viewBinding = StripeCardBrandSpinnerMainBinding.bind(
            arrayAdapter.getView(0, null, parentView)
        )
        assertThat(viewBinding.root.contentDescription)
            .isEqualTo("Visa")

        assertThat(
            StripeCardBrandSpinnerDropdownBinding.bind(
                arrayAdapter.getDropDownView(0, null, parentView)
            ).textView.text
        ).isEqualTo("Visa")

        assertThat(
            StripeCardBrandSpinnerDropdownBinding.bind(
                arrayAdapter.getDropDownView(1, null, parentView)
            ).textView.text
        ).isEqualTo("Mastercard")
    }

    @Test
    fun cardBrand_shouldReturnSelectedCardBrand() {
        spinner.setCardBrands(
            listOf(
                CardBrand.Visa,
                CardBrand.MasterCard
            )
        )

        assertThat(spinner.cardBrand)
            .isEqualTo(CardBrand.Visa)
    }
}
