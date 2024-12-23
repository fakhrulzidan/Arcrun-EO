package com.arcrun.eo.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.arcrun.eo.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("SignInActivity", "onCreate: SignInActivity dimulai")

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signInBtn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                Log.d("SignInActivity", "onClick: Login dengan email $email")
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("SignInActivity", "Login berhasil untuk user: ${firebaseAuth.currentUser?.uid}")

                        // Simpan status login
                        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("userId", firebaseAuth.currentUser?.uid)
                        editor.putLong("loginTime", System.currentTimeMillis())
                        editor.apply()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("SignInActivity", "Login gagal: ${task.exception?.message}")
                        Toast.makeText(
                            this,
                            "Login gagal: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Log.w("SignInActivity", "Email atau password kosong.")
                Toast.makeText(this, "Masukkan Email dan Password Anda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d("SignInActivity", "onStart: Mengecek status login")

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val loginTime = sharedPreferences.getLong("loginTime", 0L)
        val currentTime = System.currentTimeMillis()
        val currentUser = firebaseAuth.currentUser // Periksa status dari FirebaseAuth
        val sessionTimeout = 24 * 60 * 60 * 1000 // 24 jam dalam milidetik

        Log.d("SignInActivity", "isLoggedIn: $isLoggedIn, currentUser: $currentUser, timeDiff: ${currentTime - loginTime}")

        if (isLoggedIn && (currentTime - loginTime <= sessionTimeout)) {
            if (currentUser != null) {
                Log.d("SignInActivity", "User masih login. Berpindah ke MainActivity.")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.w("SignInActivity", "FirebaseAuth tidak mendeteksi user login. Menghapus session.")
                clearSession()
            }
        } else if (isLoggedIn) {
            Log.d("SignInActivity", "Sesi login sudah kadaluarsa.")
            clearSession()
        }
    }

    private fun clearSession() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        firebaseAuth.signOut() // Pastikan FirebaseAuth logout
        Log.d("SignInActivity", "Session cleared dan FirebaseAuth signOut.")
        Toast.makeText(this, "Sesi login Anda telah berakhir. Silakan login kembali.", Toast.LENGTH_SHORT).show()
    }

}

//class SignInActivity : AppCompatActivity() {
//    private lateinit var binding: ActivitySignInBinding
//    private lateinit var firebaseAuth: FirebaseAuth
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivitySignInBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        firebaseAuth = FirebaseAuth.getInstance()
//
//        binding.signInBtn.setOnClickListener {
//            val email = binding.etEmail.text.toString()
//            val pass = binding.etPassword.text.toString()
//
//            if (email.isNotEmpty() && pass.isNotEmpty()) {
//                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        val currentUser = firebaseAuth.currentUser
//                        if (currentUser != null) {
//                            val userRef = FirebaseDatabase.getInstance()
//                                .getReference("EventOrganizer")
//                                .child(currentUser.uid)
//
//                            // Cek apakah data pengguna sudah ada
//                            userRef.get().addOnSuccessListener { snapshot ->
//                                if (!snapshot.exists()) {
//                                    // Inisialisasi data pengguna baru
//                                    val userData = mapOf(
//                                        "email" to currentUser.email,
//                                        "name" to (currentUser.displayName ?: "User"),
//                                        "event" to mapOf<String, Boolean>() // Event kosong
//                                    )
//                                    userRef.updateChildren(userData)
//                                        .addOnSuccessListener {
//                                            Log.d("SignInActivity", "User data initialized successfully.")
//                                        }
//                                        .addOnFailureListener { exception ->
//                                            Log.e("SignInActivity", "Error initializing user data: ${exception.message}")
//                                        }
//                                } else {
//                                    Log.d("SignInActivity", "User data already exists.")
//                                }
//                            }.addOnFailureListener { exception ->
//                                Log.e("SignInActivity", "Error fetching user data: ${exception.message}")
//                            }
//                        }
//
//                        // Berpindah ke MainActivity setelah login berhasil
//                        val intent = Intent(this, MainActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    } else {
//                        Toast.makeText(
//                            this,
//                            "Login gagal: ${task.exception?.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Masukkan Email dan Password Anda", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//    }
//}