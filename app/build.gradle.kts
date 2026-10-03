plugins { id("com.android.application") }

android {
    namespace = "com.example.dailycandle"
    compileSdk = 36
    enableKotlin = false

    defaultConfig {
        applicationId = "com.example.dailycandle"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.4"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
