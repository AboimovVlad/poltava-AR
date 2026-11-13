package com.aboimov.poltava_ar.features.detector

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MonumentDetector(
    context: Context,
    modelPath: String = "best.tflite",
    private val inputSize: Int = 640
) {
    private val interpreter: Interpreter
    private val postProcessor = YoloPostProcessor(inputSize.toFloat())

    private val isInputQuantized: Boolean
    private val isInputSignedInt8: Boolean
    private val inputScale: Float
    private val inputZeroPoint: Int
    private val isNCHW: Boolean

    private val isOutputQuantized: Boolean
    private val isOutputSignedInt8: Boolean
    private val outputScale: Float
    private val outputZeroPoint: Int

    private val numChannels: Int
    private val numElements: Int

    init {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(loadModelFile(context, modelPath), options)

        val inputTensor = interpreter.getInputTensor(0)
        val inShape = inputTensor.shape()
        // Проверяем, является ли формат входного тензора NCHW: [1, 3, 640, 640]
        val idxChannelsIn = 1
        isNCHW = inShape.size == 4 && inShape[idxChannelsIn] == 3

        isInputQuantized = inputTensor.dataType() == DataType.UINT8 || inputTensor.dataType() == DataType.INT8
        isInputSignedInt8 = inputTensor.dataType() == DataType.INT8
        inputScale = if (isInputQuantized) inputTensor.quantizationParams().scale else 1f
        inputZeroPoint = if (isInputQuantized) inputTensor.quantizationParams().zeroPoint else 0

        val outputTensor = interpreter.getOutputTensor(0)
        val shape = outputTensor.shape()
        isOutputQuantized = outputTensor.dataType() == DataType.UINT8 || outputTensor.dataType() == DataType.INT8
        isOutputSignedInt8 = outputTensor.dataType() == DataType.INT8

        val idxChannelsOut = 1
        val idxElementsOut = 2
        numChannels = shape[idxChannelsOut]
        numElements = shape[idxElementsOut]

        outputScale = if (isOutputQuantized) outputTensor.quantizationParams().scale else 1f
        outputZeroPoint = if (isOutputQuantized) outputTensor.quantizationParams().zeroPoint else 0

        Log.d("YOLO_DEBUG", "MODEL LOADED: $modelPath | isNCHW=$isNCHW")
        Log.d("YOLO_DEBUG", "INPUT -> type=${inputTensor.dataType()}, shape=${inShape.contentToString()}")
        Log.d("YOLO_DEBUG", "OUTPUT -> type=${outputTensor.dataType()}, shape=${shape.contentToString()}")
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resized = bitmap.scale(inputSize, inputSize)
        val inputBuffer = convertBitmapToBuffer(resized)

        val batchIdx = 0
        return if (isOutputQuantized) {
            val output = Array(1) { Array(numChannels) { ByteArray(numElements) } }
            interpreter.run(inputBuffer, output)
            postProcessor.processQuantized(output[batchIdx], numElements, outputScale, outputZeroPoint, isOutputSignedInt8)
        } else {
            val output = Array(1) { Array(numChannels) { FloatArray(numElements) } }
            interpreter.run(inputBuffer, output)
            postProcessor.processFloat(output[batchIdx], numElements)
        }
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val bytesPerChannel = if (isInputQuantized) 1 else 4
        val buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        if (isNCHW) {
            // Формат LiteRT / PyTorch: сначала весь канал R, потом G, потом B
            for (pixel in pixels) {
                writeChannel(buffer, ((pixel shr 16) and 0xFF) / 255.0f)
            }
            for (pixel in pixels) {
                writeChannel(buffer, ((pixel shr 8) and 0xFF) / 255.0f)
            }
            for (pixel in pixels) {
                writeChannel(buffer, (pixel and 0xFF) / 255.0f)
            }
        } else {
            // Классический формат TFLite: R, G, B подряд для каждого пикселя
            for (pixel in pixels) {
                writeChannel(buffer, ((pixel shr 16) and 0xFF) / 255.0f)
                writeChannel(buffer, ((pixel shr 8) and 0xFF) / 255.0f)
                writeChannel(buffer, (pixel and 0xFF) / 255.0f)
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun writeChannel(buffer: ByteBuffer, value: Float) {
        if (isInputQuantized) {
            val q = if (inputScale > 0f) (value / inputScale + inputZeroPoint).toInt() else (value * 255f - 128f).toInt()
            if (isInputSignedInt8) {
                buffer.put(q.coerceIn(-128, 127).toByte())
            } else {
                buffer.put(q.coerceIn(0, 255).toByte())
            }
        } else {
            buffer.putFloat(value)
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    fun close() {
        interpreter.close()
    }
}
