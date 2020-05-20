package xyz.codegeek.cardai.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation

import xyz.codegeek.cardai.R
import xyz.codegeek.cardai.databinding.FragmentEntryBinding

/**
 * A simple [Fragment] subclass.
 */
class EntryFragment : Fragment() {
    private lateinit var homeBinding: FragmentEntryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        homeBinding = FragmentEntryBinding.inflate(inflater, container, false)
        return homeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btn = homeBinding.button
        btn.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                EntryFragmentDirections.actionHomeFragmentToPermissionFragment()
            )
        }
    }

}
