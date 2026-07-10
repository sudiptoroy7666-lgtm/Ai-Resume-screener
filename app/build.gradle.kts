plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.airesumescreener"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/LICENSE.md" // <--- Added for poi-scratchpad
        }
    }

    defaultConfig {
        applicationId = "com.example.airesumescreener"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Splash Screen & RecyclerView
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Flexbox for modern chip wrapping
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Networking (Retrofit + Gson)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // File Parsing (PDFBox)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // File Parsing (Apache POI for DOC/DOCX)
    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("org.apache.xmlbeans:xmlbeans:5.2.1")
    implementation("org.apache.poi:poi-scratchpad:5.3.0")

    // CRITICAL: Transitive dependencies required for POI on Android
    implementation("commons-io:commons-io:2.16.1")
    implementation("commons-codec:commons-codec:1.17.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}