package com.aboimov.poltava_ar.features.detector

import android.util.Log
import kotlin.math.max
import kotlin.math.min

class YoloPostProcessor(
    private val inputSize: Float = 640f,
    private val confidenceThreshold: Float = 0.02f,
    private val iouThreshold: Float = 0.3f
) {
    fun processFloat(output: Array<FloatArray>, numElements: Int): List<DetectionResult> {
        var maxConfInFrame = 0f
        val candidates = mutableListOf<DetectionResult>()

        val dim1 = output.size          // Первое измерение (обычно 5, 8400 или 300)
        val dim2 = output[0].size       // Второе измерение (обычно 8400, 5 или 6)

        if (dim1 == 5 && dim2 > 100) {
            // Вариант А: Стандартный выход YOLO11 [5, 8400]
            for (i in 0 until dim2) {
                val conf = output[4][i]
                if (conf > maxConfInFrame) maxConfInFrame = conf
                if (conf > confidenceThreshold) {
                    addNormalizedBox(candidates, output[0][i], output[1][i], output[2][i], output[3][i], conf)
                }
            }
        } else if (dim1 > 100 && dim2 == 5) {
            // Вариант Б: Транспонированный выход YOLO11 [8400, 5]
            for (i in 0 until dim1) {
                val conf = output[i][4]
                if (conf > maxConfInFrame) maxConfInFrame = conf
                if (conf > confidenceThreshold) {
                    addNormalizedBox(candidates, output[i][0], output[i][1], output[i][2], output[i][3], conf)
                }
            }
        } else if (dim2 == 6) {
            // Вариант В: Экспорт с встроенным NMS [300, 6] -> [x1, y1, x2, y2, conf, classId]
            for (i in 0 until dim1) {
                val conf = output[i][4]
                if (conf > maxConfInFrame) maxConfInFrame = conf
                if (conf > confidenceThreshold) {
                    val scale = if (output[i][2] <= 1.0f && output[i][3] <= 1.0f) 1.0f else inputSize
                    candidates.add(
                        DetectionResult(
                            left = (output[i][0] / scale).coerceIn(0f, 1f),
                            top = (output[i][1] / scale).coerceIn(0f, 1f),
                            right = (output[i][2] / scale).coerceIn(0f, 1f),
                            bottom = (output[i][3] / scale).coerceIn(0f, 1f),
                            confidence = conf
                        )
                    )
                }
            }
            Log.d("YOLO_DEBUG", "NMS Mode [$dim1, $dim2] | MaxConf = $maxConfInFrame | Found = ${candidates.size}")
            return candidates
        }

        Log.d("YOLO_DEBUG", "Shape [$dim1, $dim2] | MaxConf = $maxConfInFrame | Found = ${candidates.size}")
        return applyNMS(candidates)
    }

    private fun addNormalizedBox(
        list: MutableList<DetectionResult>,
        rawX: Float, rawY: Float, rawW: Float, rawH: Float, conf: Float
    ) {
        val scale = if (rawX > 2.0f || rawY > 2.0f || rawW > 2.0f || rawH > 2.0f) inputSize else 1.0f
        val cx = rawX / scale
        val cy = rawY / scale
        val w = rawW / scale
        val h = rawH / scale

        list.add(
            DetectionResult(
                left = (cx - w / 2f).coerceIn(0f, 1f),
                top = (cy - h / 2f).coerceIn(0f, 1f),
                right = (cx + w / 2f).coerceIn(0f, 1f),
                bottom = (cy + h / 2f).coerceIn(0f, 1f),
                confidence = conf
            )
        )
    }

    fun processQuantized(
        output: Array<ByteArray>,
        numElements: Int,
        scale: Float,
        zeroPoint: Int,
        isSignedInt8: Boolean
    ): List<DetectionResult> {
        // Конвертируем ByteArray во FloatArray и вызываем общий метод
        val floatOutput = Array(output.size) { r ->
            FloatArray(output[r].size) { c ->
                val raw = if (isSignedInt8) output[r][c].toInt() else (output[r][c].toInt() and 0xFF)
                (raw - zeroPoint) * scale
            }
        }
        return processFloat(floatOutput, numElements)
    }

    private fun applyNMS(boxes: List<DetectionResult>): List<DetectionResult> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            selected.removeAll { calculateIoU(best, it) > iouThreshold }
        }
        return selected
    }

    private fun calculateIoU(a: DetectionResult, b: DetectionResult): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        return interArea / (areaA + areaB - interArea + 1e-6f)
    }
}
