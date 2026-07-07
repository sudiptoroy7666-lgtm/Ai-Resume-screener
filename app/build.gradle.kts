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

        // Networking (Retrofit + Gson)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // File Parsing (PDFBox)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // File Parsing (Apache POI for DOC/DOCX)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.poi:poi-scratchpad:5.2.3")

    // CRITICAL: Transitive dependencies required for POI on Android
    implementation("commons-io:commons-io:2.15.1")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-compress:1.25.0")
    implementation("org.apache.logging.log4j:log4j-api:2.21.1")

        // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}