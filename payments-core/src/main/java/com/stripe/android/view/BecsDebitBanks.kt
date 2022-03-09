package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.util.Scanner

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BecsDebitBanks(
    internal val banks: List<Bank>,
    private val shouldIncludeTestBank: Boolean = true
) {
    constructor(
        context: Context,
        shouldIncludeTestBank: Boolean = true
    ) : this(
        createBanksData(context),
        shouldIncludeTestBank
    )

    fun byPrefix(bsb: String): Bank? {
        return banks
            .plus(listOfNotNull(STRIPE_TEST_BANK.takeIf { shouldIncludeTestBank }))
            .firstOrNull {
                bsb.startsWith(it.prefix)
            }
    }

    @Parcelize
    data class Bank(
        val prefix: String,
        val name: String
    ) : Parcelable

    private companion object {
        private fun createBanksData(context: Context): List<Bank> {
            return StripeJsonUtils.jsonObjectToMap(
                JSONObject(readFile(context))
            ).orEmpty().map { entry ->
                Bank(
                    prefix = entry.key,
                    name = entry.value.toString()
                )
            }
        }

        private fun readFile(context: Context): String {
            return Scanner(
                context.resources.assets.open("au_becs_bsb.json")
            ).useDelimiter("\\A").next()
        }

        private val STRIPE_TEST_BANK = Bank(
            prefix = "00",
            name = "Stripe Test Bank"
        )
    }
}
