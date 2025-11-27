import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.rednote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rednote"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val amapWebApiKey = localProperties.getProperty("AMAP_WEB_API_KEY") ?: ""
        buildConfigField("String", "AMAP_WEB_API_KEY", "\"$amapWebApiKey\"")
        val amapApikey = localProperties.getProperty("AMAP_API_KEY") ?: ""
        manifestPlaceholders["AMAP_API_KEY"] = amapApikey
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
    
    // 高德地图：定位 (建议使用稳定的版本, 例如: 6.4.1)
    implementation("com.amap.api:location:6.4.1")
    // OkHttp: 发送网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Gson: Google 出品的 JSON 解析库
    implementation("com.google.code.gson:gson:2.10.1")
    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.google.android.material:material:1.13.0")
}