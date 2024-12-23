package com.arcrun.eo.Models

import android.os.Parcel
import android.os.Parcelable

data class PesertaModels(
    val name: String? = null,
    val gender: String? = null,
    val ttl: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val emergencyContact: String? = null,
    val category: String? = null,
    val jerseySize: String? = null,
    val namaBib: String? = null,
    val riwayatPenyakit: String? = null,
    val eventId: String? = null,
    val userId: String? = null,
    val orderId: String? = null,
    val eventPrice: Int = 0
) : Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt()
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(gender)
        parcel.writeString(ttl)
        parcel.writeString(email)
        parcel.writeString(phone)
        parcel.writeString(emergencyContact)
        parcel.writeString(category)
        parcel.writeString(jerseySize)
        parcel.writeString(namaBib)
        parcel.writeString(riwayatPenyakit)
        parcel.writeString(eventId)
        parcel.writeString(userId)
        parcel.writeInt(eventPrice)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PesertaModels> {
        override fun createFromParcel(parcel: Parcel): PesertaModels {
            return PesertaModels(parcel)
        }

        override fun newArray(size: Int): Array<PesertaModels?> {
            return arrayOfNulls(size)
        }
    }
}
