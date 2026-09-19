# Android Image Upload Integration Guide

This guide shows how to integrate the `InspectionUploadService` into your existing CameraX capture flow.

## Prerequisites

1. **Network Configuration**: 
   - Open `InspectionUploadService.kt`
   - Replace `<LOCAL_IP>` with your PC's actual WiFi IP address
   - Find your IP:
     - **Windows**: `ipconfig` → Look for "IPv4 Address" under WiFi adapter
     - **Mac/Linux**: `ifconfig` or `ip addr` → Look for inet address
   - Example: `192.168.1.100`
   - **IMPORTANT**: Both phone and PC must be on the same WiFi network

2. **Backend Running**: Ensure FastAPI server is running on `http://<YOUR_IP>:8000`

## Integration Code

### Option 1: Upload Immediately After Capture (Recommended)

Add this to your `ScanScreen.kt` in the `onImageSaved` callback:

```kotlin
import com.legalmetrology.inspector.data.api.InspectionUploadService
import kotlinx.coroutines.launch

// At the top of ScanScreen composable, create the upload service
val uploadService = remember { InspectionUploadService() }

// Inside capturePhoto() function, modify the onImageSaved callback:
override fun onImageSaved(output: ImageCapture.OutputFileResults) {
    Log.d(
        TAG,
        "Captured photo ${photosCaptures + 1}/$maxPhotos " +
            "— ${photoFile.name}, " +
            "mmPerPx=${lockedState.mmPerPixel}"
    )
    
    // Upload the captured image immediately
    scope.launch {
        val uploadResult = uploadService.uploadInspectionImage(
            imageFile = photoFile,
            packageType = "retail",
            category = "General"
        )
        
        when (uploadResult) {
            is InspectionUploadService.UploadResult.Success -> {
                Log.d(TAG, "Upload successful: ${uploadResult.inspectionId}")
                Log.d(TAG, "Annotated image: ${uploadResult.annotatedImagePath}")
                // TODO: Store inspection ID or show success UI
            }
            is InspectionUploadService.UploadResult.Failure -> {
                Log.e(TAG, "Upload failed: ${uploadResult.errorMessage}")
                // TODO: Show error toast or retry UI
            }
        }
        
        // Continue with existing photo capture logic
        photosCaptures++
        delay(800L)
        if (photosCaptures >= maxPhotos) {
            onProceedToReview(UUID.randomUUID().toString())
        } else {
            val currentResult = latestResult
            scanState = if (currentResult is ArucoResult.Valid) {
                ScanState.Locked(currentResult.mmPerPixel)
            } else {
                ScanState.Searching
            }
        }
    }
}
```

### Option 2: Upload All Photos After Review

If you prefer to upload after the user reviews all captured photos:

```kotlin
// In your review/confirmation screen or after maxPhotos is reached:
fun uploadAllPhotos(photoFiles: List<File>) {
    scope.launch {
        photoFiles.forEachIndexed { index, file ->
            val result = uploadService.uploadInspectionImage(
                imageFile = file,
                inspectionId = "INS-${UUID.randomUUID().toString().take(8).uppercase()}",
                packageType = "retail",
                category = "General"
            )
            
            when (result) {
                is InspectionUploadService.UploadResult.Success -> {
                    Log.d(TAG, "Photo ${index + 1} uploaded: ${result.inspectionId}")
                }
                is InspectionUploadService.UploadResult.Failure -> {
                    Log.e(TAG, "Photo ${index + 1} failed: ${result.errorMessage}")
                }
            }
        }
    }
}
```

## Network Permissions

Ensure your `AndroidManifest.xml` has internet permission (already included in your project):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Response Structure

The backend returns:

```json
{
  "inspection_id": "INS-A1B2C3D4",
  "saved_path": "INS-A1B2C3D4_raw.jpg",
  "results": {
    "annotated_image": "/path/to/annotated_INS-A1B2C3D4_raw.jpg",
    "scale": {
      "mm_per_pixel": 0.0612,
      "marker_detected": true
    },
    "fields": [
      {
        "field": "mrp",
        "bbox": [100, 200, 300, 250],
        "confidence": 0.92,
        "height_mm": 2.3,
        "extracted_text": "Rs. 50"
      }
    ]
  }
}
```

## Error Handling

Common errors and solutions:

1. **Connection Refused**: Check if backend is running and IP is correct
2. **Timeout**: Image might be too large or network is slow
3. **404 Not Found**: Verify the endpoint URL matches your backend
4. **File Not Found**: Ensure the captured file exists before uploading

## Testing

1. Start FastAPI backend: `cd server && uvicorn app.main:app --host 0.0.0.0 --port 8000`
2. Update `InspectionUploadService.kt` with your IP
3. Run Android app on physical device (same WiFi)
4. Capture photo and check logs for upload status
5. Check `server/storage/annotated/` for annotated images with bounding boxes

## Complete Example with UI Feedback

```kotlin
// Add state for upload status
var uploadStatus by remember { mutableStateOf<String?>(null) }
var isUploading by remember { mutableStateOf(false) }

// Inside onImageSaved:
override fun onImageSaved(output: ImageCapture.OutputFileResults) {
    scope.launch {
        isUploading = true
        uploadStatus = "Uploading..."
        
        val result = uploadService.uploadInspectionImage(
            imageFile = photoFile,
            packageType = "retail",
            category = "General"
        )
        
        uploadStatus = when (result) {
            is InspectionUploadService.UploadResult.Success -> 
                "✓ Uploaded: ${result.inspectionId}"
            is InspectionUploadService.UploadResult.Failure -> 
                "✗ Failed: ${result.errorMessage}"
        }
        
        isUploading = false
        
        // Clear status after 3 seconds
        delay(3000)
        uploadStatus = null
    }
}

// Add to your Compose UI to show upload status
uploadStatus?.let { status ->
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = status,
            color = if (isUploading) Color.Yellow else Color.Green,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}
```

## Next Steps

After successful upload:
- Retrieve annotated image from backend to show user
- Store inspection ID in local database
- Implement offline queue for failed uploads
- Add retry logic with exponential backoff
