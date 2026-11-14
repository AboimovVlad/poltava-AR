package com.aboimov.poltava_ar.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

// Легка модель для першої вибірки (без великих текстів опису)
data class EmbeddingItem(
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "monument_id") val monumentId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "construction_year") val constructionYear: String,
    @ColumnInfo(name = "embedding") val embedding: ByteArray
)

@Dao
interface MonumentDao {
    @Query("""
        SELECT 
            me.id, 
            me.monument_id, 
            m.name, 
            m.construction_year,
            me.embedding 
        FROM MonumentEmbeddings me
        INNER JOIN monuments m ON me.monument_id = m.id
    """)
    suspend fun getAllEmbeddings(): List<EmbeddingItem>

    @Query("SELECT * FROM monuments WHERE id = :monumentId LIMIT 1")
    suspend fun getMonumentDetails(monumentId: Int): MonumentEntity?
}
