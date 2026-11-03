package com.mytextwidget.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mytextwidget.app.databinding.BottomSheetHelpBinding

class HelpBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetHelpBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "HelpBottomSheet"
        fun newInstance() = HelpBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGotIt.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
