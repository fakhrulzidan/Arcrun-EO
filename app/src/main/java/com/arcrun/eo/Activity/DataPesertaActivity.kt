package com.arcrun.eo.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcrun.eo.Adapter.PesertaAdapter
import com.arcrun.eo.Models.PesertaModels
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityDataPesertaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DataPesertaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDataPesertaBinding
    private lateinit var database: DatabaseReference
    private val pesertaList = ArrayList<PesertaModels>()
    private lateinit var adapter: PesertaAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataPesertaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("Peserta")
        adapter = PesertaAdapter(pesertaList)
        binding.viewDataPeserta.layoutManager = LinearLayoutManager(this)
        binding.viewDataPeserta.adapter = adapter

        auth = FirebaseAuth.getInstance()

        val eventId = intent.getStringExtra("event_id")
        if (eventId != null) {
            fetchPesertaData(eventId)
        } else {
            Toast.makeText(this, "Event ID tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
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
                    Toast.makeText(this@DataPesertaActivity, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DataPesertaActivity, "Gagal memuat nama user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPesertaData(eventId: String) {
        database.orderByChild("eventId").equalTo(eventId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        pesertaList.clear()
                        for (pesertaSnapshot in snapshot.children) {
                            val peserta = pesertaSnapshot.getValue(PesertaModels::class.java)
                            if (peserta != null) {
                                pesertaList.add(peserta)
                            }
                        }
                        adapter.notifyDataSetChanged()

                        if (pesertaList.isEmpty()) {
                            binding.tvNoPeserta.visibility = View.VISIBLE
                            binding.viewDataPeserta.visibility = View.GONE
                        } else {
                            binding.tvNoPeserta.visibility = View.GONE
                            binding.viewDataPeserta.visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvNoPeserta.visibility = View.VISIBLE
                        binding.viewDataPeserta.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DataPesertaActivity, "Gagal memuat data peserta: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}
