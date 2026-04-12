package com.memorytag.app.ui

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.memorytag.app.data.model.CreateMemoryUiState
import com.memorytag.app.databinding.ActivityCreateMemoryBinding
import com.memorytag.app.nfc.NfcWriter
import com.memorytag.app.ui.viewmodel.CreateMemoryViewModel
import kotlinx.coroutines.launch

class CreateMemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMemoryBinding
    private val viewModel: CreateMemoryViewModel by viewModels()

    private var pendingMemoryId: String? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setupNfcPendingIntent()
        binding.backButton.setOnClickListener { finish() }

        binding.createButton.setOnClickListener {
            hideKeyboard()
            // Lire tous les champs — obligatoires + optionnels
            viewModel.createMemory(
                title       = binding.titleInput.text.toString(),
                location    = binding.locationInput.text.toString(),
                description = binding.descriptionInput.text.toString(),
                date        = binding.dateInput.text.toString(),
                photosRaw   = binding.photosInput.text.toString()
            )
        }

        observeViewModel()
    }

    private fun setupNfcPendingIntent() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onResume() {
        super.onResume()
        if (pendingMemoryId != null)
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val id  = pendingMemoryId ?: return

        val success = NfcWriter.writeTextToTag(tag, id)
        if (success) {
            nfcAdapter?.disableForegroundDispatch(this)
            pendingMemoryId = null
            binding.nfcWriteStatus.text = "✓ Tag programmé avec succès !"
            binding.nfcWriteStatus.setTextColor(0xFF34C759.toInt())
            Snackbar.make(binding.root, "Tag NFC programmé ! Scannez-le pour voir le souvenir.", Snackbar.LENGTH_LONG)
                .setBackgroundTint(0xFF1C1C1E.toInt())
                .setTextColor(0xFFFFFFFF.toInt())
                .show()
            binding.root.postDelayed({ finish() }, 2500)
        } else {
            binding.nfcWriteStatus.text = "Échec — tag non compatible ou protégé en écriture"
            binding.nfcWriteStatus.setTextColor(0xFFFF453A.toInt())
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreateMemoryUiState.Idle    -> setUiEnabled(true)
                        is CreateMemoryUiState.Loading -> setUiEnabled(false)
                        is CreateMemoryUiState.Success -> onCreationSuccess(state.memoryId)
                        is CreateMemoryUiState.Error   -> onCreationError(state.message)
                    }
                }
            }
        }

        // Observer les coordonnées géocodées
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.coords.collect { coords ->
                    if (coords != null) {
                        binding.coordsContainer.visibility = View.VISIBLE
                        binding.coordsText.text =
                            "%.4f, %.4f".format(coords.first, coords.second)
                    }
                }
            }
        }
    }

    private fun onCreationSuccess(memoryId: String) {
        setUiEnabled(true)
        pendingMemoryId = memoryId

        binding.createdIdContainer.visibility = View.VISIBLE
        binding.createdIdText.text = memoryId
        binding.createdIdContainer.alpha = 0f
        binding.createdIdContainer.animate().alpha(1f).setDuration(400).start()

        binding.nfcWritePrompt.visibility = View.VISIBLE
        binding.nfcWriteStatus.visibility = View.VISIBLE
        binding.nfcWriteStatus.text = "Approchez un tag NFC vierge pour le programmer…"
        binding.nfcWriteStatus.setTextColor(0xFF8E8E93.toInt())

        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)

        Snackbar.make(binding.root, "Souvenir créé ! Approchez un tag NFC vierge.", Snackbar.LENGTH_LONG)
            .setBackgroundTint(0xFF1C1C1E.toInt())
            .setTextColor(0xFFFFFFFF.toInt())
            .setAction("Copier l'ID") { copyToClipboard(memoryId) }
            .setActionTextColor(0xFFFF453A.toInt())
            .show()
    }

    private fun onCreationError(message: String) {
        setUiEnabled(true)
        binding.createButton.animate()
            .translationX(12f).setDuration(60).withEndAction {
                binding.createButton.animate().translationX(-12f).setDuration(60).withEndAction {
                    binding.createButton.animate().translationX(0f).setDuration(60).start()
                }.start()
            }.start()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(0xFF2C1B1B.toInt())
            .setTextColor(0xFFFF453A.toInt())
            .show()
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.titleInput.isEnabled        = enabled
        binding.locationInput.isEnabled     = enabled
        binding.descriptionInput.isEnabled  = enabled
        binding.dateInput.isEnabled         = enabled
        binding.photosInput.isEnabled       = enabled
        binding.createButton.isEnabled      = enabled
        binding.loadingIndicator.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.createButton.alpha          = if (enabled) 1.0f else 0.4f
    }

    private fun copyToClipboard(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Memory ID", text))
        Snackbar.make(binding.root, "ID copié !", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(0xFF1C1C1E.toInt()).setTextColor(0xFFFFFFFF.toInt()).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
