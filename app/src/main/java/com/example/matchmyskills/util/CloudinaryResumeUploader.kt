package com.example.matchmyskills.util

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.matchmyskills.BuildConfig

object CloudinaryResumeUploader {

    fun uploadPdf(
        context: Context,
        pdfUri: Uri,
        onStart: () -> Unit,
        onSuccess: (secureUrl: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val preset = BuildConfig.CLOUDINARY_UNSIGNED_PRESET
        if (preset.isBlank()) {
            onError("Cloudinary preset is not configured")
            return
        }

        val mimeType = context.contentResolver.getType(pdfUri)?.lowercase().orEmpty()
        val uriPath = pdfUri.toString().lowercase()
        val isPdf = mimeType == "application/pdf" || uriPath.endsWith(".pdf")
        if (!isPdf) {
            onError("Only PDF files are allowed")
            return
        }

        onStart()

        MediaManager.get().upload(pdfUri)
            .unsigned(preset)
            .option("resource_type", "raw")
            .option("folder", "matchmyskills/resumes")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) = Unit

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val secureUrl = resultData?.get("secure_url")?.toString().orEmpty()
                    if (secureUrl.isBlank()) {
                        onError("Upload succeeded but URL is missing")
                        return
                    }
                    onSuccess(secureUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Resume upload failed")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) = Unit
            })
            .dispatch(context)
    }
}
