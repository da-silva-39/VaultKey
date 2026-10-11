plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "mz.ac.ucm.feng.ti.vaultkey"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mz.ac.ucm.feng.ti.vaultkey"
        // minSdk 26: cobre Android Keystore AES/GCM (API 23+), BiometricPrompt via
        // AndroidX (API 28+ nativo, com fallback controlado) e CameraX (API 21+).
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Persistência local (Room 2.x sobre SQLite — Java + annotationProcessor)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Estado da UI
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Leitura de QR Code: apenas ZXing core (descodificação, offline).
    // A captura usa a API Camera do framework (sem dependências novas);
    // a biometria usa android.hardware.biometrics (API 28+, nativo).
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.core)
}