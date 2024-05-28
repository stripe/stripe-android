package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R

class CardInputWidgetComposeExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val stateHolder = rememberSaveableStateHolder()

            var showCardInputWidget by remember {
                mutableStateOf(true)
            }

            StripeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(it)
                            .padding(16.dp)
                            .fillMaxSize(),
                        horizontalAlignment = CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Show Card Input Widget")
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = showCardInputWidget,
                                onCheckedChange = { checked ->
                                    showCardInputWidget = checked
                                }
                            )
                        }

                        if (showCardInputWidget) {
                            SaveableCardInputWidget(stateHolder)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SaveableCardInputWidget(
        stateHolder: SaveableStateHolder,
    ) {
        stateHolder.SaveableStateProvider("stripe_elements") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth(),
                        factory = { context ->
                            CardInputWidget(context).apply {
                                id = R.id.card_input_widget

                                setCardValidCallback { isValid, fields ->
                                    // Do something here
                                }

                                postalCodeRequired = true
                            }
                        },
                        update = {
                            // do nothing
                        },
                        onRelease = {
                        },
                    )
                }
            }
        }
    }
}
