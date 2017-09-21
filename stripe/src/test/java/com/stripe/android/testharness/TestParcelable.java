package com.stripe.android.testharness;

import android.os.Parcel;
import android.os.Parcelable;

public class TestParcelable implements Parcelable {

    public int field1;
    public boolean field2;
    public String field3;

    public TestParcelable(int f1, boolean f2, String f3) {
        field1 = f1;
        field2 = f2;
        field3 = f3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.field1);
        dest.writeByte(this.field2 ? (byte) 1 : (byte) 0);
        dest.writeString(this.field3);
    }

    protected TestParcelable(Parcel in) {
        this.field1 = in.readInt();
        this.field2 = in.readByte() != 0;
        this.field3 = in.readString();
    }

    public static final Parcelable.Creator<TestParcelable> CREATOR = new Parcelable.Creator<TestParcelable>() {
        @Override
        public TestParcelable createFromParcel(Parcel source) {
            return new TestParcelable(source);
        }

        @Override
        public TestParcelable[] newArray(int size) {
            return new TestParcelable[size];
        }
    };
}
