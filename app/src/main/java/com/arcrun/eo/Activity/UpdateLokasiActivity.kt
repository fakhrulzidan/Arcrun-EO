package com.arcrun.eo.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityUpdateLokasiBinding
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class UpdateLokasiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateLokasiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateLokasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.map_api))
        }

        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment_update) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val resultIntent = Intent().apply {
                    putExtra("selected_location", "${place.name}, ${place.address}")
                    putExtra("latitude", place.latLng?.latitude)
                    putExtra("longitude", place.latLng?.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            override fun onError(status: Status) {
                Toast.makeText(this@UpdateLokasiActivity, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
