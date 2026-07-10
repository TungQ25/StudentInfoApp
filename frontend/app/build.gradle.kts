plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.taskmanagerapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.taskmanagerapp"
        minSdk = 24
        targetSdk = 36
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycleViewmodel)
    implementation(libs.lifecycleLivedata)
    implementation(libs.roomRuntime)
    annotationProcessor(libs.roomCompiler)
    implementation(libs.retrofit)
    implementation(libs.retrofitGson)
    implementation(libs.okhttpLogging)
    implementation(libs.workRuntime)
    implementation(libs.emoji2Picker)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
