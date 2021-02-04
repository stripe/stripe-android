package com.stripe.example.activity

import android.content.Context
import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.stripe.example.R
import com.stripe.example.paymentsheet.PaymentSheetViewModel

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    protected val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application,
            getPreferences(Context.MODE_PRIVATE)
        )
    }

    private val prefsManager: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    protected val merchantName: String
        get() = prefsManager.getString("merchant_name", null) ?: "Widget Store"

    protected val isCustomerEnabled: Boolean
        get() = prefsManager.getBoolean("enable_customer", true)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.payment_sheet, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.config) {
            PaymentSheetConfigBottomSheet().show(
                supportFragmentManager,
                PaymentSheetConfigBottomSheet.TAG
            )
            return true
        } else if (item.itemId == R.id.refresh_key) {
            viewModel.clearKeys()
            onRefreshEphemeralKey()
        }
        return super.onOptionsItemSelected(item)
    }

    abstract fun onRefreshEphemeralKey()
}
