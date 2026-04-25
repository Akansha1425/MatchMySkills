package com.example.matchmyskills

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.button.MaterialButton

class ImagePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imageUrl = intent.getStringExtra("image_url")
        val photoView: PhotoView = findViewById(R.id.photo_view)
        val btnClose: MaterialButton = findViewById(R.id.btn_close)

        if (!imageUrl.isNullOrBlank() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(photoView)
        } else {
            photoView.setImageResource(R.drawable.ic_profile)
            Toast.makeText(this, "No valid image available", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }
}
