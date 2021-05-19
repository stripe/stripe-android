package com.stripe.android.compose.elements

class CountryElement(
    private val config: Country = Country(),
    val countries: List<String> = config.getCountries()
) : Element(config) {
    init {
        onValueChange(countries[0])
    }
}