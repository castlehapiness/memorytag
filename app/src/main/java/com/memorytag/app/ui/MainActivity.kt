package com.memorytag.app.ui

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.memorytag.app.data.model.MemoryUiState
import com.memorytag.app.databinding.ActivityMainBinding
import com.memorytag.app.nfc.NfcHelper
import com.memorytag.app.ui.viewmodel.MemoryViewModel
import kotlinx.coroutines.launch

/**
 * Écran principal : invite l'utilisateur à scanner un tag NFC.
 * Design minimaliste — une seule animation de pulsation + message.
 *
 * Cycle de vie NFC :
 *   onResume  → enableForegroundDispatch (capture les tags)
 *   onPause   → disableForegroundDispatch (libère)
 *   onNewIntent → reçoit l'intent NFC et déclenche le chargement
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcHelper: NfcHelper
    private val viewModel: MemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcHelper = NfcHelper(this)

        setupUI()
        observeViewModel()
        handleNfcIntent(intent) // Si lancé via intent NFC direct
    }

    private fun setupUI() {
        // Affiche un message si NFC non disponible
        if (!nfcHelper.isNfcAvailable) {
            binding.nfcStatusText.text = "NFC non disponible sur cet appareil"
            binding.nfcRippleView.visibility = View.GONE
        } else if (!nfcHelper.isNfcEnabled) {
            binding.nfcStatusText.text = "Activez le NFC dans les paramètres"
        }

        // Bouton debug : charge directement un mock (pratique en dev sans tag NFC)
        binding.debugButton.setOnClickListener {
            viewModel.loadMemory("PARIS_001")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MemoryUiState.Idle -> showIdleState()
                        is MemoryUiState.Loading -> showLoadingState()
                        is MemoryUiState.Success -> {
                            // Lance l'écran détail avec les données
                            val intent = Intent(this@MainActivity, MemoryDetailActivity::class.java)
                            intent.putExtra(MemoryDetailActivity.EXTRA_MEMORY_ID, state.memory.id)
                            startActivity(intent)
                            // Reset après navigation
                            viewModel.resetState()
                        }
                        is MemoryUiState.Error -> showErrorState(state.message)
                    }
                }
            }
        }
    }

    private fun showIdleState() {
        binding.loadingGroup.visibility = View.GONE
        binding.idleGroup.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.idleGroup.visibility = View.GONE
        binding.loadingGroup.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.loadingGroup.visibility = View.GONE
        binding.idleGroup.visibility = View.VISIBLE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    // ─── Cycle de vie NFC ────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        nfcHelper.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableForegroundDispatch()
    }

    /**
     * Reçoit l'intent NFC quand l'app est déjà au premier plan.
     * launchMode="singleTop" garantit que cette méthode est appelée
     * plutôt que de créer une nouvelle instance.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (intent.action in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )
        ) {
            val memoryId = nfcHelper.extractMemoryId(intent)
            if (memoryId != null) {
                viewModel.loadMemory(memoryId)
            } else {
                showErrorState("Tag NFC non reconnu")
            }
        }
    }
}
