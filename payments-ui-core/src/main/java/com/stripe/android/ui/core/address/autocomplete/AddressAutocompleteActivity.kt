package com.stripe.android.ui.core.address.autocomplete

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.stripe.android.ui.core.DefaultPaymentsTheme
import com.stripe.android.ui.core.elements.AddressAutocompleteTextField
import com.stripe.android.ui.core.elements.AddressAutocompleteTextFieldController

class AddressAutocompleteActivity : AppCompatActivity() {

    val args by lazy {
        requireNotNull(
            AddressAutocompleteContract.Args.fromIntent(intent)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DefaultPaymentsTheme {
                AddressAutocompleteScreen()
            }
        }
    }

    @Composable
    fun AddressAutocompleteScreen() {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AddressAutocompleteTextField(
                controller = AddressAutocompleteTextFieldController(
                    context = this@AddressAutocompleteActivity,
                    country = args.country,
                    googlePlacesApiKey = args.googlePlacesApiKey,
                    workerScope = lifecycleScope
                ),
            ) {
                val autoCompleteResult =
                    AddressAutocompleteResult.Succeeded(it)
                setResult(
                    autoCompleteResult.resultCode,
                    Intent().putExtras(autoCompleteResult.toBundle())
                )
                finish()
            }
        }
    }
}