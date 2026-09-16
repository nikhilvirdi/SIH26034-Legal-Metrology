package com.legalmetrology.inspector.data.api

import com.legalmetrology.inspector.domain.model.DeclarationField
import com.legalmetrology.inspector.domain.model.FieldVerdict
import com.legalmetrology.inspector.domain.model.VerdictStatus
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MockInspectionApiService — simulates the backend pipeline for demo purposes.
 *
 * TODO — BACKEND INTEGRATION:
 * Replace this class with a real Retrofit interface:
 *
 * ```kotlin
 * interface InspectionApiService {
 *     @Multipart
 *     @POST("inspections")
 *     suspend fun submitInspection(
 *         @Header("Authorization") token: String,
 *         @Part photos: List<MultipartBody.Part>,
 *         @Part("metadata") metadata: RequestBody
 *     ): InspectionReportResponse
 *
 *     @POST("auth/login")
 *     suspend fun login(@Body request: LoginRequest): LoginResponse
 * }
 * ```
 *
 * The Retrofit instance should be configured in NetworkModule.kt with:
 *   - baseUrl = BuildConfig.BASE_URL
 *   - OkHttp logging interceptor (debug only)
 *   - Auth header interceptor (reads token from DataStore)
 *   - Timeout: 60s for inspection submission (backend ML inference is slow)
 *
 * SIMULATED BEHAVIOR:
 * This mock generates realistic-looking compliance reports with a mix of
 * PASS/FAIL/INCONCLUSIVE verdicts to demonstrate the full UI flow.
 */
@Singleton
class MockInspectionApiService @Inject constructor() {

    /**
     * Simulates submitting photos + AR metadata to the backend.
     * Returns a realistic compliance report after a simulated processing delay.
     */
    suspend fun submitInspection(
        request: InspectionSubmitRequest,
        // TODO: add photoFiles: List<File> when wiring real Retrofit
    ): InspectionReportResponse {
        // Simulate backend ML inference time (YOLOv8 + OCR pipeline)
        delay(2500L)

        val mmPerPixel = request.arScaleMetadata?.mmPerPixel ?: 0.1
        val labelArea = request.arScaleMetadata?.labelAreaCm2 ?: 320.0

        // Generate mock verdicts — in real backend, these come from
        // YOLO detection + PaddleOCR + rule engine
        val verdicts = generateMockVerdicts(request.commodityCategory, mmPerPixel, labelArea)

        return InspectionReportResponse(
            inspectionId = UUID.randomUUID().toString(),
            productName = "Lays Classic Salted",  // Mock: from OCR generic name
            manufacturerName = "PepsiCo India Holdings Pvt. Ltd.",
            manufacturerAddress = "Plot No. 1, Phase IV, Udyog Vihar, Gurugram, Haryana - 122015",
            labelAreaCm2 = labelArea,
            fieldVerdicts = verdicts,
            registryCheckStatus = "VERIFIED",  // Mock: registry check always passes
            registryNotes = "Manufacturer verified in Legal Metrology PCR registry.",
            processingTimeMs = 2467L
        )
    }

