package com.legalmetrology.inspector.data.api

import com.legalmetrology.inspector.domain.model.CommodityCategory
import com.legalmetrology.inspector.domain.model.DeclarationField
import com.legalmetrology.inspector.domain.model.FieldVerdict
import com.legalmetrology.inspector.domain.model.PackageType
import com.legalmetrology.inspector.domain.model.VerdictStatus
import kotlinx.serialization.Serializable

// ============================================================
// API DTOs — Request/Response shapes for the backend pipeline.
//
// TODO — BACKEND INTEGRATION:
// Replace MockInspectionApiService with a real Retrofit interface
// once the backend is deployed. The DTOs below define the contract
// between this app and the Python backend.
//
// Backend pipeline flow:
//   1. App POSTs photos + AR metadata to /inspections
//   2. YOLOv8 detects declaration field bounding boxes
//   3. PaddleOCR reads text; Qwen2.5-VL handles low-confidence regions
//   4. Measurement checks run on numeral height (using mmPerPixel)
//   5. Rule engine checks all fields against the law
//   6. Backend responds with InspectionReportResponse
// ============================================================

@Serializable
data class ArScaleMetadataDto(
    val distanceMeters: Double,
    val focalLengthX: Float,
    val focalLengthY: Float,
    val mmPerPixel: Double,
    val labelAreaCm2: Double,
    val source: String           // "ARCORE", "COIN_FALLBACK", "MANUAL_ENTRY"
)

@Serializable
data class InspectionSubmitRequest(
    val packageType: String,         // PackageType.name
    val commodityCategory: String,   // CommodityCategory.name
    val commodityName: String?,
    val officerId: String,
    val arScaleMetadata: ArScaleMetadataDto?,
    // TODO: photos sent as multipart/form-data in the Retrofit call,
    // not in this JSON body. The DTO maps only the metadata part.
    val isOnlineListingInspection: Boolean = false
)

@Serializable
data class FieldVerdictDto(
    val field: String,               // DeclarationField.name
    val status: String,              // VerdictStatus.name
    val extractedValue: String?,
    val measuredHeightMm: Double?,
    val requiredHeightMm: Double?,
    val ruleReference: String,
    val confidence: Float,
    val notes: String?
)

@Serializable
data class InspectionReportResponse(
    val inspectionId: String,
    val productName: String?,
    val manufacturerName: String?,
    val manufacturerAddress: String?,
    val labelAreaCm2: Double?,
    val fieldVerdicts: List<FieldVerdictDto>,
    val registryCheckStatus: String,  // "VERIFIED", "NOT_FOUND", "INCONCLUSIVE"
    val registryNotes: String?,
    val processingTimeMs: Long
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val role: String,  // "OFFICER", "CONTROLLER", "DIRECTOR"
    val district: String?,
    val state: String?
)
