package com.project.tugasakhir.Cart

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityBottomSheetPesanBinding

class BottomSheetPesanActivity : BottomSheetDialogFragment() {
    private var _binding: ActivityBottomSheetPesanBinding? = null
    private val binding get() = _binding!!

    interface BottomSheetListener {
        fun onConfirmOrder(itemCount: Int, totalPrice: Double)
        fun onCancelOrder()
    }

    private var listener: BottomSheetListener? = null

    companion object {
        private const val ARG_ITEM_COUNT = "arg_item_count"
        private const val ARG_TOTAL_PRICE = "arg_total_price"

        fun newInstance(itemCount: Int, totalPrice: Double): BottomSheetPesanActivity {
            val fragment = BottomSheetPesanActivity()
            val args = Bundle()
            args.putInt(ARG_ITEM_COUNT, itemCount)
            args.putDouble(ARG_TOTAL_PRICE, totalPrice)
            fragment.arguments = args
            return fragment
        }
    }

    fun setBottomSheetListener(listener: BottomSheetListener) {
        this.listener = listener
    }

    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityBottomSheetPesanBinding.inflate(inflater, container, false)

        val itemCount = arguments?.getInt(ARG_ITEM_COUNT) ?: 0
        val totalPrice = arguments?.getDouble(ARG_TOTAL_PRICE) ?: 0.0
        updateDisplay(itemCount, totalPrice)

        // Set warna tombol dan teks
        binding.confirmButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.btn_color))
        binding.cancelButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

        binding.confirmButton.setOnClickListener {
            if (itemCount <= 0) {
                Toast.makeText(requireContext(), "Tidak ada item untuk diproses", Toast.LENGTH_SHORT).show()
            } else {
                listener?.onConfirmOrder(itemCount, totalPrice)
                dismiss()
            }
        }

        binding.cancelButton.setOnClickListener {
            listener?.onCancelOrder()
            dismiss()
        }

        return binding.root
    }

    private fun updateDisplay(itemCount: Int, totalPrice: Double) {
        binding.itemCount.text = "Item: $itemCount"
        binding.HargaBarang.text = "Rp ${String.format("%,.0f", totalPrice)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}