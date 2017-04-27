package com.stripe.wrap.pay.utils;

import android.util.Log;

import com.google.android.gms.wallet.LineItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link CartManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CartManagerTest {

    @Test
    public void addItem_whenRegularItem_addsOnlyRegularItem() {
        CartManager cartManager = new CartManager();
        LineItem regularItem = LineItem.newBuilder().setRole(LineItem.Role.REGULAR).build();
        cartManager.addLineItem(regularItem);

        Map<String, LineItem> regularItems = cartManager.getLineItemsRegular();
        Map<String, LineItem> shippingItems = cartManager.getLineItemsShipping();
        LineItem taxItem = cartManager.getLineItemTax();

        assertEquals(1, regularItems.size());
        assertTrue(shippingItems.isEmpty());
        assertNull(taxItem);
    }

    @Test
    public void addItem_whenShippingItem_addsOnlyShippingItem() {
        CartManager cartManager = new CartManager();
        LineItem shippingItem = LineItem.newBuilder().setRole(LineItem.Role.SHIPPING).build();
        cartManager.addLineItem(shippingItem);

        Map<String, LineItem> regularItems = cartManager.getLineItemsRegular();
        Map<String, LineItem> shippingItems = cartManager.getLineItemsShipping();
        LineItem taxItem = cartManager.getLineItemTax();

        assertEquals(1, shippingItems.size());
        assertTrue(regularItems.isEmpty());
        assertNull(taxItem);
    }

    @Test
    public void addItem_whenTaxItem_addsOnlyTaxItem() {
        CartManager cartManager = new CartManager();
        LineItem taxItem = LineItem.newBuilder().setRole(LineItem.Role.TAX).build();
        cartManager.addLineItem(taxItem);

        Map<String, LineItem> regularItems = cartManager.getLineItemsRegular();
        Map<String, LineItem> shippingItems = cartManager.getLineItemsShipping();
        LineItem cartTaxItem = cartManager.getLineItemTax();

        assertTrue(regularItems.isEmpty());
        assertTrue(shippingItems.isEmpty());
        assertNotNull(cartTaxItem);
    }

    @Test
    public void addTaxItem_whenTaxItemExists_overwritesAndLogsWarning() {
        ShadowLog.stream = System.out;
        Locale.setDefault(Locale.US);

        String expectedWarning = "Adding a tax line item, but a tax line item " +
                "already exists. Old tax of 1.00 is being overwritten " +
                "to maintain a valid cart.";

        CartManager cartManager = new CartManager();

        LineItem firstTaxItem = new LineItemBuilder(LineItem.Role.TAX, "USD")
                .setTotalPrice(100L).build();
        LineItem secondTaxItem = new LineItemBuilder(LineItem.Role.TAX, "USD")
                .setTotalPrice(200L).build();

        cartManager.addLineItem(firstTaxItem);
        cartManager.addLineItem(secondTaxItem);

        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(CartManager.TAG);
        assertFalse(logItems.isEmpty());
        assertEquals(1, logItems.size());
        assertEquals(expectedWarning, logItems.get(0).msg);
        assertEquals(Log.WARN, logItems.get(0).type);

        LineItem cartTaxItem = cartManager.getLineItemTax();

        assertNotNull(cartTaxItem);
        assertEquals(PaymentUtils.getPriceString(200L), cartTaxItem.getTotalPrice());
    }
}
