package com.stripe.android.identity.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.material.composethemeadapter.MdcTheme

class EntryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MdcTheme {
                val context = LocalContext.current
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        startActivity(Intent(context, ThemeActivity::class.java))
                    }) {
                        Text(stringResource(id = R.string.classic_example))
                    }
                    Button(onClick = {
                        startActivity(Intent(context, ComposeThemeActivity::class.java))
                    }) {
                        Text(stringResource(id = R.string.compose_example))
                    }
                }
            }
        }
    }
}
