package com.memorytag.app.ui

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
        startPulseAnimation()
        observeViewModel()
        handleNfcIntent(intent)
    }

    private fun setupUI() {
        if (!nfcHelper.isNfcAvailable) {
            binding.nfcStatusText.text = "NFC non disponible sur cet appareil"
            binding.nfcIconContainer.visibility = View.GONE
        } else if (!nfcHelper.isNfcEnabled) {
            binding.nfcStatusText.text = "Activez le NFC dans les paramètres"
        }

        // Bouton debug : charge un mock sans tag NFC
        binding.debugButton.setOnClickListener {
            vibrateSuccess()
            viewModel.loadMemory("PARIS_001")
        }

        // ── NOUVEAU : Bouton "Créer un souvenir" ─────────────────────────────
        binding.createMemoryButton.setOnClickListener {
            startActivity(Intent(this, CreateMemoryActivity::class.java))
        }
    }

    private fun startPulseAnimation() {
        animateRing(binding.ring1, 0L)
        animateRing(binding.ring2, 400L)
        animateRing(binding.ring3, 800L)
    }

    private fun animateRing(view: View, startDelay: Long) {
        view.alpha = 0.6f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.postDelayed({
            view.animate()
                .scaleX(1.8f).scaleY(1.8f).alpha(0f)
                .setDuration(1400)
                .withEndAction { animateRing(view, 0L) }
                .start()
        }, startDelay)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MemoryUiState.Idle    -> showIdleState()
                        is MemoryUiState.Loading -> showLoadingState()
                        is MemoryUiState.Success -> {
                            vibrateSuccess()
                            startActivity(
                                Intent(this@MainActivity, MemoryDetailActivity::class.java)
                                    .putExtra(MemoryDetailActivity.EXTRA_MEMORY_ID, state.memory.id)
                            )
                            viewModel.resetState()
                        }
                        is MemoryUiState.Error -> showErrorState(state.message)
                    }
                }
            }
        }
    }

    private fun vibrateSuccess() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
                    .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 40), -1))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 40), -1))
            }
        } catch (_: Exception) {}
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
        showIdleState()
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
        // Micro-animation shake
        binding.errorText.animate().translationX(14f).setDuration(55).withEndAction {
            binding.errorText.animate().translationX(-14f).setDuration(55).withEndAction {
                binding.errorText.animate().translationX(0f).setDuration(55).start()
            }.start()
        }.start()
    }

    override fun onResume()  { super.onResume();  nfcHelper.enableForegroundDispatch() }
    override fun onPause()   { super.onPause();   nfcHelper.disableForegroundDispatch() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleNfcIntent(intent) }

    private fun handleNfcIntent(intent: Intent) {
        if (intent.action in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED)) {
            val id = nfcHelper.extractMemoryId(intent)
            if (id != null) viewModel.loadMemory(id) else showErrorState("Tag NFC non reconnu")
        }
    }
}
