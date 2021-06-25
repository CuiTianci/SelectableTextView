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
                    binding.tvSelectionCount.text = "${sequence?.length ?: 0}/${binding.tv.length()}"
                    binding.tvSelection.text = sequence
                }
            })
        }
        binding.tvSelectionCount.text = "0/${binding.tv.length()}"
    }
}