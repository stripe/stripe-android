package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.LinkedAccountFixtures
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.model.BankAccount
import com.stripe.android.model.Token
import java.util.Date

val financialConnectionsSessionWithNoMoreAccounts = FinancialConnectionsSession(
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

val financialConnectionsSessionWithMoreAccounts = FinancialConnectionsSession(
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

val bankAccountToken = Token(
    id = "tok_189fi32eZvKYlo2Ct0KZvU5Y",
    livemode = false,
    created = Date(1462905355L * 1000L),
    used = false,
    type = Token.Type.BankAccount,
    bankAccount = BankAccount(
        id = "ba_1H3NOMCRMbs6FrXfahj",
        accountHolderName = "Test Bank Account",
        accountHolderType = BankAccount.Type.Individual,
        bankName = "STRIPE TEST BANK",
        countryCode = "US",
        currency = "usd",
        fingerprint = "wxXSAD5idPUzgBEz",
        last4 = "6789",
        routingNumber = "110000000",
        status = BankAccount.Status.New
    )
)
