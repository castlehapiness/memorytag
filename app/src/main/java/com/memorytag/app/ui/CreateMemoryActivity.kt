package com.memorytag.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.memorytag.app.ui.viewmodel.CreateMemoryViewModel
import kotlinx.coroutines.launch
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.widget.Toast
import com.memorytag.app.nfc.NfcWriter

/**
 * Écran de création d'un nouveau souvenir.
 *
 * Flux :
 *  1. L'utilisateur remplit titre / localisation / description
 *  2. Clique "Créer le souvenir"
 *  3. Le ViewModel génère un UUID et écrit dans Firestore
 *  4. L'ID est affiché et l'application attend qu'un tag NFC soit approché
 *  5. L'application écrit automatiquement l'ID sur le tag NFC
 *  6. L'activité se ferme après écriture réussie
 */
class CreateMemoryActivity : AppCompatActivity() {
    private var pendingMemoryId: String? = null
    private lateinit var binding: ActivityCreateMemoryBinding
    private val viewModel: CreateMemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCreateButton()
        observeViewModel()

    }

    // ── Toolbar avec bouton retour ────────────────────────────────────────────

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    // ── Bouton de création ────────────────────────────────────────────────────

    private fun setupCreateButton() {
        binding.createButton.setOnClickListener {
            hideKeyboard()

            val title       = binding.titleInput.text.toString()
            val location    = binding.locationInput.text.toString()
            val description = binding.descriptionInput.text.toString()

            viewModel.createMemory(title, location, description)
        }
    }

    // ── Observation du ViewModel ──────────────────────────────────────────────

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
    }

    /**
     * Succès : affiche l'ID Firestore dans un Snackbar avec bouton "Copier".
     * L'ID est ce que l'utilisateur devra encoder sur son tag NFC.
     */
    private fun onCreationSuccess(memoryId: String) {
        setUiEnabled(true)
        pendingMemoryId = memoryId

        // Affiche l'ID généré dans le champ dédié
        binding.createdIdContainer.visibility = View.VISIBLE
        binding.createdIdText.text = memoryId

        // Animation fade-in du conteneur ID
        binding.createdIdContainer.alpha = 0f
        binding.createdIdContainer.animate().alpha(1f).setDuration(400).start()

        // Snackbar avec action "Copier"
        Snackbar.make(
            binding.root,
            "✓ Souvenir créé avec succès",
            Snackbar.LENGTH_LONG
        ).setAction("Copier l'ID") {
            copyToClipboard(memoryId)
        }.setBackgroundTint(0xFF1C1C1E.toInt())
         .setTextColor(0xFFFFFFFF.toInt())
         .setActionTextColor(0xFFFF453A.toInt())
         .show()

        // Demander à l'utilisateur d'approcher un tag NFC
        Snackbar.make(
            binding.root,
            "Souvenir créé. Approche un tag NFC pour écrire l'ID.",
            Snackbar.LENGTH_LONG
        ).setAction("Copier l'ID") {
            copyToClipboard(memoryId)
        }.setBackgroundTint(0xFF1C1C1E.toInt())
            .setTextColor(0xFFFFFFFF.toInt())
            .setActionTextColor(0xFFFF453A.toInt())
            .show()
    }

    private fun onCreationError(message: String) {
        setUiEnabled(true)

        // Animation shake sur le bouton
        binding.createButton.animate()
            .translationX(12f).setDuration(60).withEndAction {
                binding.createButton.animate()
                    .translationX(-12f).setDuration(60).withEndAction {
                        binding.createButton.animate().translationX(0f).setDuration(60).start()
                    }.start()
            }.start()

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(0xFF2C1B1B.toInt())
            .setTextColor(0xFFFF453A.toInt())
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Active ou désactive les champs et le bouton pendant le chargement.
     */
    private fun setUiEnabled(enabled: Boolean) {
        binding.titleInput.isEnabled       = enabled
        binding.locationInput.isEnabled    = enabled
        binding.descriptionInput.isEnabled = enabled
        binding.createButton.isEnabled     = enabled
        binding.loadingIndicator.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.createButton.alpha          = if (enabled) 1.0f else 0.4f
    }

    /**
     * Copie l'ID dans le presse-papier — l'utilisateur pourra le coller
     * dans une app d'écriture NFC (ex: NFC Tools).
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Memory ID", text))
        Snackbar.make(binding.root, "ID copié !", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(0xFF1C1C1E.toInt())
            .setTextColor(0xFFFFFFFF.toInt())
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun onResume() {
        super.onResume()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: return
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val memoryId = pendingMemoryId ?: return
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

        if (tag == null) {
            Toast.makeText(this, "Aucun tag NFC détecté", Toast.LENGTH_SHORT).show()
            return
        }

        val success = NfcWriter.writeTextToTag(tag, memoryId)

        if (success) {
            Toast.makeText(this, "Tag NFC écrit avec succès", Toast.LENGTH_SHORT).show()
            pendingMemoryId = null
            viewModel.resetState()
            finish()
        } else {
            Toast.makeText(this, "Erreur lors de l'écriture NFC", Toast.LENGTH_SHORT).show()
        }
    }
}
