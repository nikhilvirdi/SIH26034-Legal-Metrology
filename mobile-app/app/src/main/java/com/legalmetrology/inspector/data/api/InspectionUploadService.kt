package com.legalmetrology.inspector.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for uploading inspection images to the FastAPI backend.
 *
 * Uses ADB reverse tunnel: adb reverse tcp:8000 tcp:8000
 * This maps 127.0.0.1:8000 on the Android device to localhost:8000 on the development machine.
 */
class InspectionUploadService {

    companion object {
        private const val TAG = "InspectionUpload"
        
        // Base URL for FastAPI backend via ADB reverse tunnel
        // Run: adb reverse tcp:8000 tcp:8000
        private const val BASE_URL = "http://127.0.0.1:8000/api/v1/"
        
        private const val TIMEOUT_SECONDS = 60L
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Upload an inspection image to the backend.
     *
     * @param imageFile The captured image file to upload
     * @param packageType Type of package (e.g., "retail")
     * @param category Product category (e.g., "General")
     * @return Result sealed class with Success or Error
     */
    suspend fun uploadInspectionImage(
        imageFile: File,
        packageType: String,
        category: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (!imageFile.exists()) {
                return@withContext Result.Error("Image file does not exist: ${imageFile.path}")
            }

            Log.d(TAG, "Uploading: ${imageFile.name} (${imageFile.length() / 1024} KB)")

            // Build multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .addFormDataPart("package_type", packageType)
                .addFormDataPart("category", category)
                .build()

            // Build HTTP request
            val request = Request.Builder()
                .url("${BASE_URL}inspections/upload")
                .post(requestBody)
                .build()

            Log.d(TAG, "POST ${BASE_URL}inspections/upload")

            // Execute request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                Log.d(TAG, "✓ Upload successful")
                Log.d(TAG, "Response: $responseBody")
                Result.Success(responseBody)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "✗ Upload failed: ${response.code}")
                Log.e(TAG, "Error: $errorBody")
                Result.Error("HTTP ${response.code}: $errorBody")
            }
        } catch (e: IOException) {
            Log.e(TAG, "✗ Network error", e)
            Result.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Unexpected error", e)
            Result.Error("Unexpected error: ${e.message}")
        }
    }

    /**
     * Sealed class representing the upload result.
     */
    sealed class Result {
        data class Success(val responseJson: String) : Result()
        data class Error(val message: String) : Result()
    }
}
