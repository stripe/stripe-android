package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.stripe.android.getPaymentIntentResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.example.R
import kotlinx.coroutines.launch

class DumyActivity : StripeIntentActivity(), View.OnClickListener {


//    private lateinit var stripeObject : StripeSDK


//    private val stripe: Stripe by lazy {
//        Stripe(
//            this@DumyActivity,
//            Dependencies.getStripePublishableKey()
////        "pk_test_51IJXMhLCEaVJ86ftcrlQ6HblJWtukEq7wYh88HKD1acjiyqHqDz8mXnSOhJVTkYgiedNESzSrPr19IcefCDiUz5Q00FRBW37cz"
//        )
//    }

    lateinit var btnPay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ideal_test)


        btnPay = findViewById<Button>(R.id.ideal_button)


//        stripeObject = intent.extras?.get("stripe_object") as StripeSDK
//        Log.e("dumy",stripeObject.data.publishable)


//        PaymentConfiguration.init(
//            this,
//            stripeObject.data.publishable
////        "pk_test_51IJXMhLCEaVJ86ftcrlQ6HblJWtukEq7wYh88HKD1acjiyqHqDz8mXnSOhJVTkYgiedNESzSrPr19IcefCDiUz5Q00FRBW37cz"
//        )


        btnPay.setOnClickListener(this@DumyActivity)

        // print the status of the PI
        viewModel.status.observe(this) {
            Log.d("IDEALTest", it)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        Log.e(
            "dumyactivity",
            "result code of intent is --- $resultCode-----request code is --------$requestCode-----timestamp of intent is --- ${System.currentTimeMillis()}"
        )


        val bundle = data!!.extras


        Log.d("my-samples-bundle", bundle.toString())


        if (bundle != null) {
            for (key in bundle.keySet()) {
                var bundleVal: Any
                if (bundle.get(key)
                    != null
                ) {
                    bundleVal = bundle.get(key)!!
                } else {
                    bundleVal = "NULL"
                }
                Log.d("my-samples-bundle", key + " : " + bundleVal)
            }
        }


        Log.e("dumy", "this is data in the object 'data' of intent ---------- ${data?.extras}")


        if (stripe.isPaymentResult(requestCode, data)) {


            val paymentBundle = data!!.extras;


            Log.d(
                "my-samples-bundle",
                "inside stripe.isPaymentResult() ${paymentBundle.toString()} ---------timestamp is ---------${System.currentTimeMillis()}"
            )


            val gson = Gson()
            val jsonData = gson.toJson(data)
            val jsonExtras = gson.toJson(data!!.extras)
            val jsonRequestCode = gson.toJson(requestCode)
            val jsonResultCode = gson.toJson(resultCode)


            Log.d("my-samples-onAcRes", jsonData)
            Log.d("my-samples-onAcRes", jsonExtras)
            Log.d("my-samples-onAcRes", jsonRequestCode)
            Log.d("my-samples-onAcRes", jsonResultCode)


            lifecycleScope.launch {
                runCatching {


                    Log.d("my-samples", data.toString())
                    Log.d("my-samples", data!!.extras.toString())
                    Log.d("my-samples", requestCode.toString())


                    stripe.getPaymentIntentResult(requestCode, data!!).intent
                }.fold(
                    onSuccess = { paymentIntent ->
                        val status = paymentIntent.status
                        Log.e(
                            "dumy",
                            "timestamp inside lifecycle.fold() is --------- ${System.currentTimeMillis()}"
                        )
                        Log.e("dumy", "this is payment intent --------- $paymentIntent")
                        Log.e(
                            "dumy",
                            "this is status of payment intent --------- ${paymentIntent.status.toString()}"
                        )


                        when (status) {
                            StripeIntent.Status.Processing -> {
                                // Payment authorized
                                Log.e("dumy", "payment processing----------")
                                snackBar("INSIDE PROCESSING")
                            }
                            StripeIntent.Status.Canceled -> {
                                Log.e("dumy", "payment cancelled----------")
                                snackBar("INSIDE PAYMENT CANCELLED")
                            }
                            StripeIntent.Status.Succeeded -> {
                                Log.e("dumy", "payment success----------")
                                snackBar("INSIDE PAYMENT SUCCEEDED")
                                val intent = Intent()
                                intent.putExtra("is_success", true)
                                setResult(RESULT_OK, intent)
                            }
                            StripeIntent.Status.RequiresAction -> {
                                Log.e("dumy", "payment required action----------")
                                snackBar("INSIDE PAYMENT REQUIRES ACTION")
                            }
                            else -> {
                                // Payment failed/cancelled
                                Log.e("dumy", "api success but payment failed----------")
                                snackBar("INSIDE PAYMENT FAILED")

                            }
                        }
                    },
                    onFailure = {
                        // Payment failed
                        Log.e("dumy", "-payment failed----------")
                    }
                )
            }
        }
    }

    fun snackBar(text: String) {
        val snack = Snackbar.make(btnPay, text, Snackbar.LENGTH_SHORT)
        snack.show()

    }


    override fun onClick(view: View?) {
        if (view?.id == R.id.ideal_button) {
            val idealPaymentMethodParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Ideal("abn_amro"),
                PaymentMethod.BillingDetails(name = "Jenny Rosen")
            )

// Create a new payment intent with your test secret key in command line and copy the "client_secret" here
// curl 'https://api.stripe.com/v1/payment_intents' -u sk_test_your_test_secret_key: -d payment_method_types[]=ideal -d payment_method_data[type]=ideal -d confirm=true -d currency=eur -d amount=1099 -d return_url=http://stripe.com
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                idealPaymentMethodParams,
                "pi_xxxxxx_secret_xxxxxx" // paste "client_scret" of the PI just created
            )
            Log.e(
                "dumy",
                "calling stripe.confirmPayment(), timestamp is -----------${System.currentTimeMillis()}"
            )
            stripe.confirmPayment(this, confirmParams)
        }
    }
}