plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Optional: for logging HTTP requests

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.8.9")
    // OkHttp for HTTP requests
    testImplementation("com.squareup.okhttp3:okhttp:4.9.3")
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Optional: for logging HTTP requests
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

    // Gson for JSON parsing
    testImplementation("com.google.code.gson:gson:2.8.9")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ui.auth)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)

    androidTestImplementation(libs.appcompat)
    androidTestImplementation(libs.material)
    testImplementation(libs.activity)
    testImplementation(libs.constraintlayout)
    testImplementation(libs.firebase.auth)
    testImplementation(libs.firebase.database)
    testImplementation(libs.firebase.firestore)
    testImplementation(libs.firebase.storage)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
