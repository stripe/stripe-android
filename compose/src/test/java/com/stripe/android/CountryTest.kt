package com.stripe.android

import com.stripe.android.compose.elements.country.CountryConfig
import org.junit.Test

class CountryTest {
    private val country = CountryConfig()

    @Test
    fun test(){
        country.debugLabel
    }

}