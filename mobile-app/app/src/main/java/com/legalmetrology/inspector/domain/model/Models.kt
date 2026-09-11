package com.legalmetrology.inspector.domain.model

// ============================================================
// DOMAIN MODELS — Legal Metrology Inspector App
// These are pure Kotlin data classes, decoupled from Room/API.
// Room entities and API DTOs map to/from these.
// ============================================================

// --- Package classification ---

enum class PackageType(val displayName: String) {
    RETAIL("Retail"),
    WHOLESALE("Wholesale"),
    COMBINATION("Combination / Group / Multi-piece")
}

enum class CommodityCategory(val displayName: String) {
    FOOD("Food (FSSAI)"),
    COSMETIC("Cosmetic (Drugs & Cosmetics Rules)"),
    ALCOHOL("Alcohol (State Excise)"),
    SEED("Seed"),
    MEDICAL_DEVICE("Medical Device"),
    GENERAL("General")
}

// --- AR measurement metadata ---

/**
 * Spatial metadata captured alongside a photo frame.
 * Derived from ARCore intrinsics and hit-test distance.
 *
 * Physical height (mm) = pixelHeight × distanceMeters × 1000 / focalLengthY
 *
 * If ARCore is unavailable, the coin-fallback path populates mmPerPixel
 * via the known physical diameter of a standard 1-rupee coin (22mm).
 */
data class ArScaleMetadata(
    val distanceMeters: Double,          // Camera-to-surface distance (D)
    val focalLengthX: Float,             // fx from ARCore ImageIntrinsics
    val focalLengthY: Float,             // fy from ARCore ImageIntrinsics
    val mmPerPixel: Double,              // Derived conversion factor
    val labelAreaCm2: Double = 0.0,      // Computed from AR plane bounds
    val source: ScaleSource = ScaleSource.ARCORE
)

enum class ScaleSource {
    ARCORE,             // Full ARCore plane detection + depth
    COIN_FALLBACK,      // User placed a 1-rupee coin (22mm diameter known)
    MANUAL_ENTRY        // Officer manually typed the scale (last resort)
}

// --- Field-level compliance ---

/**
 * All mandatory declaration fields on a retail package.
 * Wholesale requires only: MANUFACTURER_DETAILS, NET_QUANTITY, MRP.
 * Medical devices skip font-size measurement (since 2025).
 */
enum class DeclarationField(val displayName: String, val ruleReference: String) {
    MANUFACTURER_DETAILS("Manufacturer / Packer / Importer", "Rule 6(1)(a)"),
    GENERIC_NAME("Generic / Common Name", "Rule 6(1)(b)"),
    NET_QUANTITY("Net Quantity", "Rule 6(1)(c)"),
    MANUFACTURING_DATE("Month & Year of Manufacture / Packing", "Rule 6(1)(d)"),
    MRP("MRP incl. all taxes", "Rule 6(1)(e)"),
    CONSUMER_CARE("Consumer Care Details", "Rule 6(1)(f)"),
    COUNTRY_OF_ORIGIN("Country of Origin (imported goods)", "Rule 6(1)(g)"),
    BARCODE("Barcode / QR Code", "Rule 2A (2022 amendment)"),
    FSSAI_LICENSE("FSSAI License Number", "Food Safety and Standards Act")
}

enum class VerdictStatus {
    PASS,           // Field present, format correct, measurement meets threshold
    FAIL,           // Confirmed violation with rule citation
    INCONCLUSIVE,   // System cannot establish with confidence — needs human review
    NOT_APPLICABLE, // Field not required for this package type / category
    PENDING         // Flagged for human review (e.g., duplicate MRP — needs Controller)
}

data class FieldVerdict(
    val field: DeclarationField,
    val status: VerdictStatus,
    val extractedValue: String?,           // Raw text read by OCR
    val measuredHeightMm: Double?,         // Null if measurement not applicable
    val requiredHeightMm: Double?,         // From font-size table, null if N/A
    val ruleReference: String,             // Specific rule citation for FAIL
    val confidence: Float,                 // OCR/YOLO extraction confidence (0..1)
    val notes: String? = null              // Human-readable explanation
)

// --- Font-size rule table (Rule 7 as amended 2017) ---

/**
 * Minimum numeral heights based on label area.
 * Source: Section 7, Legal Metrology (Packaged Commodities) Rules, 2011
 * as amended by G.S.R. 629(E), 2017.
 */
