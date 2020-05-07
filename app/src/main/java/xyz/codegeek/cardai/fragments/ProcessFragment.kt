package xyz.codegeek.cardai.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.Navigation


import xyz.codegeek.cardai.R
import xyz.codegeek.cardai.databinding.FragmentProcessBinding
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A simple [Fragment] subclass.
 */
class ProcessFragment : Fragment(), LifecycleOwner {

    private var camera: Camera? = null

    private lateinit var processFragmentBinding: FragmentProcessBinding
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var viewFinder: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PermissionFragment.hasPermissions(requireContext())){
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ProcessFragmentDirections.actionProcessFragmentToPermissionFragment()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        processFragmentBinding = FragmentProcessBinding.inflate(inflater, container, false)
        return processFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder = processFragmentBinding.viewFinder

        viewFinder.post {
            startCamera()
        }
        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them
        if (!PermissionFragment.hasPermissions(requireContext())){
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ProcessFragmentDirections.actionProcessFragmentToPermissionFragment()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Shut down our background executor
        executor.shutdown()
    }

    private fun startCamera(){
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        // Create configuration object for the viewfinder use case

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // cameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // preview
            val preview = Preview.Builder()
            // we request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
            // set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
            }catch (exc: Exception) {
                Log.e(TAG, "Use case binding failure")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun aspectRatio(widthPixels: Int, heightPixels: Int): Int {
        val previewRatio = max(widthPixels, heightPixels).toDouble() / min(widthPixels, heightPixels)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun updateTransform() {
        //TODO: Implement camera viewfinder transformations
    }

    companion object {

        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)
        */
    }
}
