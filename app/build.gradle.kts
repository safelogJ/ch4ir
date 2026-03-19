import com.android.build.api.dsl.ApplicationExtension


plugins {
    alias(libs.plugins.android.application)
}

configure <ApplicationExtension> {
    namespace = "com.safelogj.ch4ir"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.safelogj.ch4ir"
        minSdk = 23
        targetSdk = 36
        versionCode = 86
        versionName = "2.5.7 ch4ir"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.exoplayer)
    implementation(libs.exoplayerui)
    implementation(libs.exoplayerrtsp)
    implementation(libs.exoplayerhls)
    implementation(libs.exoplayerdash)
    implementation(libs.exoplayerdata)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.documentfile)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.transition)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}