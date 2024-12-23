package com.arcrun.eo.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityCariLokasiBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class CariLokasiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCariLokasiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCariLokasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.map_api))
        }

        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.d("CariLokasiActivity", "Latitude: ${place.latLng?.latitude}, Longitude: ${place.latLng?.longitude}")
                if (place.latLng?.latitude == null || place.latLng?.longitude == null) {
                    Toast.makeText(this@CariLokasiActivity, "Lokasi tidak valid. Silakan pilih lokasi kembali.", Toast.LENGTH_SHORT).show()
                    return
                }

                val resultIntent = Intent().apply {
                    putExtra("selected_location", "${place.name}, ${place.address}")
                    place.latLng?.let {
                        putExtra("latitude", it.latitude)
                        putExtra("longitude", it.longitude)
                    } ?: run {
                        Toast.makeText(this@CariLokasiActivity, "Lokasi tidak valid. Silakan pilih lokasi kembali.", Toast.LENGTH_SHORT).show()
                    }
                    Log.d("CariLokasiActivity", "Place selected: ${place.name}, LatLng: ${place.latLng}")
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@CariLokasiActivity, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}