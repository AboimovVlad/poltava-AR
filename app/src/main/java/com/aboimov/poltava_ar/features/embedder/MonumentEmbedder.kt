package com.aboimov.poltava_ar.features.embedder

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MonumentEmbedder(
    context: Context,
    modelPath: String = "efficientnet_lite0_android.tflite",
    private val inputSize: Int = 224
) {
    private val interpreter: Interpreter

    init {
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(loadModelFile(context, modelPath), options)
    }

    fun getEmbedding(croppedBitmap: Bitmap): FloatArray {
        val resized = croppedBitmap.scale(inputSize, inputSize)
        val inputBuffer = convertBitmapToBuffer(resized)

        val output = Array(1) { FloatArray(1280) }
        interpreter.run(inputBuffer, output)
        return output[0]
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF).toFloat()) // R
            buffer.putFloat(((pixel shr 8) and 0xFF).toFloat())  // G
            buffer.putFloat((pixel and 0xFF).toFloat())          // B
        }

        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val fd = context.assets.openFd(fileName)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    fun close() {
        interpreter.close()
    }
}