package com.vishwajitrajput.musetraceai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceProjectDao {
    @Query("SELECT * FROM trace_projects ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<TraceProjectEntity>>

    @Query("SELECT * FROM trace_projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TraceProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: TraceProjectEntity): Long

    @Query("DELETE FROM trace_projects WHERE id = :id")
    suspend fun deleteById(id: Long)
}
