package com.example.chowcheck.frag // Or your actual package

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chowcheck.R // Make sure R is imported


class NavigationFragment : Fragment() {
    // Variable to hold the argument's value
    private var receivedParam1: String? = null
    // Add other params if needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments using the keys from the companion object
        arguments?.let {
            receivedParam1 = it.getString(ARG_PARAM1) // Use the defined constant
            // param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // You can now use the value in 'receivedParam1'
        // e.g., val textView: TextView = view.findViewById(R.id.myTextView)
        // textView.text = receivedParam1
    }

    companion object {
        // *** Define the argument keys here ***
        private const val ARG_PARAM1 = "unique_key_for_param1" // Use descriptive keys
        // private const val ARG_PARAM2 = "unique_key_for_param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * //@param param2 Parameter 2.
         * @return A new instance of fragment NavigationFragment.
         */
        @JvmStatic
        fun newInstance(param1: String/*, param2: String*/) = // Add params needed
            NavigationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1) // Use the defined constant
                    // putString(ARG_PARAM2, param2)
                }
            }
    }
}