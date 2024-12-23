package com.arcrun.eo.Models

import android.os.Parcel
import android.os.Parcelable

data class EventModels(
    var batas_akhir: String? = null,
    var deskripsi: String? = null,
    var gambar: String? = null,
    var harga: Int = 0,
    var kategori: String? = null,
    var kuota: Int = 0,
    var lokasi: LokasiModels? = null,
    var nama_event: String? = null,
    var status_event: String? = null,
    var waktu_mulai: String? = null,
    var benefit: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readParcelable(LokasiModels::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(batas_akhir)
        parcel.writeString(deskripsi)
        parcel.writeString(gambar)
        parcel.writeInt(harga)
        parcel.writeString(kategori)
        parcel.writeInt(kuota)
        parcel.writeParcelable(lokasi, flags)
        parcel.writeString(nama_event)
        parcel.writeString(status_event)
        parcel.writeString(waktu_mulai)
        parcel.writeString(benefit)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EventModels> {
        override fun createFromParcel(parcel: Parcel): EventModels {
            return EventModels(parcel)
        }

        override fun newArray(size: Int): Array<EventModels?> {
            return arrayOfNulls(size)
        }
    }
}