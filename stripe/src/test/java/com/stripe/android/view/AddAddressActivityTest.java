package com.stripe.android.view;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;
import com.stripe.android.model.Address;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class AddAddressActivityTest {

    private ActivityController<AddAddressActivity> mActivityController;
    private ShadowActivity mShadowActivity;
    private Address mAddress;
    private AddAddressWidget mAddAddressWidget;

    @Before
    public void setup() {
        mAddress = new Address.Builder()
                .setCity("San Francisco")
                .setName("Fake Name")
                .setState("CA")
                .setCountry("US")
                .setLine1("185 Berry St")
                .setLine2("10th Floor")
                .setPostalCode("12345")
                .setPhoneNumber("(123) 456 - 7890")
                .build();
    }

    private void launchActivityWithAddress() {
        Intent intent = AddAddressActivity.newIntent(RuntimeEnvironment.application, null, null, mAddress);
        mActivityController = Robolectric.buildActivity(AddAddressActivity.class, intent)
                .create().start().resume().visible();
        mAddAddressWidget = mActivityController.get().findViewById(R.id.add_address_widget);
        mShadowActivity = shadowOf(mActivityController.get());
    }

    private void launchActivityWithoutAddress() {
        Intent intent = AddAddressActivity.newIntent(RuntimeEnvironment.application, null, null, null);
        mActivityController = Robolectric.buildActivity(AddAddressActivity.class, intent)
                .create().start().resume().visible();
        mAddAddressWidget = mActivityController.get().findViewById(R.id.add_address_widget);
        mShadowActivity = shadowOf(mActivityController.get());
    }

    @Test
    public void launchActivity_whenAddressProvided_populatesAsExpected() {
        launchActivityWithAddress();
        assertEquals(mAddAddressWidget.getAddress().toMap(), mAddress.toMap());
    }

    @Test
    public void saveActivity_whenAddressSaved_returnsWithAddress() {
        launchActivityWithAddress();
        mActivityController.get().onActionSave();
        Intent intent = mShadowActivity.getResultIntent();
        assertTrue(mShadowActivity.isFinishing());
        Address address = intent.getParcelableExtra(AddAddressActivity.EXTRA_NEW_ADDRESS);
        assertEquals(address.toMap(), mAddress.toMap());
    }

    @Test
    public void saveActivity_whenAddressInvalid_displaysError() {
        launchActivityWithoutAddress();
        mActivityController.get().onActionSave();
        assertFalse(mShadowActivity.isFinishing());
        TextInputLayout nameTextInputLayout = mAddAddressWidget.findViewById(R.id.tl_name_aaw);
        assertTrue(nameTextInputLayout.isErrorEnabled());
    }

}
