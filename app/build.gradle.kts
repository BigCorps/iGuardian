plugins {
    id("com.android.application")
}

val devKeystorePath = System.getenv("GUARDIAN_DEV_KEYSTORE_PATH")
val devStorePassword = System.getenv("GUARDIAN_DEV_STORE_PASSWORD")
val devKeyAlias = System.getenv("GUARDIAN_DEV_KEY_ALIAS")
val devKeyPassword = System.getenv("GUARDIAN_DEV_KEY_PASSWORD")

val devSigningReady =
    !devKeystorePath.isNullOrBlank() &&
    !devStorePassword.isNullOrBlank() &&
    !devKeyAlias.isNullOrBlank() &&
    !devKeyPassword.isNullOrBlank()

android {
    namespace = "com.bigcorps.guardian"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bigcorps.guardian.dev"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.1.8"
    }

    if (devSigningReady) {
        signingConfigs {
            create("guardianDev") {
                storeFile = file(devKeystorePath!!)
                storePassword = devStorePassword!!
                keyAlias = devKeyAlias!!
                keyPassword = devKeyPassword!!
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (devSigningReady) {
                signingConfig = signingConfigs.getByName("guardianDev")
            }
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.12.0")
    testImplementation("junit:junit:4.13.2")
}
