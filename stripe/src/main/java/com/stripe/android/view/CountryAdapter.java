package com.stripe.android.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.ConfigurationCompat;

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

    @VisibleForTesting
    List<String> mCountries;
    @VisibleForTesting
    List<String> mSuggestions;
    private Filter mFilter;

    private Context mContext;

    CountryAdapter(Context context, List<String> countries) {
        super(context, R.layout.menu_text_view);
        mContext = context;
        mCountries = getOrderedCountries(countries);
        mSuggestions = mCountries;
        mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                List<String> suggestedCountries = new ArrayList<>();
                if (charSequence == null) {
                    filterResults.values = mCountries;
                    return filterResults;
                }
                String charSequenceLowercase = charSequence.toString().toLowerCase(Locale.ROOT);
                for (String country : mCountries) {
                    if (country.toLowerCase(Locale.ROOT).startsWith(charSequenceLowercase)) {
                        suggestedCountries.add(country);
                    }
                }
                if (suggestedCountries.size() == 0 || (suggestedCountries.size() == 1 &&
                        suggestedCountries.get(0).equals(charSequence))) {
                    suggestedCountries = mCountries;
                }
                filterResults.values = suggestedCountries;
                return filterResults;
            }

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

    @Override
    public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
        if (view instanceof TextView) {
            ((TextView) view).setText(getItem(i));
            return view;
        } else {
            TextView countryText = (TextView) LayoutInflater.from(mContext).inflate(
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

    @VisibleForTesting
    List<String> getOrderedCountries(List<String> countries) {
        // Show user's current locale first, followed by countries alphabetized by display name
        Collections.sort(countries, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.toLowerCase(Locale.ROOT).compareTo(t1.toLowerCase(Locale.ROOT));
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
