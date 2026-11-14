package com.aboimov.poltava_ar.features.embedder

import com.aboimov.poltava_ar.database.MonumentDao
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RecognizedMonument(
    val monumentId: Int,
    val name: String,
    val year: String,
    val similarity: Float
)

class MonumentMatcher(
    private val dao: MonumentDao,
    private val similarityThreshold: Float = 0.70f
) {
    private data class CachedEmbedding(
        val monumentId: Int,
        val name: String,
        val year: String,
        val vector: FloatArray
    )

    private var cachedList: List<CachedEmbedding>? = null

    // Завантажуємо вектори з Room у пам'ять (викликається один раз при старті)
    suspend fun preloadEmbeddings() {
        if (cachedList != null) return
        val dbItems = dao.getAllEmbeddings()
        cachedList = dbItems.map { item ->
            CachedEmbedding(
                monumentId = item.monumentId,
                name = item.name,
                year = item.constructionYear,
                vector = item.embedding.toFloatArray()
            )
        }
    }

    // Порівнює вектор із камери з усіма еталонами в базі
    fun findBestMatch(queryVector: FloatArray): RecognizedMonument? {
        val list = cachedList ?: return null
        var bestMatch: RecognizedMonument? = null
        var maxScore = -1f

        for (item in list) {
            val score = dotProduct(queryVector, item.vector)
            if (score > maxScore) {
                maxScore = score
                bestMatch = RecognizedMonument(
                    monumentId = item.monumentId,
                    name = item.name,
                    year = item.year,
                    similarity = score
                )
            }
        }

        return if (bestMatch != null && bestMatch.similarity >= similarityThreshold) {
            bestMatch
        } else {
            null
        }
    }

    private fun dotProduct(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        val size = minOf(a.size, b.size)
        for (i in 0 until size) {
            sum += a[i] * b[i]
        }
        return sum
    }

    private fun ByteArray.toFloatArray(): FloatArray {
        val floatBuffer = ByteBuffer.wrap(this)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
        val array = FloatArray(floatBuffer.remaining())
        floatBuffer.get(array)
        return array
    }
}
