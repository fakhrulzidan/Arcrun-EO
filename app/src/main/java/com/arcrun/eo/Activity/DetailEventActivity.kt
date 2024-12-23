package com.arcrun.eo.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityDetailEventBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailEventBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapFragmentContainer, mapFragment)
            .commit()

        val eventId = intent.getStringExtra("event_id")
        val eventName = intent.getStringExtra("event_name")
        val eventDesc = intent.getStringExtra("event_desc")
        val eventBenefit = intent.getStringExtra("event_benefit")
        val batasAkhirPendaftaran = intent.getStringExtra("batas_akhir")
        val waktuMulaiEvent = intent.getStringExtra("waktu_mulai")
        val kuota = intent.getIntExtra("kuota", 0)
        val eventImage = intent.getStringExtra("event_image")
        val eventPrice = intent.getIntExtra("event_price", 0)

        Log.d("EventAdapter", "Kuota dikirim: ${kuota}")

        binding.dataPeserta.setOnClickListener {
            val intent = Intent(this, DataPesertaActivity::class.java)
            intent.putExtra("event_id", eventId)
            startActivity(intent)
        }

        if (eventId != null) {
            fetchEventLocation(eventId, mapFragment)
        }

        if (eventName != null && eventDesc != null) {
            displayEventDetails(eventName, eventDesc, eventImage, eventBenefit, batasAkhirPendaftaran, waktuMulaiEvent, eventPrice, kuota)
        } else {
            Log.e("DetailEventActivity", "Event data not found!")
        }

        binding.profile.setOnClickListener {
            showProfileMenu()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "User terdeteksi: ${currentUser.uid}")
            loadUserName(currentUser.uid)
        } else {
            Log.w("MainActivity", "User tidak login. Mengarahkan ke SignInActivity.")
            Toast.makeText(this, "Silakan login untuk melanjutkan.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        Log.d("MainActivity", "onCreate: MainActivity dimulai")
    }

    private fun showProfileMenu() {
        val popupMenu = PopupMenu(this, binding.profile)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        // Kembali ke SignInActivity
        finish()
        startActivity(Intent(this, SignInActivity::class.java))
    }
    private fun loadUserName(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("EventOrganizer/$userId")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    if (!userName.isNullOrEmpty()) {
                        binding.namaUser.text = userName
                    } else {
                        binding.namaUser.text = "User"
                    }
                } else {
                    Toast.makeText(this@DetailEventActivity, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailEventActivity, "Gagal memuat nama user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchEventLocation(eventId: String, mapFragment: SupportMapFragment) {
        database = FirebaseDatabase.getInstance().getReference("TiketEvents")
        database.child(eventId).child("lokasi").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                val locationName = snapshot.child("namaLokasi").getValue(String::class.java) ?: "Unknown Location"

                mapFragment.getMapAsync { googleMap ->
                    setupMap(googleMap, latitude, longitude, locationName)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DetailEventActivity", "Failed to fetch location: ${error.message}")
            }
        })
    }

    private fun setupMap(googleMap: GoogleMap, latitude: Double, longitude: Double, locationName: String) {
        if (latitude != 0.0 && longitude != 0.0) {
            val eventLocation = LatLng(latitude, longitude)
            googleMap.addMarker(
                MarkerOptions()
                    .position(eventLocation)
                    .title(locationName)
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 15f))
        } else {
            Log.e("DetailEventActivity", "Invalid location data")
        }
    }

    private fun displayEventDetails(
        eventName: String,
        eventDesc: String,
        eventImage: String?,
        eventBenefit: String?,
        batasAkhirPendaftaran: String?,
        waktuMulaiEvent: String?,
        eventPrice: Int,
        kuota : Int) {

        binding.eventNameDetail.text = eventName
        binding.deskripsiTxt.text = eventDesc
        binding.benefitTxt.text = eventBenefit
        binding.DlEvent.text = batasAkhirPendaftaran
        binding.MulaiEvent.text = waktuMulaiEvent
        binding.deskripsiTxt.text = eventDesc
        binding.hargaEventDetail.text = "Rp. $eventPrice"

        if (kuota > 0) {
            binding.statusKuotaTxt.text = "Kuota tersedia: $kuota"
            binding.statusKuotaTxt.setTextColor(ContextCompat.getColor(this, R.color.greenTxt))
        } else if (kuota == 0) {
            binding.statusKuotaTxt.text = "Kuota penuh"
            binding.statusKuotaTxt.setTextColor(ContextCompat.getColor(this, R.color.redTxt))
        } else {
            binding.statusKuotaTxt.text = "Informasi kuota tidak tersedia"
            binding.statusKuotaTxt.setTextColor(ContextCompat.getColor(this, R.color.greyIcon))
        }

        if (eventImage != null) {
            Glide.with(this)
                .load(eventImage)
                .apply(RequestOptions().error(R.drawable.imagenotfound))
                .into(binding.imgEvent)
        } else {
            binding.imgEvent.setImageResource(R.drawable.imagenotfound)
        }
    }

}
