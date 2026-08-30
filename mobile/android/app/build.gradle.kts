plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "uz.repairauto.mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "uz.repairauto.mobile"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH")
                .orElse(providers.gradleProperty("RELEASE_KEYSTORE_PATH"))
                .orNull
                ?: providers.environmentVariable("RELEASE_STORE_FILE")
                .orElse(providers.gradleProperty("RELEASE_STORE_FILE"))
                .orNull

            val keystorePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD")
                .orElse(providers.gradleProperty("RELEASE_KEYSTORE_PASSWORD"))
                .orNull
                ?: providers.environmentVariable("RELEASE_STORE_PASSWORD")
                .orElse(providers.gradleProperty("RELEASE_STORE_PASSWORD"))
                .orNull

            val keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS")
                .orElse(providers.gradleProperty("RELEASE_KEY_ALIAS"))
                .orNull
                ?: providers.environmentVariable("KEY_ALIAS")
                .orElse(providers.gradleProperty("KEY_ALIAS"))
                .orNull

            val keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD")
                .orElse(providers.gradleProperty("RELEASE_KEY_PASSWORD"))
                .orNull
                ?: providers.environmentVariable("KEY_PASSWORD")
                .orElse(providers.gradleProperty("KEY_PASSWORD"))
                .orNull

            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            val releaseSigning = signingConfigs.getByName("release")
            val allowDebugFallback = providers.gradleProperty("ALLOW_DEBUG_SIGNING_FALLBACK")
                .orElse(providers.environmentVariable("ALLOW_DEBUG_SIGNING_FALLBACK"))
                .map { it.toBoolean() }
                .getOrElse(false)

            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            } else if (allowDebugFallback) {
                // Explicitly opted-in non-production development build mode
                logger.warn("WARNING: Using debug signing for release build because ALLOW_DEBUG_SIGNING_FALLBACK=true is set.")
                signingConfig = signingConfigs.getByName("debug")
            } else {
                // Release builds must not silently use machine-specific debug keys
                signingConfig = null
            }
        }
    }
}

dependencies {
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
