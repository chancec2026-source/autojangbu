import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 공유 서버 주소와 앱 키는 저장소에 올리지 않는 local.properties에서 읽는다.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "kr.chance.autojangbu"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.chance.autojangbu"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SHARE_URL", "\"${localProps.getProperty("autojangbu.shareUrl", "")}\"")
        buildConfigField("String", "SHARE_KEY", "\"${localProps.getProperty("autojangbu.appKey", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.vehicle.sdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
    // 로컬 단위 테스트에서 안드로이드 org.json 대신 실제 구현을 쓴다.
    testImplementation("org.json:json:20231013")
}
