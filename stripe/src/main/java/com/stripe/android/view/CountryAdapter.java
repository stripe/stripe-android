package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.os.ConfigurationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.stripe.android.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that populates a list of countries for a spinner.
 */
class CountryAdapter extends ArrayAdapter {
    @NonNull private final Context mContext;
    @NonNull private final List<String> mCountries;
    @NonNull private final Filter mFilter;

    private List<String> mSuggestions;

    CountryAdapter(@NonNull Context context, @NonNull List<String> countries) {
        super(context, R.layout.menu_text_view);
        mContext = context;
        mCountries = getOrderedCountries(countries);
        mSuggestions = mCountries;
        mFilter = new Filter() {
            @NonNull
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                final FilterResults filterResults = new FilterResults();
                final List<String> suggestedCountries = new ArrayList<>();
                if (charSequence == null) {
                    filterResults.values = mCountries;
                    return filterResults;
                }
                final String charSequenceLowercase = charSequence.toString()
                        .toLowerCase(Locale.ROOT);
                for (String country : mCountries) {
                    if (country.toLowerCase(Locale.ROOT).startsWith(charSequenceLowercase)) {
                        suggestedCountries.add(country);
                    }
                }
                if (suggestedCountries.size() == 0 || (suggestedCountries.size() == 1 &&
                        suggestedCountries.get(0).equals(charSequence.toString()))) {
                    filterResults.values = mCountries;
                } else {
                    filterResults.values = suggestedCountries;
                }
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mSuggestions = (List<String>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getCount() {
        return mSuggestions.size();
    }

    @Override
    public String getItem(int i) {
        return mSuggestions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
        if (view instanceof TextView) {
            ((TextView) view).setText(getItem(i));
            return view;
        } else {
            final TextView countryText = (TextView) LayoutInflater.from(mContext).inflate(
                    R.layout.menu_text_view, viewGroup, false);
            countryText.setText(getItem(i));
            return countryText;
        }
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    @NonNull
    private List<String> getOrderedCountries(@NonNull List<String> countries) {
        // Show user's current locale first, followed by countries alphabetized by display name
        Collections.sort(countries, new Comparator<String>() {
            @Override
            public int compare(String country1, String country2) {
                return country1.toLowerCase(Locale.ROOT)
                        .compareTo(country2.toLowerCase(Locale.ROOT));
            }
        });
        countries.remove(getCurrentLocale().getDisplayCountry());
        countries.add(0, getCurrentLocale().getDisplayCountry());
        return countries;
    }

    @VisibleForTesting
    Locale getCurrentLocale() {
        return ConfigurationCompat.getLocales(mContext.getResources().getConfiguration()).get(0);
    }

}
