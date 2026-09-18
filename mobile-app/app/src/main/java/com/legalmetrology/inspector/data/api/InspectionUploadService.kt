package com.legalmetrology.inspector.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for uploading inspection images to the FastAPI backend.
 *
 * This handles the HTTP multipart upload with OkHttp, including:
 * - Image file as multipart/form-data
 * - Additional form fields (package_type, category)
 * - Network execution on background thread (Dispatchers.IO)
 *
 * IMPORTANT: Replace <LOCAL_IP> with your development machine's actual Wi-Fi IPv4 address
 * (e.g., 192.168.1.100). You can find this by running `ipconfig` (Windows) or `ifconfig` (Mac/Linux).
 * DO NOT use "localhost" or "127.0.0.1" — on Android, these refer to the phone itself, not your PC.
 */
class InspectionUploadService {

    companion object {
        private const val TAG = "InspectionUpload"
        
        // Base URL for FastAPI backend
        // Note: 127.0.0.1 works if running Android emulator and backend on same machine
        // For physical device, replace with your PC's WiFi IP (e.g., 192.168.1.100)
        private const val BASE_URL = "http://127.0.0.1:8000/api/v1"
        private const val UPLOAD_ENDPOINT = "/inspections/upload"
        
        private const val TIMEOUT_SECONDS = 60L
    }

    private val client: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Upload an inspection image to the backend.
     *
     * @param imageFile The captured image file to upload
     * @param inspectionId Optional inspection ID (auto-generated on backend if null)
     * @param packageType Type of package (default: "retail")
     * @param category Product category (default: "General")
     * @return UploadResult containing the response data or error information
     */
    suspend fun uploadInspectionImage(
        imageFile: File,
        inspectionId: String? = null,
        packageType: String = "retail",
        category: String = "General"
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            if (!imageFile.exists()) {
                return@withContext UploadResult.Failure("Image file does not exist: ${imageFile.path}")
            }

            // Build multipart request body
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            // Add image file
            val imageRequestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            requestBodyBuilder.addFormDataPart(
                "file",
                imageFile.name,
                imageRequestBody
            )

            // Add form fields
            if (inspectionId != null) {
                requestBodyBuilder.addFormDataPart("inspection_id", inspectionId)
            }
            requestBodyBuilder.addFormDataPart("package_type", packageType)
            requestBodyBuilder.addFormDataPart("category", category)

            val requestBody = requestBodyBuilder.build()

            // Build HTTP request
            val request = Request.Builder()
                .url("$BASE_URL$UPLOAD_ENDPOINT")
                .post(requestBody)
                .build()

            Log.d(TAG, "Uploading image: ${imageFile.name} (${imageFile.length() / 1024} KB)")
            Log.d(TAG, "Endpoint: $BASE_URL$UPLOAD_ENDPOINT")

            // Execute request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                Log.d(TAG, "Upload successful: $responseBody")
                
                val json = JSONObject(responseBody)
                UploadResult.Success(
                    inspectionId = json.optString("inspection_id", ""),
                    savedPath = json.optString("saved_path", ""),
                    annotatedImagePath = json.optJSONObject("results")
                        ?.optString("annotated_image", ""),
                    responseJson = responseBody
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Upload failed with status ${response.code}: $errorBody")
                UploadResult.Failure("Upload failed: ${response.code} - $errorBody")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during upload", e)
            UploadResult.Failure("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during upload", e)
            UploadResult.Failure("Unexpected error: ${e.message}")
        }
    }

    /**
     * Represents the result of an upload operation.
     */
    sealed class UploadResult {
        /**
         * Upload succeeded.
         *
         * @param inspectionId The inspection ID returned by the backend
         * @param savedPath The filename of the saved raw image
         * @param annotatedImagePath Optional path to the annotated image with bounding boxes
         * @param responseJson Full JSON response from the server
         */
        data class Success(
            val inspectionId: String,
            val savedPath: String,
            val annotatedImagePath: String?,
            val responseJson: String
        ) : UploadResult()

        /**
         * Upload failed.
         *
         * @param errorMessage Description of what went wrong
         */
        data class Failure(val errorMessage: String) : UploadResult()
    }
}
