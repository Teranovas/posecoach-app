plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.posecoach"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.posecoach"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 로컬 서버 URL을 BuildConfig에 넣어 사용 (에뮬레이터: 10.0.2.2)
        buildConfigField "String", "POSE_SERVER_BASE_URL", "\"http://10.0.2.2:5001/\"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation "com.squareup.retrofit2:retrofit:2.11.0"
    implementation "com.squareup.retrofit2:converter-gson:2.11.0"
    implementation "com.squareup.okhttp3:okhttp:4.12.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"

    // 이미지 로딩(선택)
    implementation "io.coil-kt:coil:2.6.0"
}