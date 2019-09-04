package com.stripe.android.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.stripe.android.R
import java.util.Locale

/**
 * Adapter that populates a list of countries for a spinner.
 */
internal class CountryAdapter(
    context: Context,
    private val initialCountries: List<String>
) : ArrayAdapter<String>(context, R.layout.country_text_view) {
    private val countryFilter: Filter = createFilter()
    private var suggestions: List<String>? = initialCountries

    override fun getCount(): Int {
        return suggestions?.size ?: 0
    }

    override fun getItem(i: Int): String? {
        return suggestions?.get(i)
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
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

    private fun createFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                filterResults.values = constraint?.let {
                    filteredSuggestedCountries(constraint)
                } ?: this@CountryAdapter.initialCountries
                return filterResults
            }

            override fun publishResults(
                constraint: CharSequence?,
                filterResults: FilterResults?
            ) {
                suggestions = filterResults?.values as List<String>
                notifyDataSetChanged()
            }

            private fun filteredSuggestedCountries(constraint: CharSequence?): List<String> {
                val suggestedCountries = this@CountryAdapter.initialCountries
                    .filter {
                        it.toLowerCase(Locale.ROOT).startsWith(
                            constraint.toString().toLowerCase(Locale.ROOT)
                        )
                    }

                return if (suggestedCountries.isEmpty() ||
                    (suggestedCountries.size == 1 &&
                        suggestedCountries[0] == constraint.toString())) {
                    this@CountryAdapter.initialCountries
                } else {
                    suggestedCountries
                }
            }
        }
    }
}
