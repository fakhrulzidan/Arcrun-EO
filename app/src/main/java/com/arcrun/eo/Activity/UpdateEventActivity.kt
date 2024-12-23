package com.arcrun.eo.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arcrun.eo.Activity.TambahEventActivity.Companion
import com.arcrun.eo.Activity.TambahEventActivity.Companion.LOCATION_REQUEST_CODE
import com.arcrun.eo.Activity.TambahEventActivity.Companion.REQUEST_IMAGE_PICK
import com.arcrun.eo.Activity.TambahEventActivity.Companion.REQUEST_PERMISSION
import com.arcrun.eo.Models.EventModels
import com.arcrun.eo.Models.LokasiModels
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityUpdateEventBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class UpdateEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateEventBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var event: EventModels? = null
    private var eventId: String? = null
    private var statusEvent: String = "active"
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Terima objek EventModels dari Intent
        event = intent.getParcelableExtra("object")
        eventId = intent.getStringExtra("eventId")
        if (eventId != null) {
            Log.d("UpdateEventActivity", "eventId diterima: $eventId")
        } else {
            Log.e("UpdateEventActivity", "eventId tidak ditemukan dalam Intent")
        }

        event?.let {
            populateData(it)
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val kategoriList = listOf("Lari 5K", "Lari 10K", "Maraton")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.etKategori.adapter = adapter

//        val kategoriIndex = getKategoriIndex(event!!.kategori)
//        Log.d("UpdateEventActivity", "Setting spinner selection to index: $kategoriIndex")
        // Tunggu hingga spinner selesai diinisialisasi, baru set selection
        binding.etKategori.post {
            binding.etKategori.setSelection(getKategoriIndex(event?.kategori))
        }

        binding.saveBtn.setOnClickListener {
            updateEventToFirebase()
        }

        binding.statusEventTxt.setOnClickListener {
            toggleEventStatus()
        }

        binding.btnPilihGambar.setOnClickListener {
            pickImageFromGallery()
        }
        binding.etLokasi.setOnClickListener {
            val intent = Intent(this, UpdateLokasiActivity::class.java)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
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
        val userRef = database.getReference("EventOrganizer/$userId")

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
                    Toast.makeText(this@UpdateEventActivity, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UpdateEventActivity, "Gagal memuat nama user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, TambahEventActivity.REQUEST_IMAGE_PICK)
        if (checkAndRequestPermissions()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, TambahEventActivity.REQUEST_IMAGE_PICK)
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSION)
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            val location = data?.getStringExtra("selected_location")
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)
            if (location != null) {
                binding.etLokasi.setText(location)
                binding.etLokasi.tag = Pair(latitude, longitude)
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            Glide.with(this)
                .load(selectedImageUri)
                .apply(RequestOptions().transform(RoundedCorners(30)))
                .into(binding.ivPreviewGambar)
        }
    }

    private fun toggleEventStatus() {
        statusEvent = if (statusEvent == "active") "finish" else "active"
        binding.statusEventTxt.text = statusEvent.capitalize()
    }

    private fun getKategoriIndex(kategori: String?): Int {
        val kategoriList = listOf("Lari 5K", "Lari 10K", "Maraton")
        return kategoriList.indexOf(kategori).takeIf { it != -1 } ?: 0
    }

    private fun populateData(event: EventModels) {
        binding.etTittle.setText(event.nama_event)
        binding.etDlEvent.setText(event.batas_akhir)
        binding.etMulaiEvent.setText(event.waktu_mulai)
        binding.etDeskripsi.setText(event.deskripsi)
        binding.etBenefit.setText(event.benefit)
        binding.etHarga.setText(event.harga.toString())
        binding.etKuota.setText(event.kuota.toString())
        binding.etLokasi.setText(event.lokasi!!.namaLokasi)
        binding.etLokasi.tag = Pair(event.lokasi!!.latitude, event.lokasi!!.longitude)

        val kategoriIndex = getKategoriIndex(event.kategori)
        Log.d("UpdateEventActivity", "Kategori dari event: ${event.kategori}, Index: $kategoriIndex")
        binding.etKategori.setSelection(kategoriIndex)

        // Set status event
        statusEvent = event.status_event ?: "active"
        binding.statusEventTxt.text = statusEvent.capitalize()

        // Load gambar lama
        Glide.with(this)
            .load(event.gambar)
            .error(R.drawable.imagenotfound)
            .into(binding.ivPreviewGambar)
    }
    private fun updateEventToFirebase() {
        val namaEvent = binding.etTittle.text.toString()
        val batasAkhir = binding.etDlEvent.text.toString()
        val waktuMulai = binding.etMulaiEvent.text.toString()
        val deskripsi = binding.etDeskripsi.text.toString()
        val benefit = binding.etBenefit.text.toString()
        val kategori = binding.etKategori.selectedItem.toString()
        val harga = binding.etHarga.text.toString().toIntOrNull() ?: 0
        val kuota = binding.etKuota.text.toString().toIntOrNull() ?: 0
        val lokasiNama = binding.etLokasi.text.toString()
        val (latitude, longitude) = binding.etLokasi.tag as? Pair<Double, Double> ?: Pair(0.0, 0.0)

        if (eventId != null) {
            if (selectedImageUri != null) {
                // Upload gambar baru jika dipilih
                uploadImageToFirebase(selectedImageUri!!) { imageUrl ->
                    if (imageUrl != null) {
                        updateEventData(namaEvent, batasAkhir, waktuMulai, deskripsi, benefit, kategori, harga, kuota, lokasiNama, latitude, longitude, imageUrl)
                    } else {
                        Toast.makeText(this, "Gagal mengunggah gambar.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Tidak ada gambar baru, gunakan gambar lama
                updateEventData(namaEvent, batasAkhir, waktuMulai, deskripsi, benefit, kategori, harga, kuota, lokasiNama, latitude, longitude, event?.gambar)
            }
        } else {
            Toast.makeText(this, "ID Event tidak ditemukan.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri, callback: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("event_images/${System.currentTimeMillis()}.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun updateEventData(
        namaEvent: String,
        batasAkhir: String,
        waktuMulai: String,
        deskripsi: String,
        benefit: String,
        kategori: String,
        harga: Int,
        kuota: Int,
        lokasiNama: String,
        latitude: Double,
        longitude: Double,
        gambarUrl: String?
    ) {
        val updatedLokasi = LokasiModels(namaLokasi = lokasiNama, latitude = latitude, longitude = longitude)
        val databaseRef = FirebaseDatabase.getInstance().getReference("TiketEvents").child(eventId!!)
        val updatedData = mapOf(
            "nama_event" to namaEvent,
            "batas_akhir" to batasAkhir,
            "waktu_mulai" to waktuMulai,
            "deskripsi" to deskripsi,
            "benefit" to benefit,
            "kategori" to kategori,
            "harga" to harga,
            "kuota" to kuota,
            "gambar" to gambarUrl,
            "status_event" to statusEvent,
            "lokasi" to updatedLokasi
        )

        databaseRef.updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1001
        const val REQUEST_PERMISSION = 1002
    }
}

//    private fun updateEventToFirebase() {
//        val namaEvent = binding.etTittle.text.toString()
//        val batasAkhir = binding.etDlEvent.text.toString()
//        val waktuMulai = binding.etMulaiEvent.text.toString()
//        val deskripsi = binding.etDeskripsi.text.toString()
//        val benefit = binding.etBenefit.text.toString()
//        val kategori = binding.etKategori.selectedItem.toString()
//        val harga = binding.etHarga.text.toString().toIntOrNull() ?: 0
//        val kuota = binding.etKuota.text.toString().toIntOrNull() ?: 0
//        val lokasiNama = binding.etLokasi.text.toString()
//        val (latitude, longitude) = binding.etLokasi.tag as? Pair<Double, Double> ?: Pair(0.0, 0.0)
//
//        // Cek apakah ada gambar baru yang dipilih
//        val localImagePath = selectedImageUri?.let { saveImageToLocalDirectory(it) }
//        val updatedImagePath = localImagePath ?: event?.gambar // Gunakan gambar lama jika tidak ada gambar baru
//
//        val updatedLokasi = LokasiModels(namaLokasi = lokasiNama, latitude = latitude, longitude = longitude)
//
//        if (eventId != null) {
//            val databaseRef = FirebaseDatabase.getInstance().getReference("TiketEvents").child(eventId!!)
//            val updatedData = mapOf(
//                "nama_event" to namaEvent,
//                "batas_akhir" to batasAkhir,
//                "waktu_mulai" to waktuMulai,
//                "deskripsi" to deskripsi,
//                "benefit" to benefit,
//                "kategori" to kategori,
//                "harga" to harga,
//                "kuota" to kuota,
//                "gambar" to updatedImagePath,
//                "status_event" to statusEvent,
//                "lokasi" to updatedLokasi
//
//            )
//            databaseRef.updateChildren(updatedData)
//                .addOnSuccessListener {
//                    Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
//                    val intent = Intent(this, MainActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                    startActivity(intent)
//                    finish()
//                }
//                .addOnFailureListener {
//                    Toast.makeText(this, "Gagal memperbarui data: ${it.message}", Toast.LENGTH_SHORT).show()
//                }
//        } else {
//            Toast.makeText(this, "ID Event tidak ditemukan.", Toast.LENGTH_SHORT).show()
//        }
//    }