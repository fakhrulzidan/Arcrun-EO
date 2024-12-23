package com.arcrun.eo.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arcrun.eo.Adapter.EventAdapter
import com.arcrun.eo.Models.EventModels
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: EventAdapter
    private val items = ArrayList<EventModels>()
    private val eventIds = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.addBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, TambahEventActivity::class.java)
            startActivity(intent)
        }

        binding.profile.setOnClickListener {
            showProfileMenu()
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "User terdeteksi: ${currentUser.uid}")
            loadUserName(currentUser.uid)
            loadUserEvents(currentUser.uid)
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
                    binding.namaUser.text = userName ?: "User"
                } else {
                    Toast.makeText(this@MainActivity, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal memuat nama user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserEvents(userId: String) {
        val userRef = database.getReference("EventOrganizer/$userId/event")
        val eventRef = database.getReference("TiketEvents")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userEventIds = snapshot.children.mapNotNull { it.key }
                    eventRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(eventSnapshot: DataSnapshot) {
                            if (eventSnapshot.exists()) {
                                items.clear()
                                eventIds.clear()
                                for (eventId in userEventIds) {
                                    val event = eventSnapshot.child(eventId).getValue(EventModels::class.java)
                                    if (event != null) {
                                        items.add(event)
                                        eventIds.add(eventId)
                                    }
                                }
                                setupRecyclerView()
                            } else {
                                Toast.makeText(this@MainActivity, "Tidak ada event ditemukan.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MainActivity, "Gagal memuat event: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this@MainActivity, "User tidak memiliki event.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Gagal memuat data user: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(items, eventIds, this, ::deleteEvent)
        binding.viewEvent.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.viewEvent.adapter = adapter
    }

    private fun deleteEvent(eventId: String, position: Int) {
        if (position < 0 || position >= items.size) {
            Toast.makeText(this, "Indeks tidak valid. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val tiketEventsRef = database.getReference("TiketEvents").child(eventId)
        val userEventsRef = database.getReference("EventOrganizer/$userId/event").child(eventId)

        tiketEventsRef.removeValue()
            .addOnSuccessListener {
                userEventsRef.removeValue()
                    .addOnSuccessListener {
                        items.removeAt(position)
                        eventIds.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(this, "Event berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus ID event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}