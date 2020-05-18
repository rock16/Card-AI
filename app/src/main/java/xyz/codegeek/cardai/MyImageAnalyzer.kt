package xyz.codegeek.cardai

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import org.greenrobot.eventbus.EventBus
import xyz.codegeek.cardai.fragments.ProcessFragment
import xyz.codegeek.cardai.util.VisionImage
import xyz.codegeek.cardai.util.VisionImage.TAG
import java.util.concurrent.TimeUnit

class MyImageAnalyzer: ImageAnalysis.Analyzer {
    private var lastAnalyzedTimestamp = 0L
    private var shouldAnalyze: Boolean = true
    var result = "no result"

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        val currentTimeStamp = System.currentTimeMillis()

        if (currentTimeStamp - lastAnalyzedTimestamp >=
                TimeUnit.MILLISECONDS.toMillis(1000) && shouldAnalyze) {
            val mediaImage = image.image
            val imageRotation = VisionImage.degreesToFirbaseRotation(image.imageInfo.rotationDegrees)
            if (mediaImage != null) {
                val myImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

                detector.processImage(myImage)
                    .addOnSuccessListener { firebaseVisionText ->
                        Log.d(TAG, "Starting")
                        result = VisionImage.processResult(firebaseVisionText).toString()
                        if (result.replace("-", "").replace(" ", "").matches(Regex("\\d{15}"))){
                            EventBus.getDefault().post(result)
                            Log.i(TAG, "Result is $result")
                            shouldAnalyze = false
                        }

                        Log.d(TAG, "Finished")
                    }
                    .addOnFailureListener {
                        Log.i(TAG, "Error detecting text $it.message")
                    }
            }


            lastAnalyzedTimestamp = currentTimeStamp
        }
        image.close()
    }
}