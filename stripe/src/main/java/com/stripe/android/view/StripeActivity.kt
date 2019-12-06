package com.stripe.android.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.stripe.android.R

/**
 * Provides a toolbar, save button, and loading states for the save button.
 */
abstract class StripeActivity : AppCompatActivity() {

    lateinit var progressBar: ProgressBar
    lateinit var viewStub: ViewStub

    private var communicating: Boolean = false

    internal lateinit var alertDisplayer: AlertDisplayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe)
        progressBar = findViewById(R.id.progress_bar_as)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_as)
        viewStub = findViewById(R.id.widget_viewstub_as)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setCommunicatingProgress(false)
        alertDisplayer = AlertDisplayer.DefaultAlertDisplayer(this)
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
            R.drawable.stripe_ic_checkmark)
        saveItem.icon = tintedIcon
        return super.onPrepareOptionsMenu(menu)
    }

    protected abstract fun onActionSave()

    protected open fun setCommunicatingProgress(communicating: Boolean) {
        this.communicating = communicating
        progressBar.visibility = if (communicating) {
            View.VISIBLE
        } else {
            View.GONE
        }
        invalidateOptionsMenu()
    }

    protected fun showError(error: String) {
        alertDisplayer.show(error)
    }
}
