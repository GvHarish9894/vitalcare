import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    // Crashlytics Gradle plugin — uploads the R8 mapping so release crash traces
    // de-obfuscate (CI). The runtime Analytics + Crashlytics SDKs are wired via
    // the shared module's AndroidTelemetry (behind the Telemetry seam, D-028),
    // gated by the Settings opt-out (D-029).
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Contributor-supplied Drive gate (D-027): flip by adding
// `vitalcare.drive.enabled=true` to local.properties after registering an
// OAuth client (package + signing SHA-1) in your Google Cloud project.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { stream -> load(stream) }
}
val driveEnabled = localProperties.getProperty("vitalcare.drive.enabled", "false")

// Version comes from version.properties, overridable per-build with
// -PVERSION_NAME / -PVERSION_CODE (used by the release CI workflow).
val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) file.inputStream().use { stream -> load(stream) }
}

// Release signing (CI): the keystore is decoded to signing/keystore.jks and the
// passwords come from SIGNING_* env vars. Absent for open-source clones — the
// release build then stays unsigned but still builds (D-027/NFR-9).
val releaseKeystore = rootProject.file("signing/keystore.jks")
val hasReleaseSigning = releaseKeystore.exists()

android {
    namespace = "com.techgv.vitalcare"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.techgv.vitalcare"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
            ?: versionProps.getProperty("VERSION_CODE")?.toIntOrNull()
            ?: 1
        versionName = (project.findProperty("VERSION_NAME") as? String)
            ?: versionProps.getProperty("VERSION_NAME")
            ?: "1.0"
        buildConfigField("boolean", "DRIVE_ENABLED", driveEnabled)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed in CI when a keystore is present; unsigned for plain clones.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Upload the R8 mapping to Crashlytics only when the CI release job
            // asks for it (-PenableCrashlyticsMappingUpload); off for local builds
            // so they never touch the network.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = project.hasProperty("enableCrashlyticsMappingUpload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Consumed by the release CI workflow to read the current version (D-032 CI).
tasks.register("printVersionName") {
    val name = versionProps.getProperty("VERSION_NAME") ?: "1.0"
    doLast { println(name) }
}