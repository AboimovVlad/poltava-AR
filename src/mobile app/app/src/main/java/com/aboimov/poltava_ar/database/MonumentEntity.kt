package com.aboimov.poltava_ar.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monuments")
data class MonumentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int?,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "construction_year")
    val constructionYear: String,

    @ColumnInfo(name = "lat")
    val lat: Double?,

    @ColumnInfo(name = "lon")
    val lon: Double?,

    @ColumnInfo(name = "audio_path")
    val audioPath: String,

    @ColumnInfo(name = "youtube_url")
    val youtubeUrl: String
)
