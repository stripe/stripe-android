package com.stripe.form.example.ui.theme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.form.example.ui.theme.cardinput.CardInputScreen
import com.stripe.form.example.ui.theme.cardinput.CardInputViewModel
import com.stripe.form.example.ui.theme.customform.CustomFormScreen
import com.stripe.form.example.ui.theme.customform.CustomFormViewModel
import com.stripe.form.example.ui.theme.ui.theme.StripeandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StripeandroidTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "index"
                ) {
                    composable("index") {
                        IndexScreen(
                            navController = navController
                        )
                    }
                    composable("cardInput") {
                        val viewModel: CardInputViewModel = viewModel()
                        CardInputScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                    composable("customForm") {
                        val viewModel: CustomFormViewModel = viewModel()
                        CustomFormScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StripeandroidTheme {
        Greeting("Android")
    }
}