package com.stripe.android.view

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.support.v4.os.ConfigurationCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.stripe.android.R
import java.util.ArrayList
import java.util.Locale

/**
 * Adapter that populates a list of countries for a spinner.
 */
internal class CountryAdapter(
    context: Context,
    countries: List<String>
) : ArrayAdapter<String>(context, R.layout.menu_text_view) {
    private val countries: List<String>
    private val filter: Filter
    private var suggestions: List<String>? = null

    val currentLocale: Locale
        @VisibleForTesting
        get() = ConfigurationCompat.getLocales(context.resources.configuration).get(0)

    init {
        this.countries = getOrderedCountries(countries)
        suggestions = this.countries
        filter = object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val suggestedCountries = ArrayList<String>()
                if (charSequence == null) {
                    filterResults.values = this@CountryAdapter.countries
                    return filterResults
                }
                val charSequenceLowercase = charSequence.toString()
                    .toLowerCase(Locale.ROOT)
                for (country in this@CountryAdapter.countries) {
                    if (country.toLowerCase(Locale.ROOT).startsWith(charSequenceLowercase)) {
                        suggestedCountries.add(country)
                    }
                }
                if (suggestedCountries.size == 0 || suggestedCountries.size == 1 &&
                    suggestedCountries[0] == charSequence.toString()) {
                    filterResults.values = this@CountryAdapter.countries
                } else {
                    filterResults.values = suggestedCountries
                }
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                suggestions = filterResults.values as List<String>
                notifyDataSetChanged()
            }
        }
    }

    override fun getCount(): Int {
        return suggestions!!.size
    }

    override fun getItem(i: Int): String? {
        return suggestions!![i]
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
                R.layout.menu_text_view, viewGroup, false) as TextView
            countryText.text = getItem(i)
            countryText
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    private fun getOrderedCountries(countries: List<String>): List<String> {
        // Show user's current locale first, followed by countries alphabetized by display name
        return listOf(currentLocale.displayCountry)
            .plus(
                countries.sortedWith(compareBy { it.toLowerCase(Locale.ROOT) })
                    .minus(currentLocale.displayCountry)
            )
    }
}
