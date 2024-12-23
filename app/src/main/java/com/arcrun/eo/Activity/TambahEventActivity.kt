package com.arcrun.eo.Activity

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arcrun.eo.Models.EventModels
import com.arcrun.eo.Models.LokasiModels
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityTambahEventBinding
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
import java.util.Calendar

class TambahEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTambahEventBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val kategoriList = listOf("Lari 5K", "Lari 10K", "Maraton")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.etKategori.adapter = adapter

        binding.etDlEvent.setOnClickListener {
            showDatePicker { date -> binding.etDlEvent.setText(date) }
        }

        binding.etMulaiEvent.setOnClickListener {
            showDatePicker { date -> binding.etMulaiEvent.setText(date) }
        }

        binding.btnPilihGambar.setOnClickListener {
            pickImageFromGallery()
        }

        binding.etLokasi.setOnClickListener {
            val intent = Intent(this, CariLokasiActivity::class.java)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
        }

        binding.saveBtn.setOnClickListener {
            saveEventToFirebase()
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
                    Toast.makeText(this@TambahEventActivity, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TambahEventActivity, "Gagal memuat nama user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
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

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
        if (checkAndRequestPermissions()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
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
            Log.d("TambahEventActivity", "Tag lokasi: ${binding.etLokasi.tag}")
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                Glide.with(this)
                    .load(selectedImageUri)
                    .apply(RequestOptions().transform(RoundedCorners(30)))
                    .into(binding.ivPreviewGambar)

                binding.ivPreviewGambar.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Gagal mendapatkan gambar dari galeri.", Toast.LENGTH_SHORT).show()
            }
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

    private fun saveEventToFirebase() {
        val namaEvent = binding.etTittle.text.toString()
        val tanggalAkhirDaftar = binding.etDlEvent.text.toString()
        val tanggalMulai = binding.etMulaiEvent.text.toString()
        val deskripsi = binding.etDeskripsi.text.toString()
        val benefit = binding.etBenefit.text.toString()
        val kategori = binding.etKategori.selectedItem.toString()
        val harga = binding.etHarga.text.toString().toIntOrNull()
        val kuota = binding.etKuota.text.toString().toIntOrNull()
        val lokasiNama = binding.etLokasi.text.toString()
        val (latitude, longitude) = binding.etLokasi.tag as? Pair<Double, Double> ?: Pair(0.0, 0.0)

        if (namaEvent.isEmpty() || tanggalAkhirDaftar.isEmpty() || tanggalMulai.isEmpty() || deskripsi.isEmpty()
            || benefit.isEmpty() || kategori.isEmpty() || harga == null || kuota == null
            || lokasiNama.isEmpty() || latitude == 0.0 || longitude == 0.0 || selectedImageUri == null
        ) {
            Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            return
        }

        uploadImageToFirebase(selectedImageUri!!) { imageUrl ->
            if (imageUrl != null) {
                val lokasiData = LokasiModels(
                    namaLokasi = lokasiNama,
                    latitude = latitude,
                    longitude = longitude
                )

                val tiketId = database.getReference("TiketEvents").push().key ?: return@uploadImageToFirebase

                val event = EventModels(
                    batas_akhir = tanggalAkhirDaftar,
                    deskripsi = deskripsi,
                    gambar = imageUrl,
                    harga = harga,
                    kategori = kategori,
                    kuota = kuota,
                    lokasi = lokasiData,
                    nama_event = namaEvent,
                    status_event = "active",
                    waktu_mulai = tanggalMulai,
                    benefit = benefit
                )

                database.getReference("TiketEvents")
                    .child(tiketId)
                    .setValue(event)
                    .addOnSuccessListener {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            database.getReference("EventOrganizer/$userId/event")
                                .child(tiketId)
                                .setValue(true)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Event berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal menyimpan relasi user-event.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menyimpan event, coba lagi.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Gagal mengunggah gambar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val LOCATION_REQUEST_CODE = 2001
        const val REQUEST_IMAGE_PICK = 1001
        const val REQUEST_PERMISSION = 1002
    }
}