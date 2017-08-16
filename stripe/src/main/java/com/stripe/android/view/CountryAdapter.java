package com.stripe.android.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.stripe.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Adapter that populates a list of countries for a spinner.
 */
class CountryAdapter extends BaseAdapter implements SpinnerAdapter {

    private List<Pair<String, String>> mCountryToDisplayName;
    private Context mContext;

    public CountryAdapter(Context context) {
        mContext = context;
        mCountryToDisplayName = getOrderedCountries();
    }

    @Override
    public int getCount() {
        return mCountryToDisplayName.size();
    }

    @Override
    public Pair<String, String> getItem(int i) {
        return mCountryToDisplayName.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view != null && view instanceof TextView) {
            ((TextView) view).setText(getItem(i).second);
            return view;
        } else {
            TextView countryText = (TextView) LayoutInflater.from(mContext).inflate(R.layout.spinner_text_view, viewGroup, false);
            countryText.setText(getItem(i).second);
            return countryText;
        }
    }

    @VisibleForTesting
     List getOrderedCountries() {
        // Show user's current locale first, followed by countries alphabetized by display name
        List<String> countries = new ArrayList<>(Arrays.asList(Locale.getISOCountries()));
        List<Pair<String, String>> countriesToDisplayName = new ArrayList<>();
        for (String country: countries) {
            Locale locale = new Locale("", country);
            countriesToDisplayName.add(new Pair<>(country, locale.getDisplayCountry()));
        }
        Collections.sort(countriesToDisplayName, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> pair1, Pair<String, String> pair2) {
                return pair1.second.compareTo(pair2.second);
            }
        });

        Pair<String, String> currentCountryToDisplay = new Pair<>(getCurrentLocale().getCountry(), getCurrentLocale().getDisplayCountry());
        if (countriesToDisplayName.remove(currentCountryToDisplay)) {
            countriesToDisplayName.add(0, currentCountryToDisplay);
        }
        return countriesToDisplayName;
    }

    @VisibleForTesting
    Locale getCurrentLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return mContext.getResources().getConfiguration().getLocales().get(0);
        } else {
            return mContext.getResources().getConfiguration().locale;
        }
    }
}
