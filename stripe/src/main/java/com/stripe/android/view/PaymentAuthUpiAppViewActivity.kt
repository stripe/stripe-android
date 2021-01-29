package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthUpiAppViewStarter
import com.stripe.android.R
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION
import java.nio.charset.Charset

class PaymentAuthUpiAppViewActivity : AppCompatActivity() {

    private val _args: PaymentAuthUpiAppViewStarter.Args? by lazy {
        intent.getParcelableExtra(PaymentAuthUpiAppViewStarter.EXTRA_ARGS)
    }

    private val logger: Logger by lazy {
        Logger.getInstance(_args?.enableLogging == true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = _args
        if (args == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        logger.debug("PaymentAuthUpiAppViewActivity#onCreate()")

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))


        val clientSecret = args.clientSecret

        if (clientSecret.isBlank()) {
            logger.debug("PaymentAuthAppViewActivity#onCreate() - clientSecret is blank")
            finish()
            return
        }

        logger.debug("PaymentAuthUpiAppViewActivity#onCreate() - PaymentAuthUpiAppView init and loadUrl")

        val nativeData = args.nativeData
        val decodedNativeData = Base64.decode(nativeData,0)
        val nativeDataString = decodedNativeData.toString(Charset.forName("utf-8"))

        val upiPayIntent = Intent(Intent.ACTION_VIEW)
        upiPayIntent.data = Uri.parse(nativeDataString)

        val chooser = Intent.createChooser(upiPayIntent, "Pay with")

        if (null != chooser.resolveActivity(packageManager)) {
            startActivityForResult(chooser, 0)
        } else {
            print("error")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        logger.debug("PaymentAuthUpiAppViewActivity#onCreateOptionsMenu()")
        menuInflater.inflate(R.menu.payment_auth_web_view_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logger.debug("PaymentAuthUpiAppViewActivity#onOptionsItemSelected()")
        if (item.itemId == R.id.action_close) {
            cancelIntentSource()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun cancelIntentSource() {
        finish()
    }
}
