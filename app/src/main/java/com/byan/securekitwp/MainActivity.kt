package com.byan.securekitwp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.byan.securekit.biometric.BiometricShield
import com.byan.securekit.core.SecureResult
import com.byan.securekit.biometric.UiProtection
import com.byan.securekitwp.data.repository.SecurityRepository
import com.byan.securekitwp.ui.viewmodel.SecurityViewModel
import com.byan.securekitwp.ui.viewmodel.SecurityViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: SecurityViewModel
    private val biometricShield = BiometricShield()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Setup ViewModel
        val repository = SecurityRepository(applicationContext)
        val factory = SecurityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SecurityViewModel::class.java]

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        val btnCheck = findViewById<Button>(R.id.btnCheckSecurity)
        val btnTestNetwork = findViewById<Button>(R.id.btnTestNetwork)
        val btnEnableScreenProtection = findViewById<Button>(R.id.btnEnableScreenProtection)
        val btnTestTapjacking = findViewById<Button>(R.id.btnTestTapjacking)
        val btnClearClipboard = findViewById<Button>(R.id.btnClearClipboard)
        
        // Vault / Secure Input
        val btnSaveSecret = findViewById<Button>(R.id.btnSaveSecret)
        val btnReadSecret = findViewById<Button>(R.id.btnReadSecret)
        val etSecureInput = findViewById<EditText>(R.id.etSecureInput)
        val btnSaveSecureArray = findViewById<Button>(R.id.btnSaveSecureArray)
        val btnReadSecureArray = findViewById<Button>(R.id.btnReadSecureArray)
        
        // Security checks
        btnCheck.setOnClickListener {
            viewModel.checkSecurity()
        }
        btnTestNetwork.setOnClickListener {
            viewModel.checkSecurity()
        }
        
        // UI Protection
        btnEnableScreenProtection.setOnClickListener {
            UiProtection.enableScreenProtection(this)
            Toast.makeText(this, "FLAG_SECURE Aktif! Coba Screenshot...", Toast.LENGTH_LONG).show()
        }
        btnTestTapjacking.setOnClickListener {
            UiProtection.preventTapjacking(btnTestTapjacking)
            Toast.makeText(this, "Anti-Tapjacking aktif di tombol ini!", Toast.LENGTH_SHORT).show()
        }
        btnClearClipboard.setOnClickListener {
            UiProtection.clearClipboard(this)
            Toast.makeText(this, "Isi Clipboard telah dihapus", Toast.LENGTH_SHORT).show()
        }

        // Tink Vault Data Saving via Repository
        btnSaveSecret.setOnClickListener {
            lifecycleScope.launch {
                val repository = SecurityRepository(applicationContext)
                when (val result = repository.saveEncryptedToken("SESSION_TOKEN_SECURE_123")) {
                    is SecureResult.Success -> Toast.makeText(this@MainActivity, "Token Tersimpan Aman!", Toast.LENGTH_SHORT).show()
                    is SecureResult.Error -> Toast.makeText(this@MainActivity, "Gagal menyimpan: \${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReadSecret.setOnClickListener {
            biometricShield.showPrompt(
                activity = this,
                title = "Akses Brankas",
                subtitle = "Gunakan sidik jari untuk membaca token",
                onResult = { result ->
                    if (result is SecureResult.Success) {
                        lifecycleScope.launch {
                            val repository = SecurityRepository(applicationContext)
                            when (val readRes = repository.readEncryptedToken()) {
                                is SecureResult.Success -> {
                                    val token = readRes.data ?: "Tidak ada data"
                                    Toast.makeText(this@MainActivity, "Akses Dibuka! (Simulated Token)", Toast.LENGTH_LONG).show()
                                }
                                is SecureResult.Error -> Toast.makeText(this@MainActivity, "Gagal baca: \${readRes.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Gagal Autentikasi Biometrik", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        btnSaveSecureArray.setOnClickListener {
            val editable = etSecureInput.text
            if (editable.isNullOrEmpty()) return@setOnClickListener

            val charArray = CharArray(editable.length)
            editable.getChars(0, editable.length, charArray, 0)
            
            lifecycleScope.launch {
                val repository = SecurityRepository(applicationContext)
                repository.saveSecurePin(charArray)
                etSecureInput.text.clear()
                Toast.makeText(this@MainActivity, "PIN dikunci di memori dan storage!", Toast.LENGTH_SHORT).show()
            }
        }

        btnReadSecureArray.setOnClickListener {
            val editable = etSecureInput.text
            if (editable.isNullOrEmpty()) return@setOnClickListener

            val charArray = CharArray(editable.length)
            editable.getChars(0, editable.length, charArray, 0)
            
            lifecycleScope.launch {
                val repository = SecurityRepository(applicationContext)
                val isValid = repository.verifySecurePin(charArray)
                if (isValid) {
                    Toast.makeText(this@MainActivity, "PIN Benar!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "PIN Salah!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        val tvStatus = findViewById<TextView>(R.id.tvSecurityStatus)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        tvStatus.text = "Status: Memeriksa Keamanan..."
                        tvStatus.setTextColor(Color.GRAY)
                    } else {
                        if (state.isAppSafe) {
                            tvStatus.text = "Status: AMAN ✅\nPerangkat & Jaringan Bersih."
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                        } else {
                            val issues = mutableListOf<String>()
                            if (state.securityStatus?.isRooted == true) issues.add("Rooted")
                            if (state.securityStatus?.isHooked == true) issues.add("Hooked")
                            if (state.securityStatus?.isEmulator == true) issues.add("Emulator")
                            if (state.securityStatus?.isDebuggerAttached == true) issues.add("Debugger")
                            if (state.networkStatus?.isProxyEnabled == true) issues.add("Proxy")
                            if (state.networkStatus?.isVpnEnabled == true) issues.add("VPN")
                            
                            tvStatus.text = "Status: BAHAYA ❌\nTerdeteksi: \${issues.joinToString()}"
                            tvStatus.setTextColor(Color.parseColor("#C62828"))
                        }
                    }
                }
            }
        }
    }
}