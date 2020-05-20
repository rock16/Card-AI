package xyz.codegeek.cardai.util

import android.util.Log
import androidx.camera.core.AspectRatio
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object VisionImage {

    private const val RATIO_4_3_VALUE = 4.0 / 3.0
    private const val RATIO_16_9_VALUE = 16.0 / 9.0
    const val TAG = "CameraXBasic"

    fun degreesToFirbaseRotation(degrees: Int ): Int = when(degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180 or 270.")
    }

    fun processImage(){

    }

    fun aspectRatio(widthPixels: Int, heightPixels: Int): Int {
        val previewRatio = max(widthPixels, heightPixels).toDouble() / min(widthPixels, heightPixels)
        if (abs(previewRatio - RATIO_4_3_VALUE) >= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    fun processResult(result: FirebaseVisionText): String? {
        val text = result.text
        val pattern = "((\\d{5}(\\s|-)){2}\\d{5})|((\\d{4}(\\s|-)){3}\\d{5})".toRegex()
        return pattern.find(text)?.value
    }

}