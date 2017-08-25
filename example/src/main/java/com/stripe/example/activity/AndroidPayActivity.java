package com.stripe.example.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.InstrumentInfo;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.stripe.android.model.StripePaymentSource;
import com.stripe.example.R;
import com.stripe.example.controller.ListViewController;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import java.util.Currency;
import java.util.Locale;

import static com.stripe.wrap.pay.utils.PaymentUtils.getPriceString;

public class AndroidPayActivity extends StripeAndroidPayActivity {

    private static final Locale LOC = Locale.US;
    private static final Currency USD = Currency.getInstance("USD");

    private ViewGroup mChangeDetailsContainer;
    private ViewGroup mConfirmDetailsContainer;
    private ViewGroup mFragmentContainer;
    private ListViewController mListViewController;
    private ProgressDialogController mProgressDialogController;

    private MaskedWallet mPossibleConfirmedMaskedWallet;

    private TextView mItemsPriceDisplay;
    private TextView mShippingDisplay;
    private TextView mTaxesDisplay;
    private TextView mTotalPaymentDisplay;
    private TextView mSelectedCardDisplay;
    private TextView mSelectedShippingAddressDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_pay);

        mFragmentContainer = (ViewGroup) findViewById(R.id.container_android_pay_button);
        mChangeDetailsContainer = (ViewGroup) findViewById(R.id.confirmation_total_container);
        mConfirmDetailsContainer = (ViewGroup) findViewById(R.id.proceed_container);
        ListView tokenListView = (ListView) findViewById(R.id.android_pay_listview);
        mListViewController = new ListViewController(tokenListView);
        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager());

        Button confirmButton = (Button) findViewById(R.id.btn_okay);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPayment();
            }
        });

        Button changeDetailsButton = (Button) findViewById(R.id.btn_change);
        changeDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPossibleConfirmedMaskedWallet != null) {
                    mConfirmDetailsContainer.setVisibility(View.GONE);
                    createAndAddConfirmationWalletFragment(mPossibleConfirmedMaskedWallet);
                }
            }
        });

        Button finishButton = (Button) findViewById(R.id.btn_proceed);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPossibleConfirmedMaskedWallet != null) {
                    proceedWithWallet(mPossibleConfirmedMaskedWallet);
                }
            }
        });
        mChangeDetailsContainer.setVisibility(View.GONE);
        mConfirmDetailsContainer.setVisibility(View.GONE);

        mItemsPriceDisplay = (TextView) findViewById(R.id.tv_item_total);
        mShippingDisplay = (TextView) findViewById(R.id.tv_shipping_total);
        mTaxesDisplay = (TextView) findViewById(R.id.tv_tax_total);
        mTotalPaymentDisplay = (TextView) findViewById(R.id.tv_payment_total);
        mSelectedCardDisplay = (TextView) findViewById(R.id.tv_card_info);
        mSelectedShippingAddressDisplay = (TextView) findViewById(R.id.tv_shipping_info);

        if (getCart() != null) {
            updateCartTotals(new CartManager(getCart(), true, true));
        }
    }

    @Override
    protected void onAndroidPayAvailable() {
        mFragmentContainer.setVisibility(View.VISIBLE);
        createAndAddBuyButtonWalletFragment();
    }

    @Override
    protected void onAndroidPayNotAvailable() {
        mFragmentContainer.setVisibility(View.GONE);
        mChangeDetailsContainer.setVisibility(View.GONE);
    }

    @Override
    protected void addBuyButtonWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        FragmentTransaction buttonTransaction = getSupportFragmentManager().beginTransaction();
        buttonTransaction.add(R.id.container_android_pay_button, walletFragment).commit();
    }

    @Override
    protected void addConfirmationWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        mChangeDetailsContainer.setVisibility(View.VISIBLE);
        FragmentTransaction confirmationTransaction =
                getSupportFragmentManager().beginTransaction();
        confirmationTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        confirmationTransaction.replace(R.id.container_fragment_confirmation, walletFragment).commit();
    }

    @NonNull
    @Override
    protected WalletFragmentStyle getWalletFragmentConfirmationStyle() {
        return new WalletFragmentStyle()
                .setMaskedWalletDetailsLogoImageType(WalletFragmentStyle.LogoImageType.ANDROID_PAY)
                .setStyleResourceId(R.style.SampleTheme)
                .setMaskedWalletDetailsTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
                .setMaskedWalletDetailsHeaderTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large);
    }

    @Override
    protected void onChangedMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        super.onChangedMaskedWalletRetrieved(maskedWallet);
        if (maskedWallet == null) {
            return;
        }

        mPossibleConfirmedMaskedWallet = maskedWallet;
        mChangeDetailsContainer.setVisibility(View.GONE);
        mConfirmDetailsContainer.setVisibility(View.VISIBLE);
        updatePaymentInformation(mPossibleConfirmedMaskedWallet);
    }

    @Override
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        super.onMaskedWalletRetrieved(maskedWallet);
        if (maskedWallet != null) {
            mPossibleConfirmedMaskedWallet = maskedWallet;

            updatePaymentInformation(mPossibleConfirmedMaskedWallet);

            mChangeDetailsContainer.setVisibility(View.GONE);
            mConfirmDetailsContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStripePaymentSourceReturned(
            FullWallet wallet,
            StripePaymentSource paymentSource) {
        super.onStripePaymentSourceReturned(wallet, paymentSource);
        mProgressDialogController.finishProgress();

        String cardDetails = null;
        if (wallet.getInstrumentInfos() != null && wallet.getInstrumentInfos().length > 0) {
            InstrumentInfo info = wallet.getInstrumentInfos()[0];
            cardDetails = info.getInstrumentDetails();
        }

        // This ID is what you would send to your server to create a charge.
        String id = paymentSource.getId();

        if (cardDetails == null) {
            cardDetails = "UNKN";
        }
        mListViewController.addToList(cardDetails, id);
    }

    private void confirmPayment() {
        mChangeDetailsContainer.setVisibility(View.GONE);
        mConfirmDetailsContainer.setVisibility(View.VISIBLE);
    }

    private void proceedWithWallet(@NonNull MaskedWallet maskedWallet) {
        mChangeDetailsContainer.setVisibility(View.GONE);

        if (getCart() == null) {
            return;
        }
        FullWalletRequest walletRequest =
                AndroidPayConfiguration.generateFullWalletRequest(
                        maskedWallet.getGoogleTransactionId(),
                        getCart());
        mProgressDialogController.startProgress();
        loadFullWallet(walletRequest);
    }

    private void updateCartTotals(@NonNull CartManager cartManager) {
        Long itemTotal = cartManager.calculateRegularItemTotal();
        Long shippingTotal = cartManager.calculateShippingItemTotal();
        Long tax = cartManager.calculateTax();
        mItemsPriceDisplay.setText(
                String.format(LOC,
                        "Item Total: %s",
                        getPriceString(itemTotal, USD)));
        mShippingDisplay.setText(
                String.format(LOC,
                        "Shipping: %s",
                        getPriceString(shippingTotal, USD)));
        mTaxesDisplay.setText(String.format(LOC, "Tax: %s", getPriceString(tax, USD)));
        mTotalPaymentDisplay.setText(String.format(
                LOC,
                "Total: %s",
                getPriceString(addIfNotNull(itemTotal, shippingTotal, tax), USD)));
    }

    private void updatePaymentInformation(@NonNull MaskedWallet maskedWallet) {
        if (maskedWallet.getPaymentDescriptions() != null
                && maskedWallet.getPaymentDescriptions().length > 0) {
            String cardText = String.format(LOC, "Card: %s", maskedWallet.getPaymentDescriptions()[0]);
            mSelectedCardDisplay.setText(cardText);
        }

        if (getCart() == null) {
            return;
        }

        CartManager copyCart = new CartManager(getCart());
        long shippingUpdate = calculateShipping(maskedWallet.getBuyerShippingAddress());
        long taxUpdate = calculateTaxes(maskedWallet.getBuyerBillingAddress());
        copyCart.addShippingLineItem("Shipping", shippingUpdate);
        copyCart.setTaxLineItem("Tax", taxUpdate);

        if (maskedWallet.getBuyerShippingAddress() != null) {
            mSelectedShippingAddressDisplay.setText(maskedWallet.getBuyerShippingAddress().getAddress1());
        }

        try {
            setCart(copyCart.buildCart());
            updateCartTotals(copyCart);
        } catch (CartContentException unexpected) {
            // ignore for now
        }
    }

    /**
     * When the user changes or confirms an address in the Android Pay fragment, you can
     * use that information to calculate shipping and tax information based on the location.
     *
     * Below we do a dummy calculation based on the length of the String elements to show how one
     * might update the screen, but you can use the geographic data to determine what
     * the cost of shipping your product will be.
     *
     * @param userAddress a {@link UserAddress} returned from Android Pay
     * @return the total cost of shipping, as an integer unit of the smallest denomination in the
     * currency
     */
    private long calculateShipping(UserAddress userAddress) {
        if (userAddress == null) {
            // It's hard to ship to literally nowhere!
            return 30000L;
        }

        long runningTotal = 0L;
        if (!TextUtils.isEmpty(userAddress.getAddress1())) {
            runningTotal += 10 * userAddress.getAddress1().length();
        }

        if (!TextUtils.isEmpty(userAddress.getAdministrativeArea())) {
            char[] adminAreaArray = userAddress.getAdministrativeArea().toCharArray();
            for (char c : adminAreaArray) {
                runningTotal += 5L * (long) c;
            }
        }

        if (!TextUtils.isEmpty(userAddress.getCountryCode())) {
            char[] adminAreaArray = userAddress.getAdministrativeArea().toCharArray();
            for (char c : adminAreaArray) {
                runningTotal += 8L * (long) c;
            }
        }
        return runningTotal;
    }

    /**
     * When the user changes or confirms an address in the Android Pay fragment, you can
     * use that information to calculate shipping and tax information based on the location.
     *
     * Below we do a dummy calculation based on the length of the String elements to show how one
     * might update the screen, but you can use the geographic data to determine what
     * the cost of shipping your product will be.
     *
     * @param userAddress a {@link UserAddress} returned from Android Pay
     * @return the total cost of shipping, as an integer unit of the smallest denomination in the
     * currency
     */
    private long calculateTaxes(UserAddress userAddress) {
        if (userAddress == null) {
            throw new IllegalArgumentException("User shipping address must not be null " +
                    "due to tax requirements.");
        }

        // This calculation is for sample purposes only. No actual jurisdiction uses this method.
        // Changing states or countries for the user billing address with this will show updates
        // on the screen in the Example Application.
        long runningTotal = 0L;
        if (!TextUtils.isEmpty(userAddress.getAdministrativeArea())) {
            char[] adminAreaArray = userAddress.getAdministrativeArea().toCharArray();
            for (char c : adminAreaArray) {
                runningTotal += 3L * (long) c;
            }
        }

        if (!TextUtils.isEmpty(userAddress.getCountryCode())) {
            char[] adminAreaArray = userAddress.getCountryCode().toCharArray();
            for (char c : adminAreaArray) {
                runningTotal += 7L * (long) c;
            }
        }
        return runningTotal;
    }

    private long addIfNotNull(Long... args) {
        long total = 0;
        for (Long l : args) {
            if (l != null) {
                total += l;
            }
        }
        return total;
    }
}
