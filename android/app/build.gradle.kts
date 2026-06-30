plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.claims.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.claims.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // API base URL. Defaults to the public TLS endpoint; override with
        // -PbaseUrl=... (e.g. http://10.0.2.2:8090 for an emulator against the local backend).
        val baseUrl = (project.findProperty("baseUrl") as String?) ?: "https://danovich.ddns.net:28587"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        // Social provider config injected at build time (blank disables the button).
        // The GitHub *secret* lives only on the backend; the app needs just the public client id.
        val googleClientId = (project.findProperty("googleServerClientId") as String?) ?: ""
        val githubClientId = (project.findProperty("githubClientId") as String?) ?: ""
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"$githubClientId\"")
        buildConfigField("String", "GITHUB_REDIRECT_URI", "\"claimsapp://oauth\"")
        manifestPlaceholders["oauthScheme"] = "claimsapp"
        manifestPlaceholders["oauthHost"] = "oauth"

        // Ship native libs only for real arm64 devices + x86_64 emulators
        // (drops armeabi-v7a and x86 — ~20 MB of bundled ML Kit OCR engine).
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true       // R8 code shrinking/obfuscation
            isShrinkResources = true     // strip unused resources
            // Debug-signed so the release APK is installable for testing. For real
            // distribution, replace with a dedicated release keystore (kept out of git).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material XML theme host for the Compose activity.
    implementation("com.google.android.material:material:1.12.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Scanned-document image loading (carries the bearer via a shared OkHttp client)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ML Kit — on-device document scanner + text recognition (nothing leaves the phone)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Social sign-in transitive runtime used by platform-login-ui
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.browser:browser:1.8.0")

    // platform-stack auth (source subprojects from the submodule)
    implementation(project(":android-auth"))
    implementation(project(":platform-login-ui"))
}
