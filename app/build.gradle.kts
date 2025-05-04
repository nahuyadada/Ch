plugins {
    alias(libs.plugins.androidApplication)
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.chowcheck"
    compileSdk = 35 // Ensure your Android Gradle Plugin supports this or use 34

    defaultConfig {
        applicationId = "com.example.chowcheck"
        minSdk = 24
        targetSdk = 34 // Keep targetSdk consistent with compileSdk preferably, or slightly lower
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Verify compatibility with Kotlin & AGP versions
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Add exclusion to ALL implementation dependencies
    implementation(libs.androidx.core.ktx) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.lifecycle.runtime.ktx) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.activity.compose) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(platform(libs.androidx.compose.bom)) // Platform BOMs are special, don't add exclusion here
    implementation(libs.androidx.ui) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.ui.graphics) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.ui.tooling.preview) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.material3) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    // Already added exclusion here, keep it
    implementation(libs.androidx.appcompat) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    // Already added exclusion here, keep it
    implementation(libs.material) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    implementation(libs.androidx.activity) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.constraintlayout) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation(libs.androidx.cardview) {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    implementation("androidx.viewpager2:viewpager2:1.0.0") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("com.google.code.gson:gson:2.10.1") {
        // Gson might not bring it in, but won't hurt to add exclusion if unsure
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }
    implementation("androidx.fragment:fragment-ktx:1.7.1") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}