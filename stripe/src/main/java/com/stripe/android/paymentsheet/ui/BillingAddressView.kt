package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.stripe.android.R
import com.stripe.android.databinding.StripeBillingAddressLayoutBinding
import com.stripe.android.view.CountryUtils

internal class BillingAddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val viewBinding = StripeBillingAddressLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    internal val countryView = viewBinding.country
    private val postalCodeView = viewBinding.postalCode

    init {
        orientation = VERTICAL

        ViewCompat.setBackground(
            this,
            MaterialShapeDrawable(
                ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(
                        CornerFamily.ROUNDED,
                        resources.getDimension(R.dimen.stripe_paymentsheet_form_corner_radius)
                    )
                    .build()
            ).also {
                it.setStroke(
                    resources.getDimension(R.dimen.stripe_paymentsheet_form_border_width),
                    ContextCompat.getColor(context, R.color.stripe_paymentsheet_form_border)
                )

                it.fillColor = ContextCompat.getColorStateList(context, android.R.color.transparent)
            }
        )

        configureCountryAutoComplete()
    }

    private fun configureCountryAutoComplete() {
        val countries = CountryUtils.getOrderedCountries(
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        ).map { it.name }

        val adapter = ArrayAdapter(context, R.layout.stripe_country_dropdown_item, countries)
        countryView.threshold = 0
        countryView.setAdapter(adapter)
        countryView.setText(countries.first())
    }
}
