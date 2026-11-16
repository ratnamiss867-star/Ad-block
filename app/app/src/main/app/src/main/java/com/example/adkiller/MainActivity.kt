package com.example.adkiller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val info = TextView(this).apply {
            text = "১. নিচের বাটনে চাপ দিন\n" +
                    "২. \"Ad Closer\" খুঁজে চালু করুন\n" +
                    "৩. স্ক্রিনে লাল ✕ বাটন আসবে\n" +
                    "৪. বিজ্ঞাপন এলে ওই বাটনে চাপ দিন"
            textSize = 18f
        }

        val btn = Button(this).apply {
            text = "Accessibility সেটিংস খুলুন"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(info)
        layout.addView(btn)
        setContentView(layout)
    }
}
