package com.aboimov.poltava_ar.features.detector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.view.ViewGroup

class OverlayView(context: Context) : View(context) {
    private var results: List<DetectionResult> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
    }

    init {
        setWillNotDraw(false)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    fun setResults(newResults: List<DetectionResult>) {
        results = newResults
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d("YOLO_DEBUG", "ON_DRAW -> viewSize=${width}x${height}, boxes=${results.size}")

        val cornerRadius = 32f

        for (box in results) {
            val left = box.left * width
            val top = box.top * height
            val rawBottom = box.bottom * height

            // Подтягиваем нижнюю границу вверх на 18% от высоты рамки,
            // чтобы убрать пустой запас под постамент и компенсировать вытягивание экрана
            val boxHeight = rawBottom - top
            val bottom = (rawBottom - boxHeight * 0.18f).coerceAtLeast(top + 120f)
            val right = (box.right * width).coerceAtLeast(left + 120f)

            canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, boxPaint)
        }
    }
}
