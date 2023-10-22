package com.superadtech.modids.Top;

import android.os.Parcel;
import android.os.Parcelable;

public class Countries implements Parcelable {
    private String country;
    private String ovpn;
    private String ovpnUserName;
    private String ovpnUserPassword;

    public Countries(String country, String ovpn, String ovpnUserName, String ovpnUserPassword) {
        this.country = country;
        this.ovpn = ovpn;
        this.ovpnUserName = ovpnUserName;
        this.ovpnUserPassword = ovpnUserPassword;
    }

    public String getCountry() {
        return country;
    }

    public String getOvpn() {
        return ovpn;
    }

    public String getOvpnUserName() {
        return ovpnUserName;
    }

    public String getOvpnUserPassword() {
        return ovpnUserPassword;
    }

    public static final Creator<Countries> CREATOR
            = new Creator<Countries>() {
        public Countries createFromParcel(Parcel in) {
            return new Countries(in);
        }

        public Countries[] newArray(int size) {
            return new Countries[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(country);
        dest.writeString(ovpn);
        dest.writeString(ovpnUserName);
        dest.writeString(ovpnUserPassword);
    }

    private Countries(Parcel in) {
        country = in.readString();
        ovpn = in.readString();
        ovpnUserName = in.readString();
        ovpnUserPassword = in.readString();
    }
}

