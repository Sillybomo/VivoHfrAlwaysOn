plugins {
    id("com.android.application")
}

android {
    namespace = "com.bomo.vivohfr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bomo.vivohfr"
        minSdk = 31
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/vivohfr.jks")
            storePassword = System.getenv("HFR_KS_PASS") ?: "changeme"
            keyAlias = "vivohfr"
            keyPassword = System.getenv("HFR_KS_PASS") ?: "changeme"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        checkReleaseBuilds = false
    }

    dependenciesInfo {
        includeInApk = false
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.9.1")
    compileOnly("io.github.libxposed:api:101.0.1")
}
