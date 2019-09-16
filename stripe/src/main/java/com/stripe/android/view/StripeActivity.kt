package com.stripe.android.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.stripe.android.CustomerSession
import com.stripe.android.R
import com.stripe.android.exception.StripeException

/**
 * Provides a toolbar, save button, and loading states for the save button.
 */
internal abstract class StripeActivity : AppCompatActivity() {

    lateinit var progressBar: ProgressBar
    lateinit var viewStub: ViewStub

    private lateinit var alertBroadcastReceiver: BroadcastReceiver

    private var alertMessageListener: AlertMessageListener? = null
    private var communicating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe)
        progressBar = findViewById(R.id.progress_bar_as)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_as)
        viewStub = findViewById(R.id.widget_viewstub_as)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setCommunicatingProgress(false)
        alertBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val exception =
                    intent.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) as StripeException
                showError(exception.localizedMessage)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(alertBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                alertBroadcastReceiver,
                IntentFilter(CustomerSession.ACTION_API_EXCEPTION)
            )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_payment_method, menu)
        menu.findItem(R.id.action_save).isEnabled = !communicating
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_save) {
            onActionSave()
            true
        } else {
            val handled = super.onOptionsItemSelected(item)
            if (!handled) {
                onBackPressed()
            }
            handled
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val saveItem = menu.findItem(R.id.action_save)
        val tintedIcon = StripeColorUtils(this).getTintedIconWithAttribute(
            theme,
            R.attr.titleTextColor,
            R.drawable.ic_checkmark)
        saveItem.icon = tintedIcon
        return super.onPrepareOptionsMenu(menu)
    }

    protected abstract fun onActionSave()

    protected open fun setCommunicatingProgress(communicating: Boolean) {
        this.communicating = communicating
        if (communicating) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }
        supportInvalidateOptionsMenu()
    }

    fun setAlertMessageListener(listener: AlertMessageListener?) {
        alertMessageListener = listener
    }

    fun showError(error: String) {
        alertMessageListener?.onAlertMessageDisplayed(error)

        AlertDialog.Builder(this)
            .setMessage(error)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) {
                dialogInterface, _ -> dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    internal interface AlertMessageListener {
        fun onAlertMessageDisplayed(message: String)
    }
}
