package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.stripe.android.R
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Adapter that populates a list of countries for a spinner.
 */
internal class CountryAdapter(
    context: Context,
    private var unfilteredCountries: List<Country>
) : ArrayAdapter<Country>(context, R.layout.country_text_view) {
    private val countryFilter: CountryFilter = CountryFilter(
        unfilteredCountries,
        this,
        context as? Activity
    )
    private var suggestions: List<Country> = unfilteredCountries

    internal val firstItem: Country
        @JvmSynthetic
        get() {
            return getItem(0)
        }

    override fun getCount(): Int {
        return suggestions.size
    }

    override fun getItem(i: Int): Country {
        return suggestions[i]
    }

    override fun getItemId(i: Int): Long {
        return getItem(i).hashCode().toLong()
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return if (view is TextView) {
            view.text = getItem(i).name
            view
        } else {
            val countryText = LayoutInflater.from(context).inflate(
                R.layout.country_text_view, viewGroup, false) as TextView
            countryText.text = getItem(i).name
            countryText
        }
    }

    override fun getFilter(): Filter {
        return countryFilter
    }

    /**
     * @param allowedCountryCodes A set of allowed country codes. Will be ignored if empty.
     *
     * @return `true` if [unfilteredCountries] was updated, `false` otherwise
     */
    internal fun updateUnfilteredCountries(allowedCountryCodes: Set<String>): Boolean {
        if (allowedCountryCodes.isEmpty()) {
            return false
        }

        unfilteredCountries = unfilteredCountries.filter { (countryCode) ->
            allowedCountryCodes.any { allowedCountryCode ->
                allowedCountryCode.equals(countryCode, ignoreCase = true)
            }
        }
        countryFilter.unfilteredCountries = unfilteredCountries
        suggestions = unfilteredCountries
        notifyDataSetChanged()
        return true
    }

    private class CountryFilter(
        internal var unfilteredCountries: List<Country>,
        private val adapter: CountryAdapter,
        activity: Activity?
    ) : Filter() {
        private val activityRef = WeakReference(activity)

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            filterResults.values = constraint?.let {
                filteredSuggestedCountries(constraint)
            } ?: unfilteredCountries
            return filterResults
        }

        override fun publishResults(
            constraint: CharSequence?,
            filterResults: FilterResults?
        ) {
            val suggestions = filterResults?.values as List<Country>

            activityRef.get()?.let { activity ->
                if (suggestions.any { it.name == constraint }) {
                    hideKeyboard(activity)
                }
            }

            adapter.suggestions = suggestions
            adapter.notifyDataSetChanged()
        }

        private fun filteredSuggestedCountries(constraint: CharSequence?): List<Country> {
            val suggestedCountries = getSuggestedCountries(constraint)

            return if (suggestedCountries.isEmpty() || isMatch(suggestedCountries, constraint)) {
                unfilteredCountries
            } else {
                suggestedCountries
            }
        }

        private fun getSuggestedCountries(constraint: CharSequence?): List<Country> {
            return unfilteredCountries
                .filter {
                    it.name.toLowerCase(Locale.ROOT).startsWith(
                        constraint.toString().toLowerCase(Locale.ROOT)
                    )
                }
        }

        private fun isMatch(countries: List<Country>, constraint: CharSequence?): Boolean {
            return countries.size == 1 && countries[0].name == constraint.toString()
        }

        private fun hideKeyboard(activity: Activity) {
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (inputMethodManager?.isAcceptingText == true) {
                inputMethodManager.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
            }
        }
    }
}
