import java.util.Base64

println("=== DEBUG KEYSTORE DIAGNOSTICS ===")
val diagKeystore = file("${project.rootDir}/debug.keystore")
val diagKeystoreBase64 = file("${project.rootDir}/debug.keystore.base64")
println("diagKeystore path: ${diagKeystore.absolutePath}")
println("diagKeystore exists: ${diagKeystore.exists()}")
println("diagKeystoreBase64 path: ${diagKeystoreBase64.absolutePath}")
println("diagKeystoreBase64 exists: ${diagKeystoreBase64.exists()}")
if (!diagKeystore.exists()) {
  try {
    val decodedBytes = if (diagKeystoreBase64.exists()) {
      val base64Bytes = diagKeystoreBase64.readBytes()
      Base64.getMimeDecoder().decode(base64Bytes)
    } else {
      val fallbackStr = "MIIKZgIBAzCCChAGCSqGSIb3DQEHAaCCCgEEggn9MIIJ+TCCBcAGCSqGSIb3DQEHAaCCBbEEggWtMIIFqTCCBaUGCyqGSIb3DQEMCgECoIIFQDCCBTwwZgYJKoZIhvcNAQUNMFkwOAYJKoZIhvcNAQUMMCsEFGg68ORAXI9mCtm70CuAJmIjXgT0AgInEAIBIDAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQXMwqYFigLS3QQDtwuzKcLQSCBNDx1+F4iM0WLG5ilAkcv9M4kQCLEe5tYziGLJ2+snLrZDTyHIOCCPImH+MCE4l85Qw+fnjVYzTl/O8qvTRy3IgM7LBLDtpBzPCS4NPidkeLVY9+hDnc4w+G6BP5O2X8PabSqMDP+2Pamr2e5SII/Qx2gIECgVulV1ks/VBj67oykDxe7qvk6gfqPH8KHW/8t22pGYZDQaMU7A4pbsBTeQjm9uumTVn8THtfQizPeJklTlMI9yaw5H/kR/J4prLJizaY/oumCc04Z6JbNqUJVTVEFl0/St0LhDCWDjSsp3L82111ruAxGoDoR1I4fRyVy4/3/hu+iAT05wBtVi4ZWdkDx1eLFdbmT8dT+TTtX4/Q/qtAzKjrKWq8dHMLdiibp61XVcUxTGs1dFY3vvZQ39sp926UFlhfHfOF3G4LK8b+zYCm1Jv7HEU73bs5IB86iThtt8ck16lrGqZLSdRg2spY+ycfZ1tYDaYDE2hcKdmZWv4oFKjngK+hlxLXbNrBSf/eXFWJxquxBmptb+4mK/45/gow3JEkZ8exLDYjPgtyEci8HsbpoUd8HZZ9XI68izzC+vm7mqIitE4X94UuqikKHSHgRagXAXjR6fF9Vvil1M0fEwbVjrZ6e5X2tj02huU528it/1GejAJ5pKs93S/F7wfykh3aftsg4BwQm0LN00IC4zrC3YMphwjchyEpLlDsMtWhg3q6UPh50PhKti2wQtRjcUvfmZPkS9ZO85gTkYJ/a/TX9yq9uPnQwyk+r2cvczwdPYiwVdof9xNl88JU/suT4UokProVHCuseuXVOP3TkvLLgHPzCTaFcLGaPxtX6p9oA2zgxP8LEsLz3oQk2i+PjysvNCujFHzwEekScawN1qbpLva5WC3CnEqXMMUTwVpzAg9BQ1B+SywFwx+hteEURLtMpav3XE+z8MOXxspaYiZBtJZ6kp70tB+C+r2rPwukCXBf+ngmLHS9fbdedt4PzoGhdso4b6PtCBhOWg5SKzi+tg4wdtnINMSRhvmbqDz+mgHu1or3bXnCne0pH7zpUpQ2VebJi+2QUAXYqXk2bzgfI4kSrMUPxeX7EpWyF2/aV7cX/jvs6D7ZExFct9/oNEZWPehN8JH3fBRowS7NOPRN9q9sFz3AP9lwuD/JsJaQnz+zTTl2ujhMo5p/Oi6T72aJ9JbCRnZ0uKvuHfJxRZpBMpN3kTyxYddy7bmnz4oTf+xF4EsNKTXK1Sa8wkmYAZETlEV5GYDVHUgbL793mzxJBmMpBOpYuLmnq7CD6m9LBZvx6oI2T8PKTdOBHeJ3sIXAUAl2+sUXQNB3NHnDFL3Zqu5x2WujBPS90kCvLNtfNbAzmtIksycKF+xPnzaLLSX9y63EXbP6mteWqlITE2dR1f9JUoqkJ8ASbPMhrSynUsTKGVg3bjQoD49Pv+oXdEV628602Onym7NQ42XQTunPm17apNIvR2NB5kdpg1hW2WZ9bpZC3cz+ReRwkyCzvWm3XUe0yhTn1J1pzlvBZA82LhgoPFK/RGZ2djR2+2YxxfbHT+8Rly13nL98CYBBijMfkMbmQB5m4dgj5c3lqOCOrmt98GvUQlPGRyFisAdocOozFGfCsLzzLmAyO+5M7BV2/8yIUCatex6iOjFSMC0GCSqGSIb3DQEJFDEgHh4AYQBuAGQAcgBvAGkAZABkAGUAYgB1AGcAawBlAHkwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTc4MjIxNTIxNzE0MzCCBDEGCSqGSIb3DQEHBqCCBCIwggQeAgEAMIIEFwYJKoZIhvcNAQcBMGYGCSqGSIb3DQEFDTBZMDgGCSqGSIb3DQEFDDArBBRWW+moz/mRtLBlkgoHizQnTqWLdwICJxACASAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEOrvgdeiVL3VJwqD8S5dG66AggOg5WPKxKIaq3DNsUBSbMREqDqcDzXSl2TtZ9/zkIx9eIQQ+tJ/OeTpzWOZs19XbjQeCZVetBSjQkOs+nd7jzFht5sHaQcRcdJB1ynLyDru9ZXflQQR0Qsk15HOGzIyqTbyoUR0qMoUPnUMb1nxy12SaEKwYZmgmiW9fZQDIsE17HwKjk9+pDjFUM+/MJ/c1h+/x0cVaB9ZIxmlXjCTrxYWAoC/2sWZNipuRnLJe98M6iotszsGoms3e7B0B0fN2EXu39SabkK0EK3XF/QOX+0lWSVne679hjFcJ4iJsR1WQ550PGCvzBvp1L/ntgC+BxVqAO3DKsUAxacgQYbq3P4X2NV3MH2NR+nYbIwE/ZdtkzzDmmUsO14b+3thYavvIOVy51+j4RCVBWcx419YMBYFBaQGMbKyLfniV8tV5ypTwkKD9MvnbJZ3dD+BSNgxQ62M1LG6V38iW+ZhnOpGy4CvVUrjraJvMWCSe2B64/hwQt3gnOTAyrAhH9jOlDgG207df63IlBw1EdEYE9BwsYaZ+nJ2tcxjueneM3eAhVZNNhgYAmmTg2ZxUEMcF3jTdX2oyh4dS3D6gnrWZ2MkUoURlY0tauAi9GPAf1Sf5uiL2BFwV1tG5c5GNu2hguKYwh/TH0GN538OAkxj0ppC+CI/0d6Qq3AqIjlsjJ04Y4YJbX058h3EMSxlomaD8r2Dr5w0ERYyNzbfa9vzYDLRywXkm4/+BLI+KHrgJ5/30SgE/bJl23EwStMC2rvpnAEM8vWaQOjLs4Qqht9TRACf25zkRN8C5wsv3RNpP0q40SrgQ/1jKEUGi+Fhr5uVYgTO89xs/0cOleHyF/6lG6drApDd+8AzJ+I+7kLNt96LEDEeztdiD2VdEOaVvr5zECERlQOZw7+hzqSX2Ay2/m5yW7bz7cKGB3+8TRRShbPNtNfGxs6ram1j4190dRoXwbZL7sjmR2wR1PyRzfuRjzpoyTpZvpwwqHQZqWnjXUqTkBY31K0lvb9xZW6YxbVpykIxkGLFkBGq9p7aPmxMS/6bvBylSY1bGeiCF3LP/bJCpcuAbxGZBJqJlKNpiObOaQEBCW8xadRvpouY0+HwFl+iLkYykc16hGzk3yrrvf9AEJ5o1uq0Z+Hm4fhrKD4nSN1W6KYY1D1YdPbY5x/ifBtruxIAFS3Izz/+eAb5AEes+ZQxKZqzBoMroOuLgHV3XYofbCZMHzyVCPDHXX4tAsPk9xl7jBNMDEwDQYJYIZIAWUDBAIBBQAEIGNW1OE2TK0XXgeF5myhdVJMF51nSYhDJgPqipUXEqz/BBRw8ubv9978wPUiUYYmK6TQ2Qc69wICJxA="
      fallbackStr.toByteArray()
    }
    diagKeystore.writeBytes(decodedBytes)
    println("diagKeystore written successfully: size = ${diagKeystore.length()}")
  } catch (e: Exception) {
    println("Failed to write keystore during diagnostics: ${e.message}")
    e.printStackTrace()
  }
}
println("=== END DIAGNOSTICS ===")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.bajao.mscpyr"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
  doLast {
    try {
      val apkSource = file("${layout.buildDirectory.get()}/outputs/apk/debug/app-debug.apk")
      if (apkSource.exists()) {
        val apkDest = file("${rootDir}/Bajao.apk")
        apkSource.copyTo(apkDest, overwrite = true)
        logger.lifecycle("Successfully copied compiled APK to root: ${apkDest.absolutePath}")
      } else {
        logger.warn("Source APK not found for copying: ${apkSource.absolutePath}")
      }
    } catch (e: Exception) {
      logger.error("Failed to copy APK: ${e.message}")
    }
  }
}

