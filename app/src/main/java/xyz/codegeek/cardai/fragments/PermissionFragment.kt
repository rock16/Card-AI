package xyz.codegeek.cardai.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation

import xyz.codegeek.cardai.R

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE)

/**
 * A simple [Fragment] subclass.
 */
class PermissionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(this.toString(), "In onCreate _***********************************************")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(!hasPermissions(requireContext())) {
            // Request camera related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            // if permissions have already been granted, proceed
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PermissionFragmentDirections.actionPermissionFragmentToProcessFragment2()
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()){
                // Take the user to the success fragment when permission is granted
                Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionFragmentDirections.actionPermissionFragmentToProcessFragment2()
                )
            }else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}
