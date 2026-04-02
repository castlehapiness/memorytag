package com.memorytag.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.memorytag.app.databinding.ItemPhotoPagerBinding

/**
 * Adapter pour le ViewPager2 (grande photo principale).
 * Chaque page = 1 photo plein écran.
 * Glide charge avec crossfade 400ms pour la transition douce.
 */
class PhotoPagerAdapter : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    private val photos = mutableListOf<String>()

    fun submitPhotos(newPhotos: List<String>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoPagerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) =
        holder.bind(photos[position])

    override fun getItemCount() = photos.size

    inner class PhotoViewHolder(
        private val binding: ItemPhotoPagerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(url: String) {
            Glide.with(binding.root)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade(400))
                .centerCrop()
                .into(binding.photoImage)
        }
    }
}
