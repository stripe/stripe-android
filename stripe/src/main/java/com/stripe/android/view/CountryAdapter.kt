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
    initialCountries: List<String>
) : ArrayAdapter<String>(context, R.layout.country_text_view) {
    private val countryFilter: Filter = CountryFilter(
        initialCountries,
        this,
        context as? Activity
    )
    private var suggestions: List<String> = initialCountries

    override fun getCount(): Int {
        return suggestions.size
    }

    override fun getItem(i: Int): String {
        return suggestions[i]
    }

    override fun getItemId(i: Int): Long {
        return getItem(i).hashCode().toLong()
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return if (view is TextView) {
            view.text = getItem(i)
            view
        } else {
            val countryText = LayoutInflater.from(context).inflate(
                R.layout.country_text_view, viewGroup, false) as TextView
            countryText.text = getItem(i)
            countryText
        }
    }

    override fun getFilter(): Filter {
        return countryFilter
    }

    private class CountryFilter(
        private val initialCountries: List<String>,
        private val adapter: CountryAdapter,
        activity: Activity?
    ) : Filter() {
        private val activityRef = WeakReference(activity)

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            filterResults.values = constraint?.let {
                filteredSuggestedCountries(constraint)
            } ?: initialCountries
            return filterResults
        }

        override fun publishResults(
            constraint: CharSequence?,
            filterResults: FilterResults?
        ) {
            val suggestions = filterResults?.values as List<String>

            activityRef.get()?.let { activity ->
                if (suggestions.any { it == constraint }) {
                    hideKeyboard(activity)
                }
            }

            adapter.suggestions = suggestions
            adapter.notifyDataSetChanged()
        }

        private fun filteredSuggestedCountries(constraint: CharSequence?): List<String> {
            val suggestedCountries = getSuggestedCountries(constraint)

            return if (suggestedCountries.isEmpty() || isMatch(suggestedCountries, constraint)) {
                initialCountries
            } else {
                suggestedCountries
            }
        }

        private fun getSuggestedCountries(constraint: CharSequence?): List<String> {
            return initialCountries
                .filter {
                    it.toLowerCase(Locale.ROOT).startsWith(
                        constraint.toString().toLowerCase(Locale.ROOT)
                    )
                }
        }

        private fun isMatch(countries: List<String>, constraint: CharSequence?): Boolean {
            return countries.size == 1 && countries[0] == constraint.toString()
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
