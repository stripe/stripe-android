package com.stripe.wrap.pay.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.stripe.wrap.pay.AndroidPayConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    public void createCartManager_withoutCurrencyArgument_usesAndroidPayConfiguration() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager cartManager = new CartManager();
        assertEquals("JPY", cartManager.getCurrencyCode());
    }

    @Test
    public void createCartManager_withCurrencyDifferentFromConfiguration_changesConfigAndLogs() {
        ShadowLog.stream = System.out;
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");

        String expectedWarning = "Cart created with currency code GBP, which differs from " +
                "current AndroidPayConfiguration currency, KRW. Changing configuration to GBP";

        new CartManager("GBP");
        assertEquals("GBP", AndroidPayConfiguration.getInstance().getCurrencyCode());

        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(CartManager.TAG);
        assertFalse(logItems.isEmpty());
        assertEquals(1, logItems.size());
        assertEquals(expectedWarning, logItems.get(0).msg);
        assertEquals(Log.WARN, logItems.get(0).type);
    }

    @Test
    public void createCartManager_fromOldCartWithDifferentCurrency_changesConfigAndLogsWarning() {
        ShadowLog.stream = System.out;
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");

        String expectedWarning = "Cart created with currency code EUR, which differs from " +
                "current AndroidPayConfiguration currency, JPY. Changing configuration to EUR";

        Cart oldCart = Cart.newBuilder().setCurrencyCode("EUR").setTotalPrice("10.00").build();
        new CartManager(oldCart);
        assertEquals("EUR", AndroidPayConfiguration.getInstance().getCurrencyCode());

        List<ShadowLog.LogItem> logItems = ShadowLog.getLogsForTag(CartManager.TAG);
        assertFalse(logItems.isEmpty());
        assertEquals(1, logItems.size());
        assertEquals(expectedWarning, logItems.get(0).msg);
        assertEquals(Log.WARN, logItems.get(0).type);
    }

    @Test
    public void addItem_whenRegularItem_addsOnlyRegularItem() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager cartManager = new CartManager("USD");
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
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager cartManager = new CartManager("USD");
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
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager cartManager = new CartManager("JPY");
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
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");

        String expectedWarning = "Adding a tax line item, but a tax line item " +
                "already exists. Old tax of 1.00 is being overwritten " +
                "to maintain a valid cart.";

        CartManager cartManager = new CartManager("USD");

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
        assertEquals(PaymentUtils.getPriceString(200L, Currency.getInstance("USD")),
                cartTaxItem.getTotalPrice());
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
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager manager = new CartManager("USD");
        String id = manager.addLineItem("llama food", 10000L);

        LineItem item = manager.removeLineItem(id);

        assertNotNull(item);
        assertEquals(LineItem.Role.REGULAR, item.getRole());
        assertEquals("llama food", item.getDescription());
        assertEquals("100.00", item.getTotalPrice());

        assertEquals(Long.valueOf(0L), manager.calculateRegularItemTotal());
        assertEmpty(manager.getLineItemsRegular());
        assertEmpty(manager.getLineItemsShipping());
    }

    @Test
    public void addShippingItem_thenRemoveItem_leavesNoShippingItems() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");
        CartManager manager = new CartManager("KRW");
        String id = manager.addShippingLineItem("2 Day Guaranteed", 2099);
        assertNotNull(id);

        LineItem item = manager.removeLineItem(id);

        assertNotNull(item);
        assertEquals(LineItem.Role.SHIPPING, item.getRole());
        assertEquals("2 Day Guaranteed", item.getDescription());
        assertEquals("2099", item.getTotalPrice());

        assertEquals(Long.valueOf(0L), manager.calculateShippingItemTotal());
        assertEmpty(manager.getLineItemsRegular());
        assertEmpty(manager.getLineItemsShipping());
    }

    @Test
    public void addTaxItem_doesNotAffectRegularOrShippingTotals() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");
        CartManager manager = new CartManager("KRW");
        manager.addLineItem("Regular", 10L);
        manager.addLineItem("Regular Again", 20L);
        manager.addShippingLineItem("Shipping 1", 30L);
        manager.addShippingLineItem("Shipping 2", 40L);

        assertEquals(Long.valueOf(30L), manager.calculateRegularItemTotal());
        assertEquals(Long.valueOf(70L), manager.calculateShippingItemTotal());

        manager.setTaxLineItem("Taxes", 10000L);

        assertEquals(Long.valueOf(30L), manager.calculateRegularItemTotal());
        assertEquals(Long.valueOf(70L), manager.calculateShippingItemTotal());
    }

    @Test
    public void addLineItem_whenDifferentCurrencyFromCart_returnsNullForTotalCalculation() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager manager = new CartManager("JPY");
        manager.addLineItem("Regular Item", 100L);
        manager.addLineItem("Another Regular Thing", 100L);

        assertEquals(Long.valueOf(200L), manager.calculateRegularItemTotal());

        LineItem wonItem = new LineItemBuilder("KRW")
                .setDescription("Imported Item")
                .setTotalPrice(500L)
                .build();
        manager.addLineItem(wonItem);

        // We can't add prices in different currencies
        assertNull(manager.calculateRegularItemTotal());
    }

    @Test
    public void calculateTax_whenItemHasCorrectFormat_returnsValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager manager = new CartManager("USD");
        manager.setTaxLineItem("Tax", 1000L);

        assertEquals(Long.valueOf(1000L), manager.calculateTax());
    }

    @Test
    public void calculateTax_whenItemNotPresent_returnsZero() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");
        CartManager manager = new CartManager("KRW");
        assertEquals(Long.valueOf(0L), manager.calculateTax());
    }

    @Test
    public void calculateTax_whenItemHasWrongCurrency_returnsNull() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager manager = new CartManager("USD");
        LineItem taxItem = new LineItemBuilder("AUD")
                .setTotalPrice(5000L)
                .setDescription("Australian Taxes")
                .setRole(LineItem.Role.TAX)
                .build();
        manager.setTaxLineItem(taxItem);

        assertNull(manager.calculateTax());
    }

    @Test
    public void calculateTax_whenItemHasNoPrice_returnsNull() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager manager = new CartManager("JPY");
        LineItem taxItem = new LineItemBuilder("JPY")
                .setDescription("Tax")
                .setRole(LineItem.Role.TAX)
                .build();
        manager.setTaxLineItem(taxItem);

        assertNull(manager.calculateTax());
    }

    @Test
    public void createCartManager_fromExistingCart_copiesRegularLineItemsAndCurrencyCode() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        Cart oldCart = generateCartWithAllItems("USD");

        CartManager copyCartManager = new CartManager(oldCart);
        assertEquals("USD", copyCartManager.getCurrencyCode());
        assertEmpty(copyCartManager.getLineItemsShipping());
        assertNull(copyCartManager.getLineItemTax());
        assertNotNull(copyCartManager.getLineItemsRegular());

        Map<String, String> expectedItemMap = new HashMap<>();
        expectedItemMap.put("Llama Food", "20.00");
        expectedItemMap.put("Llama Bow-tie", "50.00");

        List<LineItem> copiedLineItems = new ArrayList<>();
        copiedLineItems.addAll(copyCartManager.getLineItemsRegular().values());

        assertEquals(Long.valueOf(7000L), copyCartManager.calculateRegularItemTotal());
        verifyLineItemsHaveExpectedValues(expectedItemMap, copiedLineItems);
    }

    @Test
    public void createCartManagerAndSetTrue_fromExistingCart_copiesAllItemsAndCurrencyCode() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");
        Cart oldCart = generateCartWithAllItems("KRW");

        CartManager copyCartManager = new CartManager(oldCart, true, true);
        assertEquals("KRW", copyCartManager.getCurrencyCode());

        Map<String, String> expectedItemMap = new HashMap<>();
        expectedItemMap.put("Llama Food", "2000");
        expectedItemMap.put("Llama Bow-tie", "5000");
        Map<String, String> shippingMap = new HashMap<>();
        shippingMap.put("Domestic Shipping", "500");
        shippingMap.put("Next Day Shipping", "3000");

        List<LineItem> copiedLineItems = new ArrayList<>();
        copiedLineItems.addAll(copyCartManager.getLineItemsRegular().values());
        verifyLineItemsHaveExpectedValues(expectedItemMap, copiedLineItems);

        List<LineItem> copiedShippingItems = new ArrayList<>();
        copiedShippingItems.addAll(copyCartManager.getLineItemsShipping().values());
        verifyLineItemsHaveExpectedValues(shippingMap, copiedShippingItems);

        assertNotNull(copyCartManager.getLineItemTax());
        assertEquals("Tax", copyCartManager.getLineItemTax().getDescription());
        assertEquals("333", copyCartManager.getLineItemTax().getTotalPrice());
    }

    @Test
    public void buildCart_withValidCart_createsExpectedCart() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
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
    public void buildCart_whenLineItemsEmptyAndNoPriceSet_createsZeroPriceCart() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        CartManager manager = new CartManager("USD");

        try {
            Cart cart = manager.buildCart();
            assertNotNull(cart);
            assertEquals("0.00", cart.getTotalPrice());
        } catch (CartContentException cartEx) {
            fail("Unexpected error: " + cartEx.getMessage());
        }
    }

    @Test
    public void buildCart_whenHasErrors_throwsExpectedException() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager manager = new CartManager("JPY");

        LineItem dollarItem = new LineItemBuilder("USD").setTotalPrice(100L).build();
        LineItem wonItem = new LineItemBuilder("KRW").setTotalPrice(5000L).build();
        LineItem taxItem = new LineItemBuilder("JPY")
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

    @Test
    public void buildCart_withManyErrorsButTotalValueSet_doesRemovesLineItemCurrencyErrors() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        CartManager manager = new CartManager("JPY");

        LineItem dollarItem = new LineItemBuilder("USD").setTotalPrice(100L).build();
        LineItem wonItem = new LineItemBuilder("KRW").setTotalPrice(5000L).build();
        LineItem taxItem = new LineItemBuilder("JPY")
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

        manager.setTotalPrice(5000L);

        try {
            manager.buildCart();
            fail("Should not be able to build a cart with bad line items.");
        } catch (CartContentException cartEx) {
            String message = cartEx.getMessage();
            List<CartError> errors = cartEx.getCartErrors();
            assertEquals(2, errors.size());
            assertTrue(message.startsWith(CartContentException.CART_ERROR_MESSAGE_START));

            assertEquals(CartError.LINE_ITEM_PRICE, errors.get(0).getErrorType());
            assertEquals(badPriceStringItem, errors.get(0).getLineItem());
            assertEquals(CartError.LINE_ITEM_QUANTITY, errors.get(1).getErrorType());
            assertEquals(badQuantityStringItem, errors.get(1).getLineItem());

            String[] messageLines = message.split("\n");
            assertEquals(3, messageLines.length);
            assertTrue(messageLines[1].contains("$70.00"));
            assertTrue(messageLines[2].contains("1.75"));
        }

    }

    @Test
    public void buildCart_withItemCurrencyErrorsButTotalValueIsManuallySet_doesNotThrowErrors() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("CAD");
        CartManager cartManager = new CartManager("CAD");
        LineItem dollarItem = new LineItemBuilder("USD").setTotalPrice(100L).build();
        LineItem wonItem = new LineItemBuilder("KRW").setTotalPrice(5000L).build();
        LineItem yenItem = new LineItemBuilder("JPY")
                .setTotalPrice(20L)
                .setRole(LineItem.Role.TAX)
                .build();
        cartManager.addLineItem(dollarItem);
        cartManager.addLineItem(wonItem);
        cartManager.addLineItem(yenItem);

        // Note that the total price doesn't have to be related to the other item prices.
        cartManager.setTotalPrice(500000L);

        Cart cart;
        try {
            cart = cartManager.buildCart();
            assertNotNull(cart);
            assertEquals("5000.00", cart.getTotalPrice());
        } catch (CartContentException unexpected) {
            fail("Should not have found any cart content exceptions.");
        }
    }

    @Test
    public void getTotalPrice_whenAllSectionsPresent_returnsExpectedValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("KRW");
        Cart oldCart = generateCartWithAllItems("KRW");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(null);

        assertEquals(Long.valueOf(10833L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenManualOverride_returnsManualValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("USD");
        Cart oldCart = generateCartWithAllItems("USD");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(15L);

        assertEquals(Long.valueOf(15L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenManualOverrideButItemAdded_returnsNewValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("AUD");
        Cart oldCart = generateCartWithAllItems("AUD");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(15L);

        manager.addShippingLineItem("New Shipping Addition", 100L);

        assertEquals(Long.valueOf(10933L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenManualOverrideButItemRemoved_returnsNewValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("AUD");
        Cart oldCart = generateCartWithAllItems("AUD");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(15L);

        Set<String> keys = manager.getLineItemsRegular().keySet();
        String[] keyArray = new String[keys.size()];
        keys.toArray(keyArray);
        manager.removeLineItem(keyArray[0]);

        assertEquals(Long.valueOf(8833L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenManualOverrideButTaxChanged_returnsNewValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("GBP");
        Cart oldCart = generateCartWithAllItems("GBP");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(15L);

        manager.setTaxLineItem("New Taxes", 200L);

        assertEquals(Long.valueOf(10700L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenManualOverrideAndItemUpdatedWithNoPrice_returnsManualValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("AUD");
        Cart oldCart = generateCartWithAllItems("AUD");
        CartManager manager = new CartManager(oldCart, true, true);
        manager.setTotalPrice(15L);

        LineItem noPriceItem = new LineItemBuilder().build();
        String noPriceKey = manager.addLineItem(noPriceItem);
        assertNotNull(noPriceKey);

        assertEquals(Long.valueOf(15L), manager.getTotalPrice());

        LineItem removed = manager.removeLineItem(noPriceKey);
        assertNotNull(removed);
        assertEquals(Long.valueOf(15L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenSomeSectionsAreNull_returnsExpectedValue() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("JPY");
        Cart oldCart = generateCartWithAllItems("JPY");
        // This will drop the shipping and tax items
        CartManager manager = new CartManager(oldCart);

        assertEquals(Long.valueOf(7000L), manager.getTotalPrice());
    }

    @Test
    public void getTotalPrice_whenTotalCannotBeCalculated_returnsNull() {
        AndroidPayConfiguration.getInstance().setCurrencyCode("CAD");
        Cart oldCart = generateCartWithAllItems("CAD");
        CartManager manager = new CartManager(oldCart);

        LineItem yenItem = new LineItemBuilder("JPY").setTotalPrice(100L).build();
        manager.addLineItem(yenItem);

        assertNull(manager.getTotalPrice());
    }

    private static void verifyLineItemsHaveExpectedValues(
            @NonNull Map<String, String> expectedDescriptionPriceMap,
            @NonNull List<LineItem> lineItems) {
        assertEquals(expectedDescriptionPriceMap.size(), lineItems.size());
        for (LineItem item : lineItems) {
            assertTrue(expectedDescriptionPriceMap.containsKey(item.getDescription()));
            assertEquals(expectedDescriptionPriceMap.get(item.getDescription()),
                    item.getTotalPrice());
        }
    }

    @NonNull
    private static Cart generateCartWithAllItems(String currencyCode) {
        AndroidPayConfiguration.getInstance().setCurrencyCode(currencyCode);
        CartManager manager = new CartManager(currencyCode);
        manager.addLineItem("Llama Food", 2000L);
        manager.addLineItem("Llama Bow-tie", 5000L);
        manager.addShippingLineItem("Domestic Shipping", 500L);
        manager.addShippingLineItem("Next Day Shipping", 3000L);
        manager.setTaxLineItem("Tax", 333L);
        try {
            return manager.buildCart();
        } catch (CartContentException unexpected) {
            fail("Failed to automatically build a cart.");
            return Cart.newBuilder().build();
        }
    }
}
