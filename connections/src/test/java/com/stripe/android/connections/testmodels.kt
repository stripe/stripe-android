package com.stripe.android.connections

import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkedAccountFixtures
import com.stripe.android.connections.model.LinkedAccountList

val linkAccountSessionWithNoMoreAccounts = LinkAccountSession(
    id = "las_no_more",
    clientSecret = ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
    linkedAccounts = LinkedAccountList(
        listOf(
            LinkedAccountFixtures.CREDIT_CARD,
            LinkedAccountFixtures.CHECKING_ACCOUNT
        ),
        false,
        "url",
        2
    ),
    livemode = true
)

val linkAccountSessionWithMoreAccounts = LinkAccountSession(
    id = "las_has_more",
    clientSecret = ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
    linkedAccounts = LinkedAccountList(
        linkedAccounts = listOf(
            LinkedAccountFixtures.CREDIT_CARD,
            LinkedAccountFixtures.CHECKING_ACCOUNT
        ),
        hasMore = true,
        url = "url",
        count = 2,
        totalCount = 3
    ),
    livemode = true
)

val moreLinkedAccountList = LinkedAccountList(
    linkedAccounts = listOf(LinkedAccountFixtures.SAVINGS_ACCOUNT),
    hasMore = false,
    url = "url",
    count = 1,
    totalCount = 3
)
