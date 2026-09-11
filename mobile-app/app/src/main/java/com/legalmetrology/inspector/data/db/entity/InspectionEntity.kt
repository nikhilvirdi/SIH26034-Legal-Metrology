package com.legalmetrology.inspector.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.legalmetrology.inspector.data.db.Converters
import com.legalmetrology.inspector.domain.model.CommodityCategory
import com.legalmetrology.inspector.domain.model.InspectionStatus
import com.legalmetrology.inspector.domain.model.PackageType

/**
 * Room entity for persisting inspections locally.
 * The full inspection (with all field verdicts) is stored as a JSON blob
 * in the fieldVerdictsJson column to avoid over-engineering the schema
 * for a hackathon scope.
 *
 * TODO — PRODUCTION:
 * For a production system, normalize fieldVerdicts into a separate table
 * with a FK to InspectionEntity for proper querying/indexing.
 */
@Entity(tableName = "inspections")
@TypeConverters(Converters::class)
data class InspectionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val packageType: PackageType,
    val commodityCategory: CommodityCategory,
    val commodityName: String?,
    val productBarcode: String?,
    val productName: String?,
    val manufacturerName: String?,
    val labelAreaCm2: Double?,
    val mmPerPixel: Double?,
    val scaleSource: String?,
    val photoUris: String,              // JSON array of local file URIs
    val fieldVerdictsJson: String,      // JSON-serialized list of FieldVerdict
    val overallStatus: InspectionStatus,
    val officerId: String,
    val notes: String?,
    val reportPdfUri: String?,
    val isOnlineListingInspection: Boolean = false
)
