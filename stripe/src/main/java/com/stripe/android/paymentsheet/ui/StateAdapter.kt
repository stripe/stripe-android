package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.stripe.android.R

internal class StateAdapter(
    context: Context,
    itemLayoutId: Int = R.layout.country_text_view,
    private val textViewFactory: (ViewGroup) -> TextView
) : ArrayAdapter<StateAdapter.State>(context, itemLayoutId) {

    override fun getCount(): Int = STATES.size

    override fun getItem(i: Int): State = STATES[i]

    override fun getItemId(i: Int): Long = getItem(i).hashCode().toLong()

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        return when (view) {
            is TextView -> view
            else -> textViewFactory(viewGroup)
        }.also { stateText ->
            stateText.text = getItem(i).name
        }
    }

    data class State(
        val code: String,
        val name: String
    )

    companion object {
        val STATES = listOf(
            State("AL", "Alabama"),
            State("AK", "Alaska"),
            State("AZ", "Arizona"),
            State("AR", "Arkansas"),
            State("CA", "California"),
            State("CO", "Colorado"),
            State("CT", "Connecticut"),
            State("DE", "Delaware"),
            State("DC", "District Of Columbia"),
            State("FL", "Florida"),
            State("GA", "Georgia"),
            State("HI", "Hawaii"),
            State("ID", "Idaho"),
            State("IL", "Illinois"),
            State("IN", "Indiana"),
            State("IA", "Iowa"),
            State("KS", "Kansas"),
            State("KY", "Kentucky"),
            State("LA", "Louisiana"),
            State("ME", "Maine"),
            State("MD", "Maryland"),
            State("MA", "Massachusetts"),
            State("MI", "Michigan"),
            State("MN", "Minnesota"),
            State("MS", "Mississippi"),
            State("MO", "Missouri"),
            State("MT", "Montana"),
            State("NE", "Nebraska"),
            State("NV", "Nevada"),
            State("NH", "New Hampshire"),
            State("NJ", "New Jersey"),
            State("NM", "New Mexico"),
            State("NY", "New York"),
            State("NC", "North Carolina"),
            State("ND", "North Dakota"),
            State("OH", "Ohio"),
            State("OK", "Oklahoma"),
            State("OR", "Oregon"),
            State("PA", "Pennsylvania"),
            State("RI", "Rhode Island"),
            State("SC", "South Carolina"),
            State("SD", "South Dakota"),
            State("TN", "Tennessee"),
            State("TX", "Texas"),
            State("UT", "Utah"),
            State("VT", "Vermont"),
            State("VA", "Virginia"),
            State("WA", "Washington"),
            State("WV", "West Virginia"),
            State("WI", "Wisconsin"),
            State("WY", "Wyoming"),
        )
    }
}
