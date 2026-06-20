plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.exemplo.fluidez"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.exemplo.fluidez"
        minSdk = 26
        targetSdk = 35
        // Em build local usa os valores fixos; no GitHub Actions são sobrescritos.
        versionCode = (project.findProperty("versionCodeOverride") as String?)?.toIntOrNull() ?: 11
        versionName = (project.findProperty("versionNameOverride") as String?) ?: "1.10"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("fluidez") {
            storeFile = file("fluidez.jks")
            storePassword = "fluidez123"
            keyAlias = "fluidez"
            keyPassword = "fluidez123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fluidez")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Shizuku (poderes de ADB sem root)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // ADB embutido (alternativa ao Shizuku)
    implementation(libs.libadb.android)
    implementation(libs.conscrypt.android)
    implementation(libs.sun.security.android)

    debugImplementation(libs.androidx.ui.tooling)
}
