package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils
import java.util.Scanner
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

internal class BecsDebitBanks(
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
        internal val prefix: String,
        internal val code: String,
        internal val name: String
    ) : Parcelable

    private companion object {
        private fun createBanksData(context: Context): List<Bank> {
            return StripeJsonUtils.jsonObjectToMap(
                JSONObject(readFile(context))
            ).orEmpty().map { entry ->
                (entry.value as List<*>).let {
                    Bank(
                        prefix = entry.key,
                        code = it.first().toString(),
                        name = it.last().toString()
                    )
                }
            }
        }

        private fun readFile(context: Context): String {
            return Scanner(
                context.resources.assets.open("au_becs_bsb.json")
            ).useDelimiter("\\A").next()
        }

        private val STRIPE_TEST_BANK = Bank(
            prefix = "00",
            code = "STRIPE",
            name = "Stripe Test Bank"
        )
    }
}
