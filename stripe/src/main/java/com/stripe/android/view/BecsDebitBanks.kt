package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils
import java.util.Scanner
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

internal class BecsDebitBanks(
    internal val banks: List<Bank>
) {
    constructor(context: Context) : this(createBanksData(context))

    @Parcelize
    data class Bank(
        private val prefix: String,
        private val code: String,
        private val name: String
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
    }
}
