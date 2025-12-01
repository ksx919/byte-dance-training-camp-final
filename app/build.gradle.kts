import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
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
            // 【核心修改 1】开启代码混淆和 R8 优化！
            // 只有设为 true，编译器才会进行深度优化，这才是 Release 包性能强悍的原因。
            isMinifyEnabled = true

            // 【核心修改 2】开启资源压缩，移除无用的图片和布局文件
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 【核心修改 3】使用 Debug 签名方便直接运行
            // 这样你切换到 Release Variant 后，直接点 Run 就能装到手机上，不需要生成 jks 文件。
            // 注意：正式发布上线时，请去掉这一行，配置真正的签名文件。
            signingConfig = signingConfigs.getByName("debug")
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

    // 高德地图
    implementation("com.amap.api:location:6.4.1")
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.google.android.material:material:1.13.0")

    implementation("com.tencent:mmkv:2.0.2")


    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}