package com.example.adkiller

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class AdBlockService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null

    private val exactWords = listOf("x", "×", "✕", "✖", "╳", "close", "skip")
    private val containsWords = listOf(
        "close", "skip", "dismiss", "বন্ধ", "close ad", "skip ad", "no thanks", "not now"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
    }

    private fun showBubble() {
        if (bubble != null) return
        val size = (54 * resources.displayMetrics.density).toInt()

        val tv = TextView(this).apply {
            text = "✕"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(R.drawable.bubble_bg)
            alpha = 0.85f
        }

        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 500
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    lp.x = startX + dx
                    lp.y = startY + dy
                    wm.updateViewLayout(tv, lp)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) closeAd()
                }
            }
            true
        }

        wm.addView(tv, lp)
        bubble = tv
    }

    private fun closeAd() {
        val root = rootInActiveWindow
        if (root == null) {
            toast("স্ক্রিন পড়া যাচ্ছে না")
            return
        }

        val byText = ArrayList<AccessibilityNodeInfo>()
        collectByText(root, byText, 0)
        for (n in byText) {
            if (clickUp(n)) {
                toast("বিজ্ঞাপন বন্ধ করা হলো")
                return
            }
        }

        val small = findSmallCornerButton(root)
        if (small != null && clickUp(small)) {
            toast("বিজ্ঞাপন বন্ধ করা হলো")
            return
        }

        performGlobalAction(GLOBAL_ACTION_BACK)
        toast("Close বাটন পাওয়া যায়নি, Back চাপা হলো")
    }

    private fun collectByText(
        node: AccessibilityNodeInfo?,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (node == null || depth > 40) return
        if (node.isVisibleToUser) {
            val t = node.text?.toString()?.trim()?.lowercase() ?: ""
            val d = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val id = node.viewIdResourceName?.lowercase() ?: ""

            val matchExact = t in exactWords || d in exactWords
            val matchContains =
                (t.isNotEmpty() && t.length < 25 && containsWords.any { t.contains(it) }) ||
                (d.isNotEmpty() && d.length < 25 && containsWords.any { d.contains(it) }) ||
                id.contains("close") || id.contains("dismiss") || id.contains("skip")

            if (matchExact || matchContains) out.add(node)
        }
        for (i in 0 until node.childCount) {
            collectByText(node.getChild(i), out, depth + 1)
        }
    }

    private fun findSmallCornerButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val dm = resources.displayMetrics
        val maxSize = (64 * dm.density).toInt()
        val topLimit = dm.heightPixels * 0.3
        var best: AccessibilityNodeInfo? = null
        var bestScore = -1.0

        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40) return
            if (n.isVisibleToUser && n.isClickable) {
                val r = Rect()
                n.getBoundsInScreen(r)
                if (r.width() in 1..maxSize && r.height() in 1..maxSize && r.top < topLimit) {
                    val score = r.centerX().toDouble() - r.centerY() * 0.5
                    if (score > bestScore) {
                        bestScore = score
                        best = n
                    }
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return best
    }

    private fun clickUp(node: AccessibilityNodeInfo?): Boolean {
        var cur = node
        var hops = 0
        while (cur != null && hops < 6) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            cur = cur.parent
            hops++
        }
        return false
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
