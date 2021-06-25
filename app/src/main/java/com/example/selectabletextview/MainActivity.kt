package com.example.selectabletextview

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ctc.selectabletextview.SelectableTextView
import com.example.selectabletextview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tv.apply {
            setOnClickListener {
                unselect()
            }
            setOnLongClickListener {
                selectAll()
                true
            }
            setOnSelectionChangListener(object : SelectableTextView.OnSelectionChangListener {
                override fun onSelectionChanged(sequence: CharSequence?, isAllSelected: Boolean) {
                    binding.tvAllSelected.text =
                        if (isAllSelected) "AllSelected" else "PartSelected"
                    binding.tvSelection.text = sequence
                }
            })
        }
    }

    private fun String?.loge() {
        Log.e("cui", this ?: "null")
    }
}