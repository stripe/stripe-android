package com.stripe.wrap.pay.utils;

import android.util.Log;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.stripe.wrap.pay.testutils.AssertUtils.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertEmpty(regularItems);
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

        LineItem firstTaxItem = new LineItemBuilder("USD")
                .setTotalPrice(100L).setRole(LineItem.Role.TAX).build();
        LineItem secondTaxItem = new LineItemBuilder("USD")
                .setTotalPrice(200L).setRole(LineItem.Role.TAX).build();

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

    @Test
    public void generateUuidForRole_beginsWithAppropriatePrefix() {
        String regularUuid = CartManager.generateUuidForRole(LineItem.Role.REGULAR);
        String shippingUuid = CartManager.generateUuidForRole(LineItem.Role.SHIPPING);

        assertTrue(regularUuid.startsWith(CartManager.REGULAR_ID));
        assertTrue(shippingUuid.startsWith(CartManager.SHIPPING_ID));
    }

    @Test
    public void addItem_thenRemoveItem_leavesNoItems() {
        CartManager manager = new CartManager("USD");
        String id = manager.addLineItem("llama food", 10000L);

        LineItem item = manager.removeItem(id);

        assertNotNull(item);
        assertEquals(LineItem.Role.REGULAR, item.getRole());
        assertEquals("llama food", item.getDescription());
        assertEquals("100.00", item.getTotalPrice());

        assertEmpty(manager.getLineItemsRegular());
        assertEmpty(manager.getLineItemsShipping());
    }

    @Test
    public void addShippingItem_thenRemoveItem_leavesNoShippingItems() {
        CartManager manager = new CartManager("KRW");
        String id = manager.addShippingLineItem("2 Day Guaranteed", 2099);

        LineItem item = manager.removeItem(id);

        assertNotNull(item);
        assertEquals(LineItem.Role.SHIPPING, item.getRole());
        assertEquals("2 Day Guaranteed", item.getDescription());
        assertEquals("2099", item.getTotalPrice());

        assertEmpty(manager.getLineItemsRegular());
        assertEmpty(manager.getLineItemsShipping());
    }

    @Test
    public void buildCart_withValidCart_createsExpectedCart() {
        CartManager manager = new CartManager("USD");
        manager.addLineItem("llama food", 10000L);
        manager.addLineItem("llama scarf", 2000L);
        manager.addShippingLineItem("Overnight", 50000L);
        manager.setTaxLineItem("Tax", 1278L);

        try {
            Cart cart = manager.buildCart();
            assertNotNull(cart);
            assertEquals("USD", cart.getCurrencyCode());
            assertEquals(4, cart.getLineItems().size());
            assertEquals("632.78", cart.getTotalPrice());
        } catch (CartContentException cartEx) {
            fail("Unexpected error: " + cartEx.getMessage());
        }
    }

    @Test
    public void buildCart_whenLineItemsEmpty_returnsNull() {
        CartManager manager = new CartManager("USD");

        try {
            assertNull(manager.buildCart());
        } catch (CartContentException cartEx) {
            fail("Unexpected error: " + cartEx.getMessage());
        }
    }

    @Test
    public void buildCart_whenHasErrors_throwsExpectedException() {
        Locale.setDefault(Locale.JAPAN);
        CartManager manager = new CartManager();

        LineItem dollarItem = new LineItemBuilder("USD").setTotalPrice(100L).build();
        LineItem wonItem = new LineItemBuilder("KRW").setTotalPrice(5000L).build();
        LineItem taxItem = new LineItemBuilder()
                .setTotalPrice(20L)
                .setRole(LineItem.Role.TAX)
                .build();
        LineItem badPriceStringItem = LineItem.newBuilder()
                .setCurrencyCode("JPY")
                .setTotalPrice("$70.00").build();
        LineItem badQuantityStringItem = LineItem.newBuilder()
                .setQuantity("1.75")
                .setUnitPrice("300")
                .setTotalPrice("8000000")
                .setCurrencyCode("JPY")
                .build();
        manager.addLineItem(dollarItem);
        manager.addLineItem(wonItem);
        manager.addLineItem(taxItem);
        manager.addLineItem(badPriceStringItem);
        manager.addLineItem(badQuantityStringItem);

        try {
            manager.buildCart();
            fail("Should not be able to build a cart with bad line items.");
        } catch (CartContentException cartEx) {
            String message = cartEx.getMessage();
            List<CartError> errors = cartEx.getCartErrors();
            assertEquals(4, errors.size());
            assertTrue(message.startsWith(CartContentException.CART_ERROR_MESSAGE_START));

            assertEquals(CartError.LINE_ITEM_CURRENCY, errors.get(0).getErrorType());
            assertEquals(dollarItem, errors.get(0).getLineItem());
            assertEquals(CartError.LINE_ITEM_CURRENCY, errors.get(1).getErrorType());
            assertEquals(wonItem, errors.get(1).getLineItem());
            assertEquals(CartError.LINE_ITEM_PRICE, errors.get(2).getErrorType());
            assertEquals(badPriceStringItem, errors.get(2).getLineItem());
            assertEquals(CartError.LINE_ITEM_QUANTITY, errors.get(3).getErrorType());
            assertEquals(badQuantityStringItem, errors.get(3).getLineItem());

            String[] messageLines = message.split("\n");
            assertEquals(5, messageLines.length);
            assertTrue(messageLines[1].contains("USD"));
            assertTrue(messageLines[2].contains("KRW"));
            assertTrue(messageLines[3].contains("$70.00"));
            assertTrue(messageLines[4].contains("1.75"));
        }
    }
}
