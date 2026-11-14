plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// A chave de assinatura não fica no repositório: vem dos Secrets do GitHub
// (KEYSTORE_BASE64 e KEYSTORE_PASSWORD), decodificada pelo workflow.
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")

android {
    namespace = "com.marciel.alertavelocidade"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marciel.alertavelocidade"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    // Chave própria do app (fixa): permite instalar versões novas por cima.
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = keystorePassword
            keyAlias = "alertavelocidade"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
