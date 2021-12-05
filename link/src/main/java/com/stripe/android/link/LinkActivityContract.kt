package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

class LinkActivityContract :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    @Parcelize
    data class Args(
        val email: String? = null
    ) : ActivityStarter.Args

    override fun createIntent(context: Context, input: Args) =
        Intent(context, LinkActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?) = LinkActivityResult.Success
}
