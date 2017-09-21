package com.stripe.android;

import android.os.Parcel;

import com.stripe.android.testharness.TestParcelable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.stripe.android.ParcelUtils.NONNULL;
import static com.stripe.android.ParcelUtils.NULL;
import static com.stripe.android.ParcelUtils.readNullableParcelable;
import static com.stripe.android.ParcelUtils.readNullableString;
import static com.stripe.android.ParcelUtils.writeNullableParcelable;
import static com.stripe.android.ParcelUtils.writeNullableString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ParcelUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ParcelUtilsTest {

    private Parcel mParcel;

    @Before
    public void setup() {
        mParcel = Parcel.obtain();
    }

    @After
    public void tearDown() {
        mParcel.recycle();
    }

    @Test
    public void writeNullableParcelable_whenNull_writesNullByte() {
        TestParcelable nullParcelable = null;
        writeNullableParcelable(mParcel, 0, nullParcelable);
        mParcel.writeString("hello parcel");

        mParcel.setDataPosition(0);

        assertEquals(NULL, mParcel.readByte());
        assertEquals("hello parcel", mParcel.readString());
    }

    @Test
    public void writeNullableParcelable_whenNotNull_writesObject() {
        TestParcelable testParcelable = new TestParcelable(1, true, "hi");
        writeNullableParcelable(mParcel, 0, testParcelable);
        mParcel.writeString("next");

        mParcel.setDataPosition(0);

        assertEquals(NONNULL, mParcel.readByte());
        TestParcelable outItem = mParcel.readParcelable(TestParcelable.class.getClassLoader());
        assertNotNull(outItem);
        assertEquals(1, outItem.field1);
        assertTrue(outItem.field2);
        assertEquals("hi", outItem.field3);
        assertEquals("next", mParcel.readString());
    }

    @Test
    public void writeNullableString_whenNull_writesNullByte() {
        String s = null;
        writeNullableString(mParcel, s);
        mParcel.writeString("hello parcel");

        mParcel.setDataPosition(0);

        assertEquals(NULL, mParcel.readByte());
        assertEquals("hello parcel", mParcel.readString());
    }

    @Test
    public void writeNullableString_whenNotNull_writesString() {
        String s = "hi";
        writeNullableString(mParcel, s);
        mParcel.writeString("next");

        mParcel.setDataPosition(0);

        assertEquals(NONNULL, mParcel.readByte());
        assertEquals("hi", mParcel.readString());
        assertEquals("next", mParcel.readString());
    }

    @Test
    public void readNullableParcelable_whenNull_readsNull() {
        mParcel.writeByte(NULL);
        mParcel.writeString("test");

        mParcel.setDataPosition(0);

        assertNull(readNullableParcelable(mParcel, TestParcelable.class));
        assertEquals("test", mParcel.readString());
    }

    @Test
    public void readNullableParcelable_whenNotNull_readsObject() {
        mParcel.writeByte(NONNULL);
        mParcel.writeParcelable(new TestParcelable(1, false, "hi"), 0);
        mParcel.writeString("test");

        mParcel.setDataPosition(0);

        TestParcelable readItem = readNullableParcelable(mParcel, TestParcelable.class);
        assertNotNull(readItem);
        assertEquals(1, readItem.field1);
        assertFalse(readItem.field2);
        assertEquals("hi", readItem.field3);
        assertEquals("test", mParcel.readString());
    }

    @Test
    public void readNullableString_whenNull_readsNull() {
        mParcel.writeByte(NULL);
        mParcel.writeString("test");

        mParcel.setDataPosition(0);

        assertNull(readNullableString(mParcel));
        assertEquals("test", mParcel.readString());
    }

    @Test
    public void readNullableString_whenNotNull_readsString() {
        mParcel.writeByte(NONNULL);
        mParcel.writeString("not null");
        mParcel.writeString("test");

        mParcel.setDataPosition(0);

        assertEquals("not null", readNullableString(mParcel));
        assertEquals("test", mParcel.readString());
    }
}
