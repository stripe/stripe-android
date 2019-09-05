package com.stripe.samplestore

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.samplestore.service.SampleStoreEphemeralKeyProvider
import io.reactivex.disposables.CompositeDisposable

class StoreActivity : AppCompatActivity(), StoreAdapter.TotalItemsChangedListener {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var goToCartButton: FloatingActionButton
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var ephemeralKeyProvider: SampleStoreEphemeralKeyProvider

    private val priceMultiplier: Float
        get() {
            return try {
                packageManager
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    .metaData
                    .getFloat("com.stripe.samplestore.price_multiplier")
            } catch (e: PackageManager.NameNotFoundException) {
                1.0f
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        PaymentConfiguration.init(Settings.PUBLISHABLE_KEY)
        goToCartButton = findViewById(R.id.fab_checkout)
        storeAdapter = StoreAdapter(this, priceMultiplier)

        goToCartButton.hide()
        setSupportActionBar(findViewById(R.id.my_toolbar))

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_store_items)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.addItemDecoration(ItemDivider(this, R.drawable.item_divider))
        recyclerView.adapter = storeAdapter

        goToCartButton.setOnClickListener { storeAdapter.launchPurchaseActivityWithCart() }
        setupCustomerSession(Settings.STRIPE_ACCOUNT_ID)
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        ephemeralKeyProvider.destroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PURCHASE_REQUEST &&
            resultCode == Activity.RESULT_OK &&
            data!!.extras != null) {
            val price = data.extras!!.getLong(EXTRA_PRICE_PAID, -1L)
            if (price != -1L) {
                displayPurchase(price)
            } else {
                displaySetupComplete()
            }
            storeAdapter.clearItemSelections()
        }
    }

    override fun onTotalItemsChanged(totalItems: Int) {
        if (totalItems > 0) {
            goToCartButton.show()
        } else {
            goToCartButton.hide()
        }
    }

    private fun displayPurchase(price: Long) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.purchase_complete_notification, null)

        val emojiView = dialogView.findViewById<TextView>(R.id.dlg_emoji_display)
        // Show a smiley face!
        emojiView.text = StoreUtils.getEmojiByUnicode(0x1F642)

        val priceView = dialogView.findViewById<TextView>(R.id.dlg_price_display)
        priceView.text = StoreUtils.getPriceString(price, null)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            .show()
    }

    private fun displaySetupComplete() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.setup_complete_notification, null)

        val emojiView = dialogView.findViewById<TextView>(R.id.dlg_emoji_display)
        // Show a smiley face!
        emojiView.text = StoreUtils.getEmojiByUnicode(0x1F642)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            .show()
    }

    private fun setupCustomerSession(stripeAccountId: String?) {
        // CustomerSession only needs to be initialized once per app.
        ephemeralKeyProvider = SampleStoreEphemeralKeyProvider(stripeAccountId)
        CustomerSession.initCustomerSession(this, ephemeralKeyProvider, stripeAccountId)
    }

    companion object {
        internal const val PURCHASE_REQUEST = 37

        private const val EXTRA_PRICE_PAID = "EXTRA_PRICE_PAID"

        fun createPurchaseCompleteIntent(amount: Long): Intent {
            return Intent()
                .putExtra(EXTRA_PRICE_PAID, amount)
        }
    }
}
