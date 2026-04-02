package com.memorytag.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.memorytag.app.databinding.ItemPhotoThumbnailBinding

/**
 * Adapter pour la galerie horizontale de miniatures.
 *
 * Fonctionnalités :
 * - Affiche des photos arrondies avec Glide
 * - Met en surbrillance la photo sélectionnée
 * - Callback onPhotoClick pour changer la photo principale
 */
class PhotoGalleryAdapter(
    private val onPhotoClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder>() {

    private val photos = mutableListOf<String>()
    private var selectedPosition = 0

    fun submitPhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun getPhotoAt(position: Int): String = photos[position]

    fun setSelectedPosition(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoThumbnailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position == selectedPosition)
        holder.itemView.setOnClickListener { onPhotoClick(position) }
    }

    override fun getItemCount() = photos.size

    // ─── ViewHolder ──────────────────────────────────────────────────────────

    inner class PhotoViewHolder(
        private val binding: ItemPhotoThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String, isSelected: Boolean) {
            // Glide : charge avec crop centré + coins arrondis 12dp
            Glide.with(binding.root.context)
                .load(photoUrl)
                .transform(CenterCrop(), RoundedCorners(24))
                .into(binding.thumbnailImage)

            // Indicateur de sélection : opacité et bordure
            binding.root.alpha = if (isSelected) 1.0f else 0.5f
            binding.selectionIndicator.visibility =
                if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
