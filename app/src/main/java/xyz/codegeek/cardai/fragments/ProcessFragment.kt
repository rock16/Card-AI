package xyz.codegeek.cardai.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.google.android.gms.vision.CameraSource
import kotlinx.android.synthetic.main.camera_control_ui.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import xyz.codegeek.cardai.MyImageAnalyzer


import xyz.codegeek.cardai.R
import xyz.codegeek.cardai.databinding.FragmentProcessBinding
import xyz.codegeek.cardai.util.VisionImage.aspectRatio
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A simple [Fragment] subclass.
 */
class ProcessFragment : Fragment(), LifecycleOwner {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraInfo: CameraInfo
    private var displayId: Int = -1
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var pinTarget: View

    private lateinit var processFragmentBinding: FragmentProcessBinding
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var viewFinder: PreviewView

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@ProcessFragment.displayId){
                Log.d(TAG, "Rotation changed : ${view.display.rotation}")
            }
        } ?: Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        processFragmentBinding = FragmentProcessBinding.inflate(inflater, container, false)
        return processFragmentBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //container = view as ConstraintLayout
        //controlUiBinding = CameraControlUiBinding.bind(container)
        viewFinder = processFragmentBinding.viewFinder
        pinTarget = processFragmentBinding.pinTarget

        // Initialize background executor

        displayManager.registerDisplayListener(displayListener, null)

        viewFinder.post {

            // keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId
            startCamera()
        }
        viewFinder.setOnClickListener {
            Log.d(TAG, "Running autoFocus")
            setAutoFocus()
        }

        processFragmentBinding.cameraFlashSwitch.setOnClickListener{
            togleTorchState()
        }
    }

    private fun togleTorchState() {
        if (cameraInfo.torchState.value == TorchState.ON){
            camera.cameraControl.enableTorch(false)
        } else {
            camera.cameraControl.enableTorch(true)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "In onResume method KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK")
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
        displayManager.unregisterDisplayListener(displayListener)
        Log.i(TAG, "In on Destroy method")
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(pin: String){
        pin_preview.text = pin

        loadPin(pin)
    }

    private fun loadPin(pin: String) {
        if (!PermissionFragment.hasPermissions(requireContext())){
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ProcessFragmentDirections.actionProcessFragmentToPermissionFragment()
            )
        }

        Log.i(TAG, "Loading pin ________________________________")

        try {
            var string = "tel: *127*0#"
            string = string.replace("*", Uri.encode("*")).replace("#", Uri.encode("#"))
            Log.d(TAG, "string is $string")
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.setData(Uri.parse(string))
            startActivity(callIntent)
        } catch (e: java.lang.Exception){
            Log.d(TAG, "Error loading pin", e)
        }
    }

    private fun startCamera(){
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        // Create configuration object for the viewfinder use case

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = viewFinder.display.rotation

        Log.i(TAG, "screen rotation is $rotation")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // cameraProvider
            cameraProvider = cameraProviderFuture.get()

            // preview
            preview = Preview.Builder()
            // we request aspect ratio but no resolution
                //.setTargetResolution(Size(480, 640))
                .setTargetAspectRatio(screenAspectRatio)
            // set initial target rotation
                .setTargetRotation(rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                //.setTargetResolution(Size(480, 640))
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(executor, MyImageAnalyzer())
                }

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                setAutoFocus()
                cameraInfo = camera.cameraInfo
                //viewFinder.preferredImplementationMode = PreviewView.ImplementationMode.TEXTURE_VIEW
                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(cameraInfo))
                setAutoFocus()

                setTorchStateObserver()
            }catch (exc: Exception) {
                Log.e(TAG, "Use case binding failure")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setAutoFocus() {
        Log.i(TAG ,"AutoFocus called ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        val factory = SurfaceOrientedMeteringPointFactory(viewFinder.width.toFloat(), viewFinder.height.toFloat())
        val centerWidth = viewFinder.x + viewFinder.width.toFloat()/2f
        val centerHeight = viewFinder.y + viewFinder.height.toFloat()/2f
        val afpoint = 1.0f/12.0f
        val aepoint = afpoint * 1.5f
        val autoFocusPoint = factory.createPoint(centerWidth, centerHeight, afpoint)
        val autoExposure = factory.createPoint(centerWidth, centerHeight, aepoint)
        try {
            camera.cameraControl.startFocusAndMetering(
                FocusMeteringAction.Builder(
                    autoFocusPoint,
                    FocusMeteringAction.FLAG_AF
                ).addPoint(autoExposure, FocusMeteringAction.FLAG_AE)
                    .disableAutoCancel()
                    .build()
            )
        } catch (e: CameraInfoUnavailableException) {
            Log.d(TAG, "Cannot access camera", e)
        }

    }

    private fun setTorchStateObserver() {
        cameraInfo.torchState.observe(viewLifecycleOwner, Observer { state ->
            if (state == TorchState.ON) {
                processFragmentBinding.cameraFlashSwitch.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_flash_on_black_24dp
                    )
                )
            } else {
                processFragmentBinding.cameraFlashSwitch.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_flash_off_black_24dp
                    )
                )
            }
        })
    }

    companion object {

        private const val TAG = "CameraXBasic"

        /** Helper function used to create a timestamped file
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)
        */
    }
}
