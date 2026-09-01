package com.stripe.android.link.account

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.LoggedOut
import com.stripe.android.link.TestFactory
import kotlin.test.Test

internal class LinkAccountHolderTest {
    @Test
    fun `setIfAbsent stores state when none was previously stored`() {
        val holder = LinkAccountHolder(SavedStateHandle())
        val accountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        holder.setIfAbsent(accountInfo)

        assertThat(holder.linkAccountInfo.value).isEqualTo(accountInfo)
    }

    @Test
    fun `setIfAbsent does not overwrite restored state`() {
        val savedStateHandle = SavedStateHandle()
        val restoredInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        LinkAccountHolder(savedStateHandle).set(restoredInfo)
        val restoredHolder = LinkAccountHolder(savedStateHandle)

        restoredHolder.setIfAbsent(LinkAccountUpdate.Value(null, LoggedOut))

        assertThat(restoredHolder.linkAccountInfo.value).isEqualTo(restoredInfo)
    }

    @Test
    fun `setIfAbsent does not overwrite explicitly empty state`() {
        val holder = LinkAccountHolder(SavedStateHandle())
        val emptyInfo = LinkAccountUpdate.Value(account = null)
        holder.set(emptyInfo)

        holder.setIfAbsent(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        assertThat(holder.linkAccountInfo.value).isEqualTo(emptyInfo)
    }

    @Test
    fun `set overwrites state and prevents a later setIfAbsent`() {
        val holder = LinkAccountHolder(SavedStateHandle())
        holder.setIfAbsent(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
        val overwrittenInfo = LinkAccountUpdate.Value(null, LoggedOut)

        holder.set(overwrittenInfo)
        holder.setIfAbsent(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        assertThat(holder.linkAccountInfo.value).isEqualTo(overwrittenInfo)
    }
}
