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
import androidx.viewpager2.widget.ViewPager2
import com.memorytag.app.data.model.Memory
import com.memorytag.app.data.model.MemoryUiState
import com.memorytag.app.databinding.ActivityMemoryDetailBinding
import com.memorytag.app.ui.adapter.PhotoPagerAdapter
import com.memorytag.app.ui.adapter.ThumbnailAdapter
import com.memorytag.app.ui.viewmodel.MemoryViewModel
import kotlinx.coroutines.launch

class MemoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEMORY_ID = "extra_memory_id"
    }

    private lateinit var binding: ActivityMemoryDetailBinding
    private val viewModel: MemoryViewModel by viewModels()

    private lateinit var photoPagerAdapter: PhotoPagerAdapter
    private lateinit var thumbnailAdapter: ThumbnailAdapter

    // Évite les boucles infinie entre ViewPager ↔ RecyclerView
    private var isSyncingSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = ActivityMemoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupThumbnails()
        observeViewModel()

        val memoryId = intent.getStringExtra(EXTRA_MEMORY_ID) ?: "PARIS_001"
        viewModel.loadMemory(memoryId)

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    // ── ViewPager2 : swipe entre les photos ──────────────────────────────────

    private fun setupViewPager() {
        photoPagerAdapter = PhotoPagerAdapter()
        binding.photoPager.adapter = photoPagerAdapter
        binding.photoPager.offscreenPageLimit = 2

        // Sync ViewPager → Thumbnails
        binding.photoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!isSyncingSelection) {
                    isSyncingSelection = true
                    thumbnailAdapter.setSelected(position)
                    binding.thumbnailRecycler.smoothScrollToPosition(position)
                    isSyncingSelection = false
                }
                // Indicateur de position
                binding.photoIndicator.text = "${position + 1} / ${photoPagerAdapter.itemCount}"
            }
        })
    }

    // ── RecyclerView horizontal thumbnails ───────────────────────────────────

    private fun setupThumbnails() {
        thumbnailAdapter = ThumbnailAdapter { position ->
            if (!isSyncingSelection) {
                isSyncingSelection = true
                binding.photoPager.setCurrentItem(position, true)
                isSyncingSelection = false
            }
        }
        binding.thumbnailRecycler.apply {
            adapter = thumbnailAdapter
            layoutManager = LinearLayoutManager(
                this@MemoryDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            setHasFixedSize(true)
        }
    }

    // ── ViewModel observation ────────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MemoryUiState.Loading -> setLoading(true)
                        is MemoryUiState.Success -> { setLoading(false); populateUI(state.memory) }
                        is MemoryUiState.Error   -> { setLoading(false); binding.titleText.text = state.message }
                        else -> {}
                    }
                }
            }
        }
    }

    // ── Peuplement de l'UI avec animations enchaînées ────────────────────────

    private fun populateUI(memory: Memory) {
        // Photos dans le ViewPager
        photoPagerAdapter.submitPhotos(memory.photos)
        thumbnailAdapter.submitPhotos(memory.photos)

        // Indicateur
        if (memory.photos.size > 1) {
            binding.photoIndicator.visibility = View.VISIBLE
            binding.photoIndicator.text = "1 / ${memory.photos.size}"
        }

        // Textes
        binding.titleText.text = memory.title
        binding.locationText.text = memory.location
        binding.descriptionText.text = memory.description ?: ""
        binding.dateText.text = memory.date ?: ""

        // Globe
        binding.globeView.setLocation(memory.latitude, memory.longitude)
        binding.globeLocationLabel.text = memory.location

        // ── Animation d'entrée en cascade ────────────────────────────────────
        // Le ViewPager apparaît en premier (déjà visible via layout)
        // puis le contenu remonte doucement depuis le bas
        val views = listOf(
            binding.titleText,
            binding.locationRow,
            binding.dateText,
            binding.descriptionText,
            binding.divider,
            binding.globeSection
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200L + index * 80L)
                .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                .start()
        }

        // Globe : scale depuis 0.6 + fade
        binding.globeSection.scaleX = 0.6f
        binding.globeSection.scaleY = 0.6f
        binding.globeSection.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(600)
            .setStartDelay(600)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        // Thumbnails slide depuis la droite
        binding.thumbnailRecycler.translationX = 300f
        binding.thumbnailRecycler.alpha = 0f
        binding.thumbnailRecycler.animate()
            .translationX(0f).alpha(1f)
            .setDuration(500)
            .setStartDelay(150)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .start()
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingView.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
