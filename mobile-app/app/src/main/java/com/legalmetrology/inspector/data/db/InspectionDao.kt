package com.legalmetrology.inspector.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.legalmetrology.inspector.data.db.entity.InspectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: InspectionEntity)

    @Update
    suspend fun updateInspection(inspection: InspectionEntity)

    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE officerId = :officerId ORDER BY timestamp DESC")
    fun getInspectionsByOfficer(officerId: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE productBarcode = :barcode ORDER BY timestamp DESC")
    fun getInspectionsByBarcode(barcode: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getInspectionById(id: String): InspectionEntity?

    @Query("SELECT COUNT(*) FROM inspections WHERE productBarcode = :barcode AND overallStatus = 'FLAGGED'")
    suspend fun getViolationCountForProduct(barcode: String): Int

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteInspection(id: String)

    // TODO: Add query for Controller/Director views (by district/state) when
    // backend sync is implemented. Currently all data is local per-officer.
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInspections(limit: Int = 50): Flow<List<InspectionEntity>>
}
