package com.memorytag.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.memorytag.app.databinding.ItemThumbnailBinding

/**
 * Adapter pour la galerie horizontale de miniatures.
 * La photo sélectionnée est mise en avant avec :
 *   - scale 1.0 + opacité 100% + bordure blanche
 *   - les autres : scale 0.85 + opacité 45%
 * Transition animée entre chaque sélection.
 */
class ThumbnailAdapter(
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbViewHolder>() {

    private val photos = mutableListOf<String>()
    private var selectedPosition = 0

    fun submitPhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun setSelected(position: Int) {
        val prev = selectedPosition
        selectedPosition = position
        notifyItemChanged(prev)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemThumbnailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        holder.bind(photos[position], position == selectedPosition)
        holder.itemView.setOnClickListener { onPhotoClick(position) }
    }

    override fun getItemCount() = photos.size

    inner class ThumbViewHolder(
        private val binding: ItemThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(url: String, isSelected: Boolean) {
            Glide.with(binding.root)
                .load(url)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(binding.thumbImage)

            // Animation fluide entre états sélectionné / non-sélectionné
            val targetScale  = if (isSelected) 1.0f  else 0.82f
            val targetAlpha  = if (isSelected) 1.0f  else 0.4f

            binding.root.animate()
                .scaleX(targetScale).scaleY(targetScale)
                .alpha(targetAlpha)
                .setDuration(220)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            // Bordure blanche uniquement sur la sélection
            binding.selectionBorder.alpha = if (isSelected) 1f else 0f
        }
    }
}
