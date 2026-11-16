package com.aboimov.poltava_ar.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "MonumentEmbeddings",
    foreignKeys = [
        ForeignKey(
            entity = MonumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["monument_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MonumentEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int?,

    @ColumnInfo(name = "monument_id")
    val monumentId: Int,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,

    @ColumnInfo(name = "latitude")
    val latitude: Double?,

    @ColumnInfo(name = "longitude")
    val longitude: Double?
)
