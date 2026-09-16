package com.legalmetrology.inspector

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt entry point.
 * Declared in AndroidManifest.xml as android:name=".LegalMetrologyApp"
 */
@HiltAndroidApp
class LegalMetrologyApp : Application()
