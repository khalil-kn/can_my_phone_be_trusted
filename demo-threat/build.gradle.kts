// A harmless fake "spy" app used only to test detection: it asks for SMS and microphone
// access and has an accessibility service, like real stalkerware, but does nothing.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.flashtorch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.flashtorch"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
