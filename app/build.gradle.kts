plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.verbanode.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.verbanode.mobile"
        minSdk = 23
        targetSdk = 37
        versionCode = 23
        versionName = "0.5.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val path = providers.environmentVariable("VN_KEYSTORE_PATH").orNull
        if (!path.isNullOrBlank()) {
            create("release") {
                storeFile = file(path)
                storePassword = providers.environmentVariable("VN_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("VN_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("VN_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    // Canonical Kotlin source root.
    // Older VerbaNode Android revisions used src/main/java for Kotlin. Explicitly
    // compiling only src/main/kotlin prevents stale overlay files from older
    // architectures from being mixed into current builds.
    sourceSets.named("main") {
        kotlin.directories.clear()
        kotlin.directories.add("src/main/kotlin")
    }

    // Canonical test roots for overlay-safe builds. Useful legacy coverage is
    // restored under src/test/kotlin; obsolete src/test/java overlays remain
    // excluded so old contracts cannot be mixed into current CI runs.
    sourceSets.named("test") {
        kotlin.directories.clear()
        kotlin.directories.add("src/test/kotlin")
    }
    sourceSets.named("androidTest") {
        kotlin.directories.clear()
        kotlin.directories.add("src/androidTest/kotlin")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
