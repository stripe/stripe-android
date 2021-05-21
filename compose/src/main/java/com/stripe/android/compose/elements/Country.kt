package com.stripe.android.compose.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.model.CountryUtils
import compose.R

class Country : ConfigInterface {
    override val debugLabel = "country"
    override val label = com.stripe.android.R.string.address_label_country
    override val keyboard = KeyboardType.Ascii

    override fun filter(userTyped: String) =
        userTyped.filter { Character.isLetterOrDigit(it) || Character.isSpaceChar(it) }

    override fun determineState(displayFormatted: String): ElementState {
        return when {
            displayFormatted.isEmpty() -> Error.BlankAndRequired
            COUNTRIES.contains(displayFormatted) -> Valid.Full
            else -> Error.Malformed
        }
    }

    override fun shouldShowError(elementState: ElementState, hasFocus: Boolean) =
        when (elementState) {
            is Error -> {
                when (elementState) {
                    Error.Incomplete -> !hasFocus
                    is Error.Malformed -> !hasFocus
                    Error.BlankAndRequired -> false
                }
            }
            is Valid -> false
            else -> false
        }

    fun getCountries(): List<String> = COUNTRIES

    companion object {
        // TODO: Need to determine the correct way to pass junit default locale
        val COUNTRIES: List<String> =
            CountryUtils.getOrderedCountries(java.util.Locale.getDefault()).map {
                it.name
            }

        sealed class Valid : ElementState.ElementStateValid() {
            object Full : Valid() {
                override fun isFull() = true
            }
        }

        sealed class Error(stringResId: Int) : ElementState.ElementStateError(stringResId) {
            object Incomplete : Error(R.string.incomplete)
            object Malformed : Error(R.string.malformed)
            object BlankAndRequired : Error(R.string.blank_and_required)
        }
    }
}