    /**
     * Simulates login — in real implementation, POSTs to /auth/login.
     *
     * TODO — BACKEND INTEGRATION:
     * Replace with actual Retrofit call. Tokens should be stored in
     * DataStore (not SharedPreferences) and refreshed using a refresh token.
     *
     * Demo credentials:
     *   officer@lm.gov.in / password123 → OFFICER role
     *   controller@lm.gov.in / password123 → CONTROLLER role
     *   director@lm.gov.in / password123 → DIRECTOR role
     */
    suspend fun login(request: LoginRequest): LoginResponse {
        delay(800L) // Simulate network latency

        return when (request.email.lowercase()) {
            "officer@lm.gov.in" -> LoginResponse(
                token = "mock_officer_token_${System.currentTimeMillis()}",
                userId = "officer_001",
                name = "Rajesh Kumar",
                role = "OFFICER",
                district = "South Delhi",
                state = "Delhi"
            )
            "controller@lm.gov.in" -> LoginResponse(
                token = "mock_controller_token_${System.currentTimeMillis()}",
                userId = "controller_001",
                name = "Priya Sharma",
                role = "CONTROLLER",
                district = "Delhi",
                state = "Delhi"
            )
            "director@lm.gov.in" -> LoginResponse(
                token = "mock_director_token_${System.currentTimeMillis()}",
                userId = "director_001",
                name = "Dr. Amit Verma",
                role = "DIRECTOR",
                district = null,
                state = null
            )
            else -> throw Exception("Invalid credentials")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Mock data generation helpers
    // ─────────────────────────────────────────────────────────────

    private fun generateMockVerdicts(
        category: String,
        mmPerPixel: Double,
        labelAreaCm2: Double
    ): List<FieldVerdictDto> {
        // Simulate a realistic label area tier (100–500 cm²) → requires ≥2.5mm
        val requiredMm = when {
            labelAreaCm2 < 50.0  -> 1.0
            labelAreaCm2 < 100.0 -> 1.5
            labelAreaCm2 < 500.0 -> 2.5
            labelAreaCm2 < 2500.0 -> 4.0
            else -> 6.0
        }

        // Simulate: MRP numeral height was measured at 1.8mm — BELOW 2.5mm threshold
        val mrpPixelHeight = 15.0
        val mrpMeasuredMm = mrpPixelHeight * mmPerPixel

        return listOf(
            FieldVerdictDto(
                field = "MRP",
                status = "FAIL",
                extractedValue = "₹42.00 (incl. all taxes)",
                measuredHeightMm = mrpMeasuredMm,
                requiredHeightMm = requiredMm,
                ruleReference = "Rule 7, Table (100–500 cm²) — G.S.R. 629(E), 2017",
                confidence = 0.94f,
                notes = "Numeral height ${String.format("%.1f", mrpMeasuredMm)}mm is below the required ${requiredMm}mm for this label area."
            ),
            FieldVerdictDto(
                field = "NET_QUANTITY",
                status = "PASS",
                extractedValue = "26g",
                measuredHeightMm = 3.2,
                requiredHeightMm = requiredMm,
                ruleReference = "Rule 6(1)(c), Rule 7",
                confidence = 0.97f,
                notes = null
            ),
            FieldVerdictDto(
                field = "MANUFACTURER_DETAILS",
                status = "PASS",
                extractedValue = "PepsiCo India Holdings Pvt. Ltd., Gurugram",
                measuredHeightMm = null,
                requiredHeightMm = null,
                ruleReference = "Rule 6(1)(a)",
                confidence = 0.89f,
                notes = null
            ),
            FieldVerdictDto(
                field = "GENERIC_NAME",
                status = "PASS",
                extractedValue = "Potato Chips",
                measuredHeightMm = null,
                requiredHeightMm = null,
                ruleReference = "Rule 6(1)(b)",
                confidence = 0.95f,
                notes = null
            ),
            FieldVerdictDto(
                field = "MANUFACTURING_DATE",
                status = "PASS",
                extractedValue = "JUL 2026",
                measuredHeightMm = null,
                requiredHeightMm = null,
                ruleReference = "Rule 6(1)(d)",
                confidence = 0.88f,
                notes = null
            ),
            FieldVerdictDto(
                field = "CONSUMER_CARE",
                status = "INCONCLUSIVE",
                extractedValue = null,
                measuredHeightMm = null,
                requiredHeightMm = null,
                ruleReference = "Rule 6(1)(f)",
                confidence = 0.41f,
                notes = "Consumer care details not detected with sufficient confidence. Officer should verify on physical label."
            ),
            FieldVerdictDto(
                field = "FSSAI_LICENSE",
                status = if (category == "FOOD") "PASS" else "NOT_APPLICABLE",
                extractedValue = if (category == "FOOD") "FSSAI Lic. No. 10013022004641" else null,
                measuredHeightMm = null,
                requiredHeightMm = null,
                ruleReference = "Food Safety and Standards Act, 2006",
                confidence = 0.92f,
                notes = null
            )
        )
    }
}
