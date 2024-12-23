package com.arcrun.eo.Models

import android.os.Parcel
import android.os.Parcelable

data class LokasiModels(
    var namaLokasi: String? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(namaLokasi)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LokasiModels> {
        override fun createFromParcel(parcel: Parcel): LokasiModels {
            return LokasiModels(parcel)
        }

        override fun newArray(size: Int): Array<LokasiModels?> {
            return arrayOfNulls(size)
        }
    }
}