enum class LabelAreaTier(
    val minNumeralHeightMm: Double,
    val minNumeralHeightMoldedMm: Double,
    val description: String
) {
    UNDER_50(
        minNumeralHeightMm = 1.0,
        minNumeralHeightMoldedMm = 1.5,
        description = "Label area < 50 cm²"
    ),
    FROM_50_TO_100(
        minNumeralHeightMm = 1.5,
        minNumeralHeightMoldedMm = 3.0,
        description = "50 cm² ≤ Label area < 100 cm²"
    ),
    FROM_100_TO_500(
        minNumeralHeightMm = 2.5,
        minNumeralHeightMoldedMm = 4.0,
        description = "100 cm² ≤ Label area < 500 cm²"
    ),
    FROM_500_TO_2500(
        minNumeralHeightMm = 4.0,
        minNumeralHeightMoldedMm = 6.0,
        description = "500 cm² ≤ Label area < 2500 cm²"
    ),
    OVER_2500(
        minNumeralHeightMm = 6.0,
        minNumeralHeightMoldedMm = 6.0,
        description = "Label area ≥ 2500 cm²"
    );

    companion object {
        fun forArea(labelAreaCm2: Double): LabelAreaTier = when {
            labelAreaCm2 < 50.0   -> UNDER_50
            labelAreaCm2 < 100.0  -> FROM_50_TO_100
            labelAreaCm2 < 500.0  -> FROM_100_TO_500
            labelAreaCm2 < 2500.0 -> FROM_500_TO_2500
            else                   -> OVER_2500
        }
    }
}

// --- Inspection (top-level entity) ---

enum class InspectionStatus {
    DRAFT,          // Photos captured, not yet submitted
    SUBMITTED,      // Sent to backend pipeline
    COMPLETED,      // Report received
    FLAGGED,        // Has violations
    APPEALED,       // Business has appealed
    UPHELD,         // Violation upheld after appeal
    DISMISSED,      // Violation dismissed
    COMPOUNDED      // Penalty paid / compounded
}

data class Inspection(
    val id: String,
    val timestamp: Long,
    val packageType: PackageType,
    val commodityCategory: CommodityCategory,
    val commodityName: String?,             // e.g., "Biscuits" (auto-suggested)
    val productBarcode: String?,            // From ML Kit, used as history key
    val productName: String?,               // From OCR / generic name field
    val manufacturerName: String?,
    val manufacturerAddress: String?,
    val labelAreaCm2: Double?,
    val scaleMetadata: ArScaleMetadata?,
    val photoUris: List<String>,            // Local file URIs of captured photos
    val fieldVerdicts: List<FieldVerdict>,
    val overallStatus: InspectionStatus,
    val officerId: String,
    val notes: String? = null,
    val reportPdfUri: String? = null,       // Local URI after PDF export
    val isOnlineListingInspection: Boolean = false  // E-commerce path
)

// --- User / Auth ---

enum class UserRole {
    OFFICER,        // Can create inspections, view own history
    CONTROLLER,     // Can view district-level inspections, update violation status
    DIRECTOR        // Full national dashboard access
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val district: String?,
    val state: String?
)

// --- Standard pack sizes (Schedule II items — Rule 24) ---
// These commodities can only be sold in specific sizes.
// A wrong pack size is a violation by itself.

data class StandardPackSize(
    val commodityName: String,
    val allowedSizesGrams: List<Int>? = null,
    val allowedSizesMl: List<Int>? = null
)

val STANDARD_PACK_SIZES = listOf(
    StandardPackSize("Biscuits", allowedSizesGrams = listOf(25, 50, 75, 100, 150, 200, 300, 400, 500, 600, 700, 800, 1000)),
    StandardPackSize("Tea", allowedSizesGrams = listOf(25, 50, 100, 200, 250, 500, 1000)),
    StandardPackSize("Coffee", allowedSizesGrams = listOf(25, 50, 100, 200, 250, 500, 1000)),
    StandardPackSize("Edible Oil", allowedSizesMl = listOf(200, 500, 1000, 2000, 5000)),
    StandardPackSize("Soft Drinks", allowedSizesMl = listOf(200, 300, 500, 600, 750, 1000, 1250, 1500, 2000))
)
