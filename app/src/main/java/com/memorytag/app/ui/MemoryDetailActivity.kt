package com.memorytag.app.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.memorytag.app.data.model.Memory
import com.memorytag.app.data.model.MemoryUiState
import com.memorytag.app.databinding.ActivityMemoryDetailBinding
import com.memorytag.app.ui.adapter.PhotoGalleryAdapter
import com.memorytag.app.ui.viewmodel.MemoryViewModel
import kotlinx.coroutines.launch

/**
 * Écran immersif d'affichage d'un souvenir.
 *
 * Contient :
 * - Grande photo principale (plein largeur) avec swipe ViewPager2
 * - Galerie horizontale miniatures (RecyclerView)
 * - Globe custom centré sur la localisation
 * - Nom de ville + titre + description
 *
 * L'animation fade-in global est définie dans le thème (windowEnterTransition).
 */
class MemoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEMORY_ID = "extra_memory_id"
    }

    private lateinit var binding: ActivityMemoryDetailBinding
    private val viewModel: MemoryViewModel by viewModels()
    private lateinit var galleryAdapter: PhotoGalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Plein écran immersif — contenu derrière la status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityMemoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGallery()
        observeViewModel()

        // Charge le souvenir via son ID passé en extra
        val memoryId = intent.getStringExtra(EXTRA_MEMORY_ID) ?: "PARIS_001"
        viewModel.loadMemory(memoryId)

        // Bouton retour
        binding.backButton.setOnClickListener { finish() }
    }

    // ─── Setup RecyclerView galerie ──────────────────────────────────────────

    private fun setupGallery() {
        galleryAdapter = PhotoGalleryAdapter { position ->
            // Tap sur une miniature → change la photo principale
            showMainPhoto(galleryAdapter.getPhotoAt(position))
            galleryAdapter.setSelectedPosition(position)
        }

        binding.galleryRecyclerView.apply {
            adapter = galleryAdapter
            layoutManager = LinearLayoutManager(
                this@MemoryDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            setHasFixedSize(true)
        }
    }

    // ─── Observation du ViewModel ────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MemoryUiState.Loading -> showLoading(true)
                        is MemoryUiState.Success -> {
                            showLoading(false)
                            populateUI(state.memory)
                        }
                        is MemoryUiState.Error -> {
                            showLoading(false)
                            // Affiche l'erreur (simplifié)
                            binding.titleText.text = state.message
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // ─── Peuplement de l'UI ──────────────────────────────────────────────────

    private fun populateUI(memory: Memory) {
        // Textes
        binding.titleText.text = memory.title
        binding.locationText.text = memory.location
        binding.descriptionText.text = memory.description ?: ""
        binding.dateText.text = memory.date ?: ""

        // Photo principale (première de la liste)
        if (memory.photos.isNotEmpty()) {
            showMainPhoto(memory.photos[0])
        }

        // Galerie miniatures
        galleryAdapter.submitPhotos(memory.photos)

        // Globe custom — positionne le point rouge selon lat/long
        binding.globeView.setLocation(memory.latitude, memory.longitude)
        binding.globeLocationLabel.text = memory.location

        // Animation fade-in de tout le contenu
        binding.contentContainer.alpha = 0f
        binding.contentContainer.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(100)
            .start()
    }

    private fun showMainPhoto(url: String) {
        Glide.with(this)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .into(binding.mainPhotoImage)
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingView.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
