package com.example.matchmyskills

import android.os.Bundle
import android.widget.ImageButton
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

        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }

        btnClose.setOnClickListener {
            finish()
        }
    }
}
