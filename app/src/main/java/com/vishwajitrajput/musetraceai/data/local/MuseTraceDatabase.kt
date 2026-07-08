package com.vishwajitrajput.musetraceai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TraceProjectEntity::class, GeneratedImageEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class MuseTraceDatabase : RoomDatabase() {
    abstract fun traceProjectDao(): TraceProjectDao
    abstract fun generatedImageDao(): GeneratedImageDao
}
