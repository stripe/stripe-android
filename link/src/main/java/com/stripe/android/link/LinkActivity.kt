package com.stripe.android.link

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.ui.theme.DefaultLinkTheme

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkActivity : ComponentActivity() {
    internal lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            navController = rememberNavController()
            DefaultLinkTheme {
                Surface {
                    Column(Modifier.fillMaxWidth()) {
                        LinkAppBar()
                        NavHost(navController, startDestination = LinkScreen.SignUp.name) {
                            LinkScreen.values().forEach { screen ->
                                composable(screen.name) {
                                    screen.body()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LinkAppBar() {
    TopAppBar {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link_logo),
                contentDescription = stringResource(R.string.link),
            )
        }
    }
}
