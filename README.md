# 🚀 ResumeAI Enterprise: ATS Resume Screener & Applicant Portal

> **Version:** 1.0  
> **Platform:** Android (API 28+)  
> **Language:** Kotlin  
> **Last Updated:** July 2026  

ResumeAI Enterprise is a comprehensive, dual-sided native Android application designed to bridge the gap between job applicants and HR departments. It leverages Large Language Models (LLMs) to act as an intelligent Applicant Tracking System (ATS), providing deep semantic analysis of resumes against specific job descriptions — surfacing matched skills, missing qualifications, formatting issues, and an overall fit score.

---

## 📑 Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Key Features](#2-key-features)
3. [User Roles & Detailed Workflows](#3-user-roles--detailed-workflows)
4. [Complete Project Structure](#4-complete-project-structure)
5. [Technology Stack & Dependencies](#5-technology-stack--dependencies)
6. [Prerequisites](#6-prerequisites)
7. [Step-by-Step Setup Guide](#7-step-by-step-setup-guide)
8. [Configuration & Customization](#8-configuration--customization)
9. [Architecture Deep Dive](#9-architecture-deep-dive)
10. [Data Models Reference](#10-data-models-reference)
11. [Firestore Database Schema](#11-firestore-database-schema)
12. [AI Prompt Engineering](#12-ai-prompt-engineering)
13. [Security Model](#13-security-model)
14. [Activity & Screen Reference](#14-activity--screen-reference)
15. [Adapter & UI Component Reference](#15-adapter--ui-component-reference)
16. [Utility & Service Reference](#16-utility--service-reference)
17. [Handover & Future Migration Guide](#17-handover--future-migration-guide)
18. [Known Limitations & Scaling Notes](#18-known-limitations--scaling-notes)
19. [Troubleshooting & FAQ](#19-troubleshooting--faq)
20. [Deployment & Release Checklist](#20-deployment--release-checklist)
21. [Contributing Guidelines](#21-contributing-guidelines)
22. [License](#22-license)

---

## 1. Executive Summary

The application serves two primary functions:

1. **For Applicants:** A personal ATS scanner to test resumes against any job description, identifying missing keywords, formatting issues, and overall match percentage before applying to real jobs.
2. **For HR/Recruiters:** A secure, PIN-protected portal to manage organizations, create job postings with full requirements, receive applicant submissions through a dedicated portal, run batch AI analysis on entire candidate pools, view detailed candidate reports, and export ranked results to Excel.

The app is designed as a **single APK** that serves both personas through a role selection screen at launch, making deployment and distribution simple.

---

## 2. Key Features

### 🤖 AI-Powered Analysis
- Semantic matching that understands context, not just keyword counting
- Hard skills and soft skills extraction and comparison
- ATS formatting issue detection
- Natural language summary generation
- Structured JSON output enforced via system prompts

### 📄 Document Processing
- Native on-device text extraction for `.pdf`, `.docx`, and legacy `.doc` files
- Automatic text cleaning and normalization
- Configurable size limits (10MB file, 15K characters)
- Extraction quality warnings for scanned/image-based PDFs

### 🏢 HR Portal
- PIN-protected admin access (4-6 digits)
- Organization CRUD operations with device-based ownership
- Job posting management with full requirements storage
- Batch candidate analysis with progress tracking
- WakeLock management to prevent device sleep during long analyses
- Auto-retry logic for API rate limits (429 errors)

### 📊 Results & Export
- Color-coded Strong/Moderate/Weak scoring
- Detailed candidate report with skills breakdown
- One-click Excel (.xlsx) export with ranked candidates
- Print-ready HTML candidate reports
- Unified scan history combining personal and batch analyses

### 📱 User Experience
- Material Design 3 UI with consistent teal/dark theme
- Step-by-step wizard for resume analysis
- Real-time form validation
- Animated skill chips and score indicators
- Offline-first with Firebase persistence

---

## 3. User Roles & Detailed Workflows

### 3.1 Application Launch Flow

App Launch
    │
    ▼
SplashActivity (1.5s branded animation)
    │
    ▼
RoleSelectionActivity
    ├── "I'm an Applicant" → ApplicantPortalActivity
    └── "I'm HR / Recruiter" → AdminActivity (PIN required)
```

### 3.2 Applicant / Personal User Workflows

#### Workflow A: Quick Scan (Personal ATS Check)

```
ApplicantPortalActivity → "Check My Resume" → MainActivity
    │
    ├─ Step 1: Upload Resume (PDF/DOCX/DOC)
    │     └─ File validation: type check, size check (<10MB)
    │     └─ On-device text extraction (PDFBox / Apache POI)
    │     └─ Quality check: warns if <50 words extracted
    │
    ├─ Step 2: Paste Job Description (min 20 chars)
    │     └─ Real-time validation with TextWatcher
    │
    ├─ Step 3: "Run ATS Analysis"
    │     └─ Text truncation to 40K chars
    │     └─ API call to LLM with structured JSON prompt
    │     └─ Response parsing and JSON extraction
    │
    └─ Results Display:
          ├─ ATS Score (Circular Progress Indicator)
          ├─ Verdict Strip (Strong/Moderate/Weak)
          ├─ AI Summary
          ├─ Matched/Missing Hard Skills (Flexbox Chips)
          ├─ Matched/Missing Soft Skills (Flexbox Chips)
          ├─ ATS Formatting Issues (Flexbox Chips)
          ├─ "Export Report" → Excel (.xlsx)
          └─ "Screen Another" → Reset Form
```

#### Workflow B: Submit Resume to HR

```
ApplicantPortalActivity
    │
    ├─ Enter Organization Code (from HR)
    ├─ Enter Job Opening ID (from HR)
    ├─ Enter Full Name
    ├─ Enter Email Address
    ├─ Upload Resume (PDF/DOCX only, max 10MB)
    │     └─ Text extraction and validation
    │
    └─ "Submit Application"
          ├─ Firebase Anonymous Auth
          ├─ Validates org + job exist in Firestore
          ├─ Saves candidate document with status "pending"
          ├─ Increments job cvCount
          └─ Shows success confirmation + auto-reset
```

#### Workflow C: View History

```
HistoryActivity
    │
    ├─ Loads local personal scans (HistoryManager/JSON)
    ├─ Loads batch-analyzed candidates from Firestore
    ├─ Filters by device ID and HR batch scans
    ├─ Caches org names and job titles
    ├─ Sorts by score descending
    │
    └─ Tap Item:
          ├─ Batch result → CandidateReportActivity (full detail)
          └─ Personal scan → Toast with summary
```

### 3.3 HR / Recruiter (Admin) Workflows

#### Workflow D: HR Portal Access

```
AdminActivity
    │
    ├─ First Launch:
    │     └─ "Set Admin PIN" dialog (4-6 digits)
    │     └─ PIN stored in SharedPreferences
    │
    └─ Returning:
          └─ "Enter Admin PIN" dialog
          └─ Validates against stored PIN
          └─ On success: loads owned organizations
```

#### Workflow E: Organization Management

```
AdminActivity (Authenticated)
    │
    ├─ "Create New Organization"
    │     ├─ Input: Organization Name (min 3 chars)
    │     ├─ Generates ID: uppercase, alphanumeric + underscore
    │     ├─ Creates Firestore document
    │     ├─ Registers ownership locally
    │     └─ Refreshes list
    │
    ├─ Tap Organization → JobDetailsActivity
    │
    └─ Delete Organization
          ├─ Confirmation dialog
          ├─ Deletes all jobs and candidates recursively
          ├─ Removes local ownership
          └─ Refreshes list
```

#### Workflow F: Job Opening Management

```
JobDetailsActivity (for specific org)
    │
    ├─ "Create New Job Opening"
    │     ├─ Job Title / ID (min 3 chars)
    │     ├─ Brief Description (for display)
    │     ├─ Full Requirements (min 50 chars, for AI matching)
    │     └─ Creates Firestore document
    │
    ├─ For each Job Opening card:
    │     ├─ "Analyze" → Batch Analysis (Workflow G)
    │     ├─ "Results" → JobResultsActivity (Workflow H)
    │     └─ "Close Job" → Deletes job + all candidates
    │
    └─ Info card shows portal sharing instructions
```

#### Workflow G: Batch AI Analysis

```
JobDetailsActivity → "Analyze" button
    │
    ├─ Validates job has requirements
    ├─ Acquires WakeLock (30 min timeout)
    ├─ Shows progress dialog
    │
    ├─ Fetches all candidates from Firestore
    ├─ For each candidate:
    │     ├─ Updates progress bar
    │     ├─ Truncates resume text to 20K chars
    │     ├─ Truncates job requirements to 15K chars
    │     ├─ Constructs AI prompt with both texts
    │     ├─ Makes API call (3 retries on 429)
    │     ├─ Parses JSON response
    │     ├─ Updates candidate object with score, skills, etc.
    │     ├─ Writes back to Firestore
    │     └─ 3-second delay between requests
    │
    ├─ Releases WakeLock
    ├─ Dismisses progress dialog
    └─ Launches JobResultsActivity with analyzed candidates
```

#### Workflow H: Results Review & Export

```
JobResultsActivity
    │
    ├─ Scenario A: Fresh analysis (receives candidates via Intent)
    ├─ Scenario B: View existing (loads from Firestore cache)
    │
    ├─ Stats Strip: Total, Avg Score, Strong, Moderate, Weak
    ├─ Sorted candidate list (by score descending)
    │
    ├─ Tap Candidate → CandidateReportActivity
    │     ├─ Full score dashboard
    │     ├─ Personal info (name, email, phone, address)
    │     ├─ Experience & Education summaries
    │     ├─ Hard/Soft skills (matched + missing)
    │     ├─ Formatting issues
    │     └─ "Print Full Report" → WebView HTML → PrintManager
    │
    └─ "Export All to Excel"
          ├─ File picker for save location
          └─ Generates .xlsx with all candidate data
```

---

## 4. Complete Project Structure

```
airesumescreener/
├── app/
│   ├── build.gradle.kts                    # App-level build config
│   ├── proguard-rules.pro                  # ProGuard rules (if minification enabled)
│   ├── google-services.json                # Firebase config (DO NOT commit)
│   │
│   └── src/main/
│       ├── AndroidManifest.xml             # App manifest, permissions, activities
│       │
│       ├── java/com/example/airesumescreener/
│       │   │
│       │   ├── ResumeAIApplication.kt      # Application class, Firebase init
│       │   │
│       │   │── Activities ──
│       │   ├── SplashActivity.kt           # Launch screen with animation
│       │   ├── RoleSelectionActivity.kt    # Applicant vs HR chooser
│       │   ├── MainActivity.kt             # Quick Scan / Personal ATS
│       │   ├── ApplicantPortalActivity.kt  # Submit resume to HR
│       │   ├── HistoryActivity.kt          # Unified scan history
│       │   ├── AdminActivity.kt            # HR org management
│       │   ├── JobDetailsActivity.kt       # Job openings + batch analysis
│       │   ├── JobResultsActivity.kt       # Ranked candidate results
│       │   └── CandidateReportActivity.kt  # Detailed candidate report
│       │   │
│       │   │── Adapters ──
│       │   ├── CandidateAdapter.kt         # RecyclerView for candidate list
│       │   ├── HistoryAdapter.kt           # RecyclerView for history items
│       │   ├── JobOpeningAdapter.kt        # RecyclerView for job openings
│       │   └── OrganizationAdapter.kt      # RecyclerView for organizations
│       │   │
│       │   │── Data Models ──
│       │   ├── Models.kt                   # All data classes (see §10)
│       │   │
│       │   │── Services & Utilities ──
│       │   ├── FirebaseService.kt          # Firestore CRUD operations
│       │   ├── AdminAuthManager.kt         # PIN & org ownership management
│       │   ├── HistoryManager.kt           # Local JSON history storage
│       │   ├── DeviceIdHelper.kt           # Android ID hashing
│       │   ├── ExcelExporter.kt            # Apache POI Excel generation
│       │   └── WakeLockManager.kt          # Power management for batch ops
│       │   │
│       │   │── API ──
│       │   ├── CerebrasApi.kt              # Retrofit interface (in Models.kt)
│       │   ├── RetryInterceptor.kt         # OkHttp retry logic (in MainActivity.kt)
│       │   ├── ChatRequest.kt              # Request body model
│       │   └── ChatResponse.kt             # Response body model
│       │
│       └── res/
│           ├── layout/
│           │   ├── activity_admin.xml
│           │   ├── activity_applicant_portal.xml
│           │   ├── activity_candidate_report.xml
│           │   ├── activity_history.xml
│           │   ├── activity_job_details.xml
│           │   ├── activity_job_results.xml
│           │   ├── activity_main.xml
│           │   ├── activity_role_selection.xml
│           │   ├── activity_splash.xml
│           │   ├── dialog_batch_progress.xml
│           │   ├── item_candidate_result.xml
│           │   ├── item_history.xml
│           │   ├── item_job_opening.xml
│           │   └── item_organization.xml
│           │
│           ├── menu/
│           │   ├── history_menu.xml         # Clear history action
│           │   └── main_menu.xml            # History, Submit to HR, HR Portal
│           │
│           ├── drawable/
│           │   ├── app_logo.png
│           │   ├── baseline_cloud_upload_24.xml
│           │   ├── outline_barcode_scanner_24.xml
│           │   ├── circle_bg.xml
│           │   └── gradient_background.xml
│           │
│           ├── xml/
│           │   ├── backup_rules.xml
│           │   ├── data_extraction_rules.xml
│           │   └── file_paths.xml           # FileProvider paths
│           │
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   ├── themes.xml
│           │   └── styles.xml
│           │
│           └── values-night/
│               └── themes.xml
│
├── build.gradle.kts                        # Project-level build config
├── settings.gradle.kts                     # Module includes
├── gradle.properties                       # Gradle JVM settings
├── local.properties                        # SDK path + API keys (DO NOT commit)
└── README.md                               # This file
```

---

## 5. Technology Stack & Dependencies

### 5.1 Core Technologies

| Category | Technology | Version | Purpose |
|:---|:---|:---|:---|
| **Language** | Kotlin | Latest stable | Core application logic |
| **Min SDK** | Android API 28 | Android 9.0 | Minimum supported device |
| **Target SDK** | Android API 35 | Android 15 | Compile and target version |
| **Build System** | Gradle (Kotlin DSL) | Via AGP | Build automation |
| **UI Framework** | XML Layouts + ViewBinding | Material 1.12.0 | Native Android UI |

### 5.2 Complete Dependency List

```kotlin
// ── AndroidX Core ──
implementation("androidx.core:core-ktx")
implementation("androidx.appcompat:appcompat")
implementation("androidx.constraintlayout:constraintlayout")
implementation("androidx.activity:activity-ktx")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
implementation("androidx.core:core-splashscreen:1.0.1")

// ── Material Design ──
implementation("com.google.android.material:material:1.12.0")

// ── Layout ──
implementation("com.google.android.flexbox:flexbox:3.0.0")

// ── Networking ──
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// ── Firebase ──
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-crashlytics")
implementation("com.google.firebase:firebase-analytics")

// ── Coroutines ──
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

// ── Document Parsing ──
implementation("com.tom-roush:pdfbox-android:2.0.27.0")
implementation("org.apache.poi:poi:5.3.0")
implementation("org.apache.poi:poi-ooxml:5.3.0")
implementation("org.apache.poi:poi-scratchpad:5.3.0")
implementation("org.apache.xmlbeans:xmlbeans:5.2.1")

// ── POI Transitive Dependencies ──
implementation("commons-io:commons-io:2.16.1")
implementation("commons-codec:commons-codec:1.17.0")
implementation("org.apache.commons:commons-collections4:4.4")
implementation("org.apache.commons:commons-compress:1.26.2")
implementation("org.apache.logging.log4j:log4j-api:2.23.1")
```

### 5.3 Gradle Plugins

```kotlin
// Project-level
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.google.services) apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

// App-level
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("com.google.firebase.crashlytics")
}
```

### 5.4 Packaging Exclusions

```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/DEPENDENCIES"
        excludes += "/META-INF/NOTICE.md"
        excludes += "/META-INF/INDEX.LIST"
        excludes += "/META-INF/LICENSE.md"
    }
}
```
> These exclusions are **required** to prevent build failures caused by conflicting META-INF files from Apache POI and other libraries.

---

## 6. Prerequisites

Before setting up this project, ensure you have:

- [ ] **Android Studio** Ladybug (2024.2) or newer
- [ ] **JDK 11+** installed and configured
- [ ] **Android SDK** with API 35 installed
- [ ] **Firebase Account** with billing enabled (Blaze plan recommended for Crashlytics)
- [ ] **LLM API Key** from one of:
  - Google AI Studio (Gemini)
  - OpenAI (GPT-4o)
  - Cerebras (Llama)
  - Any OpenAI-compatible endpoint
- [ ] **Git** for version control

---

## 7. Step-by-Step Setup Guide

### Step 1: Clone & Open

```bash
git clone <repository-url>
cd ResumeAI-Enterprise
```
Open the project in Android Studio and wait for Gradle sync to complete.

### Step 2: Firebase Project Setup

1. Navigate to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add Project"** → Name it → Disable Google Analytics (optional) → Create
3. Click the **Android icon** to add an app
4. Enter package name: `com.example.airesumescreener`
5. Download `google-services.json`
6. Place the file in the `app/` directory
7. Click **Next** through the remaining steps (Gradle plugin is already configured)

### Step 3: Enable Firebase Services

#### 3a. Anonymous Authentication
1. Go to **Authentication** → **Sign-in method**
2. Enable **Anonymous** sign-in
3. Save

#### 3b. Firestore Database
1. Go to **Firestore Database** → **Create database**
2. Select **Start in production mode** (or test mode for development)
3. Choose your region
4. Apply the security rules (see below)

#### 3c. Crashlytics
1. Go to **Crashlytics** → **Enable**
2. Crash reporting is automatically enabled for release builds via:
   ```kotlin
   FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
   ```

### Step 4: Firestore Security Rules

Navigate to **Firestore** → **Rules** and paste:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Organizations root
    match /organizations/{orgId} {
      allow read, write: if request.auth != null;

      // Job openings subcollection
      match /job_openings/{jobId} {
        allow read, write: if request.auth != null;

        // Candidates subcollection
        match /candidates/{candidateId} {
          allow read, write: if request.auth != null;
        }
      }
    }

    // Collection group query for history
    match /{document=**}/candidates/{candidateId} {
      allow read: if request.auth != null;
    }
  }
}
```

> ⚠️ **Production Warning:** The above rules allow any authenticated (including anonymous) user to read/write all data. For production, implement role-based access control with Firebase Custom Claims or move to a backend API with JWT validation.

### Step 5: API Key Configuration

1. Open (or create) `local.properties` in the project root
2. Add your LLM API key:

```properties
sdk.dir=/path/to/your/android/sdk
CEREBRAS_API_KEY="AIzaSyB-your-actual-api-key-here"
```

> ⚠️ **Never commit `local.properties` to version control.** It should be listed in `.gitignore`.

### Step 6: Build & Run

1. Connect a physical device (API 28+) or start an emulator
2. Click **Run ▶** in Android Studio
3. The app will install and launch with the splash screen

### Step 7: Verify Functionality

- [ ] Splash screen displays for 1.5 seconds
- [ ] Role selection shows two options
- [ ] Quick Scan: Upload a PDF, paste a JD, get results
- [ ] HR Portal: Set PIN, create org, create job
- [ ] Applicant Portal: Submit a resume with org/job codes
- [ ] History: Shows past scans

---

## 8. Configuration & Customization

### 8.1 Changing the AI Provider

All AI configuration is in `MainActivity.kt` (top-level constants):

```kotlin
// Current configuration (Google Gemini via OpenAI proxy)
const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"
const val MODEL_ID = "gemini-3.5-flash"
```

#### To use OpenAI directly:
```kotlin
const val BASE_URL = "https://api.openai.com/v1/"
const val MODEL_ID = "gpt-4o-mini"
```

#### To use Cerebras:
```kotlin
const val BASE_URL = "https://api.cerebras.ai/v1/"
const val MODEL_ID = "llama-3.3-70b"
```

#### To use any OpenAI-compatible endpoint (Ollama, Together, Groq, etc.):
```kotlin
const val BASE_URL = "https://your-endpoint.com/v1/"
const val MODEL_ID = "your-model-name"
```

> **Important:** After changing `BASE_URL`, ensure your `CEREBRAS_API_KEY` in `local.properties` matches the new provider's key format.

### 8.2 Adjusting Rate Limit & Retry Behavior

In `JobDetailsActivity.kt`, the batch analysis loop includes:

```kotlin
// Delay between API calls (currently 3 seconds)
delay(3000)

// Retry on 429 (rate limit) - up to 3 attempts
var retries = 0
while (retries < 3) {
    if (res.code() == 429) {
        delay(10000)  // Wait 10 seconds
        retries++
    }
}
```

Adjust these values based on your API provider's rate limits.

### 8.3 File Size and Token Limits

| Setting | Location | Current Value | Description |
|:---|:---|:---|:---|
| `MAX_SIZE_BYTES` | `ApplicantPortalActivity.kt` | 10MB | Max uploaded file size |
| `MAX_TEXT_CHARS` | `ApplicantPortalActivity.kt` | 15,000 | Max extracted text for applicant portal |
| Resume truncation | `MainActivity.kt` `cleanAndTruncateText()` | 40,000 | Max chars sent to AI for personal scan |
| Resume truncation | `JobDetailsActivity.kt` | 20,000 | Max chars sent to AI for batch analysis |
| Requirements truncation | `JobDetailsActivity.kt` | 15,000 | Max job requirement chars sent to AI |

### 8.4 Scoring Thresholds

The color-coded verdict system uses these thresholds (defined in multiple activities):

| Score Range | Verdict | Color |
|:---|:---|:---|
| 80-100% | STRONG MATCH | Green (#059669) |
| 60-79% | MODERATE MATCH | Amber (#D97706) |
| 0-59% | WEAK MATCH | Red (#DC2626) |

In `CandidateAdapter.kt` and `HistoryAdapter.kt`, the thresholds are slightly different:
| Score Range | Badge |
|:---|:---|
| 75-100% | Strong |
| 50-74% | Moderate |
| 0-49% | Weak |

### 8.5 Changing the Package Name

1. In `app/build.gradle.kts`, change:
   ```kotlin
   namespace = "com.yourcompany.resumescreener"
   applicationId = "com.yourcompany.resumescreener"
   ```
2. In `AndroidManifest.xml`, update the `android:name` package references
3. Rename the Kotlin source directory structure to match
4. Re-download `google-services.json` from Firebase with the new package name
5. Clean and rebuild

---

## 9. Architecture Deep Dive

### 9.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    ANDROID APP                       │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Activities   │  │   Adapters   │  │  Layouts  │ │
│  │  (UI Layer)   │──│ (Binding)    │──│  (XML)    │ │
│  └──────┬───────┘  └──────────────┘  └───────────┘ │
│         │                                            │
│         ▼                                            │
│  ┌──────────────────────────────────────────────┐   │
│  │           Business Logic Layer                │   │
│  │  FirebaseService │ HistoryManager │ Exporter  │   │
│  └──────┬───────────────────────┬───────────────┘   │
│         │                       │                    │
│         ▼                       ▼                    │
│  ┌──────────────┐     ┌──────────────────┐          │
│  │  Retrofit     │     │  Local Storage   │          │
│  │  (LLM API)   │     │  (SharedPrefs,   │          │
│  │              │     │   JSON files)    │          │
│  └──────┬───────┘     └──────────────────┘          │
│         │                                            │
└─────────┼────────────────────────────────────────────┘
          │
          ▼
┌──────────────────┐     ┌─────────────────────┐
│   LLM Provider    │     │   Firebase Cloud     │
│ (Gemini/OpenAI/  │     │  (Firestore + Auth)  │
│  Cerebras/etc.)  │     │                     │
└──────────────────┘     └─────────────────────┘
```

### 9.2 Threading Model

All asynchronous operations use **Kotlin Coroutines** with `lifecycleScope`:

- **`Dispatchers.IO`**: Network calls, file I/O, Firestore operations
- **`Dispatchers.Main`**: UI updates, Toast messages, View visibility changes
- **`lifecycleScope.launch`**: Tied to Activity lifecycle, auto-cancels on destroy

### 9.3 Offline-First Strategy

```kotlin
// ResumeAIApplication.kt
val settings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
    .build()
FirebaseFirestore.getInstance().firestoreSettings = settings
```

- All Firestore reads first check local cache
- Writes are queued when offline and synced when connectivity returns
- Personal scan history is always available offline via local JSON

### 9.4 WakeLock Management

During batch analysis, the app must prevent the device from sleeping:

```kotlin
// WakeLockManager.kt
fun acquire() {
    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ResumeAI:Batch")
        .apply { setReferenceCounted(false); acquire(30 * 60 * 1000L) }
}
```

The WakeLock is:
- Acquired when batch analysis starts
- Released when analysis completes or Activity is destroyed
- Bound to the Activity lifecycle via `DefaultLifecycleObserver`

### 9.5 Caching Strategy

`FirebaseService` implements an in-memory cache with TTL:

```kotlin
private val cache = ConcurrentHashMap<String, Pair<Long, Any>>()
private const val TTL = 5 * 60 * 1000L  // 5 minutes
```

Cached endpoints:
- `getOrganizations()` → key: `"orgs"`
- `getJobOpenings(orgId)` → key: `"jobs_$orgId"`

Cache is invalidated on create/delete operations.

---

## 10. Data Models Reference

### 10.1 Candidate (Parcelable)

```kotlin
@Parcelize
data class Candidate(
    val id: String = "",
    val fileName: String = "",
    val orgId: String = "",
    val jobId: String = "",
    val resumeText: String = "",
    var status: String = "pending",         // "pending" | "analyzed" | "Error"
    var score: Int = 0,                     // 0-100 ATS match score
    var name: String = "Unknown",           // Extracted from resume
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var education: String = "",             // Brief summary
    var experience: String = "",            // Brief summary
    var summary: String = "",               // AI-generated fit assessment
    var hardSkillsMatched: List<String> = emptyList(),
    var hardSkillsMissing: List<String> = emptyList(),
    var softSkillsMatched: List<String> = emptyList(),
    var softSkillsMissing: List<String> = emptyList(),
    var formattingIssues: List<String> = emptyList(),
    val uploadedAt: String = ""             // Date string
) : Parcelable
```

### 10.2 Organization

```kotlin
data class Organization(
    @DocumentId val id: String = "",        // Auto-generated uppercase ID
    val name: String = "",                  // Human-readable name
    @ServerTimestamp val createdAt: Date? = null
)
```

### 10.3 JobOpening

```kotlin
data class JobOpening(
    @DocumentId val id: String = "",        // Auto-generated uppercase ID
    val orgId: String = "",                 // Parent organization
    val title: String = "",                 // Display title
    val description: String = "",           // Brief description
    val requirements: String = "",          // Full JD for AI matching
    @ServerTimestamp val postedDate: Date? = null,
    var cvCount: Int = 0,                   // Number of submissions
    var status: String = "open"             // "open" | "closed"
)
```

### 10.4 ScanRecord (Local)

```kotlin
data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String = "...",               // Formatted date string
    val fileName: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>
)
```

### 10.5 HistoryItem (UI Display)

```kotlin
data class HistoryItem(
    val id: String,
    val source: String,                     // "personal" | "batch"
    val fileName: String,
    val date: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>,
    val orgName: String? = null,            // Only for batch results
    val jobTitle: String? = null,           // Only for batch results
    val candidate: Candidate? = null        // Full data for batch results
)
```

### 10.6 AI Request/Response Models

```kotlin
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat = ResponseFormat("json_object"),
    val temperature: Double = 0.1,
    val max_tokens: Int = 2048
)

data class Message(val role: String, val content: String)
data class ResponseFormat(val type: String)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)
```

---

## 11. Firestore Database Schema

### 11.1 Collection Structure

```
Firestore Root
│
├── organizations/                          (Collection)
│   │
│   ├── {orgId}                             (Document)
│   │   ├── id: String                      "ACME_CORP"
│   │   ├── name: String                    "Acme Corporation"
│   │   └── createdAt: Timestamp            Server-generated
│   │   │
│   │   └── job_openings/                   (Subcollection)
│   │       │
│   │       ├── {jobId}                     (Document)
│   │       │   ├── id: String              "SENIOR_DEV"
│   │       │   ├── orgId: String           "ACME_CORP"
│   │       │   ├── title: String           "Senior Developer"
│   │       │   ├── description: String     "Brief description..."
│   │       │   ├── requirements: String    "Full JD text..."
│   │       │   ├── postedDate: Timestamp   Server-generated
│   │       │   ├── cvCount: Integer        0
│   │       │   └── status: String          "open"
│   │       │   │
│   │       │   └── candidates/             (Subcollection)
│   │       │       │
│   │       │       └── {candidateId}       (Document)
│   │       │           ├── id: String      "portal_1721234567890"
│   │       │           ├── fileName: String "resume.pdf"
│   │       │           ├── orgId: String   "ACME_CORP"
│   │       │           ├── jobId: String   "SENIOR_DEV"
│   │       │           ├── resumeText: String (truncated)
│   │       │           ├── status: String  "pending" | "analyzed"
│   │       │           ├── score: Integer  0-100
│   │       │           ├── name: String
│   │       │           ├── email: String
│   │       │           ├── phone: String
│   │       │           ├── address: String
│   │       │           ├── education: String
│   │       │           ├── experience: String
│   │       │           ├── summary: String
│   │       │           ├── hardSkillsMatched: Array<String>
│   │       │           ├── hardSkillsMissing: Array<String>
│   │       │           ├── softSkillsMatched: Array<String>
│   │       │           ├── softSkillsMissing: Array<String>
│   │       │           ├── formattingIssues: Array<String>
│   │       │           └── uploadedAt: String
│   │       │
│   │       └── {jobId_2} ...
│   │
│   └── {orgId_2} ...
│
└── (Personal scans stored under device-specific orgId)
    organizations/
      └── PERSONAL_A1B2C3D4/
          └── job_openings/
              └── QUICK_SCAN/
                  └── candidates/
                      └── personal_1721234567890
```

### 11.2 ID Generation Rules

| Entity | ID Format | Example |
|:---|:---|:---|
| Organization | `name.uppercase().replace(non-alphanumeric, "_")` | `ACME_CORPORATION` |
| Job Opening | `title.uppercase().replace(non-alphanumeric, "_")` | `SENIOR_ANDROID_DEV` |
| Portal Candidate | `portal_{timestamp}` | `portal_1721234567890` |
| Personal Candidate | `personal_{timestamp}` | `personal_1721234567890` |
| Personal Org | `PERSONAL_{androidId.take(8).uppercase()}` | `PERSONAL_A1B2C3D4` |

---

## 12. AI Prompt Engineering

### 12.1 System Prompt

```
You are an expert ATS analyzer. Output only valid JSON.
```

### 12.2 User Prompt Template

```
You are an expert ATS (Applicant Tracking System) analyzer.

JOB REQUIREMENTS:
{job_requirements_text}

CANDIDATE RESUME:
{candidate_resume_text}

Compare the resume against the job requirements and output ONLY JSON:
{
  "score": <int 0-100 based on how well resume matches job requirements>,
  "name": "<extracted from resume>",
  "email": "<extracted>",
  "phone": "<extracted>",
  "address": "<extracted>",
  "education": "<brief summary>",
  "experience": "<brief summary>",
  "hard_skills_matched": [<skills in resume that match job requirements>],
  "hard_skills_missing": [<skills in job requirements not found in resume>],
  "soft_skills_matched": [<soft skills in resume that match>],
  "soft_skills_missing": [<soft skills in requirements not found>],
  "formatting_issues": [<ATS formatting problems>],
  "summary": "<1-2 sentence assessment of fit>"
}
```

### 12.3 JSON Extraction Logic

The app extracts valid JSON from potentially noisy LLM responses using a brace-counting algorithm:

```kotlin
private fun extractJson(s: String): String? {
    val start = s.indexOf('{')
    if (start < 0) return null
    var depth = 0; var inStr = false; var esc = false
    for (i in start until s.length) {
        val c = s[i]
        if (esc) { esc = false; continue }
        if (c == '\\') { esc = true; continue }
        if (c == '"') { inStr = !inStr; continue }
        if (!inStr) {
            if (c == '{') depth++
            else if (c == '}') { depth--; if (depth == 0) return s.substring(start, i + 1) }
        }
    }
    return null
}
```

This handles cases where the LLM wraps JSON in markdown code blocks or adds explanatory text.

### 12.4 Request Configuration

```kotlin
ChatRequest(
    model = MODEL_ID,
    messages = listOf(systemMessage, userMessage),
    response_format = ResponseFormat("json_object"),  // Enforces JSON output
    temperature = 0.1,                                // Low randomness for consistency
    max_tokens = 2048                                 // Sufficient for structured output
)
```

---

## 13. Security Model

### 13.1 Current Security Measures

| Layer | Mechanism | Strength |
|:---|:---|:---|
| **HR Access** | 4-6 digit PIN in SharedPreferences | ⚠️ Low (device-local only) |
| **Org Ownership** | Android ID hashed in SharedPreferences | ⚠️ Low (resets on factory reset) |
| **API Key** | BuildConfig from local.properties | ✅ Not in source control |
| **Firestore** | Anonymous Auth + Security Rules | ⚠️ Medium (any anonymous user can read/write) |
| **Data Transit** | HTTPS (Firebase + API) | ✅ Strong |
| **Resume Storage** | In-memory processing, not persisted to disk | ✅ Privacy-first |

### 13.2 Security Recommendations for Production

1. **Replace PIN with Firebase Auth email/password** or SSO
2. **Implement Firestore Custom Claims** for role-based access (admin vs applicant)
3. **Use EncryptedSharedPreferences** for any local secrets
4. **Add API key rotation** and server-side proxy for LLM calls
5. **Implement field-level Firestore rules** to prevent candidates from reading other candidates' data
6. **Add input sanitization** for org/job IDs to prevent injection

---

## 14. Activity & Screen Reference

### 14.1 SplashActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_splash.xml` |
| **Theme** | `@style/Theme.App.Starting` |
| **Duration** | 1500ms |
| **Navigates to** | `RoleSelectionActivity` |
| **Features** | Fade-in logo, app name, tagline, progress indicator |

### 14.2 RoleSelectionActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_role_selection.xml` |
| **Options** | "I'm an Applicant" → `ApplicantPortalActivity`, "I'm HR" → `AdminActivity` |
| **Orientation** | Handles config changes: `orientation|screenSize|screenLayout|keyboardHidden` |

### 14.3 MainActivity (Quick Scan)

| Property | Value |
|:---|:---|
| **Layout** | `activity_main.xml` |
| **Menu** | `main_menu.xml` (History, Submit to HR, HR Portal) |
| **Steps** | 3-step wizard: Upload → Describe → Analyze |
| **File Types** | PDF, DOCX, DOC |
| **API Calls** | Single LLM call per analysis |
| **Output** | In-app results + Excel export + Firestore save |
| **Applicant Mode** | Hides HR menu items when launched from Applicant Portal |

### 14.4 ApplicantPortalActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_applicant_portal.xml` |
| **File Types** | PDF, DOCX only (rejects legacy DOC) |
| **Max File Size** | 10MB |
| **Form Fields** | Org Code, Job ID, Full Name, Email, Resume |
| **Validation** | Real-time, all fields required |
| **Quick Actions** | "Check My Resume" → MainActivity, "My History" → HistoryActivity |

### 14.5 AdminActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_admin.xml` |
| **Auth** | PIN dialog on entry |
| **Features** | Create/Delete organizations, navigate to jobs |
| **Filtering** | Only shows orgs owned by current device |

### 14.6 JobDetailsActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_job_details.xml` |
| **Features** | Create jobs, trigger batch analysis, view results, close jobs |
| **Batch Analysis** | WakeLock + progress dialog + sequential API calls |
| **Job Requirements** | Minimum 50 characters for AI accuracy |

### 14.7 JobResultsActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_job_results.xml` |
| **Data Source** | Intent extras (fresh) or Firestore (cached) |
| **Stats** | Total, Average Score, Strong/Moderate/Weak counts |
| **Export** | Excel (.xlsx) via SAF file picker |

### 14.8 CandidateReportActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_candidate_report.xml` |
| **Data Source** | Parcelable Candidate via Intent |
| **Sections** | Score, Verdict, Personal Info, Experience, Education, Hard Skills, Soft Skills, Formatting Issues |
| **Print** | HTML → WebView → Android PrintManager |

### 14.9 HistoryActivity

| Property | Value |
|:---|:---|
| **Layout** | `activity_history.xml` |
| **Menu** | `history_menu.xml` (Clear action) |
| **Data Sources** | Local JSON (personal) + Firestore collection group query (batch) |
| **Filtering** | Shows HR batch scans + current device's personal scans |
| **Sorting** | Score descending, then date descending |

---

## 15. Adapter & UI Component Reference

### 15.1 CandidateAdapter

- **Layout:** `item_candidate_result.xml`
- **Data:** `List<Candidate>`
- **Displays:** Name, file name, score with progress bar, summary, email, badge (Strong/Moderate/Weak)
- **Click:** Opens `CandidateReportActivity`

### 15.2 HistoryAdapter

- **Layout:** `item_history.xml`
- **Data:** `List<HistoryItem>`
- **Displays:** Source badge (PERSONAL/HR BATCH), mini score ring, file name, date, summary, top matched keywords
- **Features:** Dynamic ring color based on score, reflection-based indicator color setting

### 15.3 JobOpeningAdapter

- **Layout:** `item_job_opening.xml`
- **Data:** `List<JobOpening>`
- **Displays:** Title, ID, status badge (OPEN/CLOSED), posted date, CV count, description
- **Actions:** Analyze, Results, Close Job (disabled when closed)

### 15.4 OrganizationAdapter

- **Layout:** `item_organization.xml`
- **Data:** `List<Organization>`
- **Displays:** Name, ID, "Manage jobs →" action text
- **Actions:** Click to open jobs, Delete button

---

## 16. Utility & Service Reference

### 16.1 FirebaseService (Singleton Object)

| Method | Description | Cache |
|:---|:---|:---|
| `getOrganizations()` | Fetches all orgs | ✅ 5min TTL |
| `createOrganization(name)` | Creates org doc, returns Result<String> | Invalidates |
| `deleteOrganization(orgId)` | Recursively deletes org, jobs, candidates | Invalidates |
| `getJobOpenings(orgId)` | Fetches jobs for org | ✅ 5min TTL |
| `createJobOpening(...)` | Creates job doc with requirements | Invalidates |
| `getCandidates(orgId, jobId)` | Fetches all candidates with error-tolerant deserialization | ❌ |
| `updateCandidate(c)` | Overwrites candidate doc | ❌ |
| `updateJobStatus(...)` | Updates job status field | Invalidates |
| `deleteJobCandidates(...)` | Deletes all candidates for a job | ❌ |
| `deleteJobOpening(...)` | Deletes job + all candidates | Invalidates |
| `getAllAnalyzedCandidates()` | Collection group query for all analyzed candidates | ❌ |
| `getOrganizationName(orgId)` | Helper for history display | ❌ |
| `getJobTitle(orgId, jobId)` | Helper for history display | ❌ |

### 16.2 AdminAuthManager (Singleton Object)

| Method | Description |
|:---|:---|
| `isPinSet(context)` | Checks if PIN has been created |
| `setPin(context, pin)` | Stores PIN in SharedPreferences |
| `verifyPin(context, pin)` | Validates PIN against stored value |
| `resetPin(context)` | Removes PIN and resets state |
| `getOwnedOrgIds(context)` | Returns Set<String> of owned org IDs |
| `addOwnedOrg(context, orgId)` | Adds org ID to ownership set |
| `removeOwnedOrg(context, orgId)` | Removes org ID from ownership set |
| `ownsOrg(context, orgId)` | Checks if device owns specific org |

### 16.3 HistoryManager

| Method | Description |
|:---|:---|
| `saveScan(record)` | Prepends to local JSON, max 30 records |
| `getHistory()` | Deserializes JSON to List<ScanRecord> |
| `clearHistory()` | Deletes local history file |

### 16.4 ExcelExporter

Generates `.xlsx` files using Apache POI with:
- Bold header row with blue background
- Columns: Rank, Name, Score, Email, Phone, Address, Matched Hard Skills, Missing Hard Skills, Matched Soft Skills, Missing Soft Skills, Experience, Education, AI Summary, Formatting Issues
- Sorted by score descending
- Uniform column width (6000 units)

### 16.5 DeviceIdHelper

Returns a stable device identifier: `"PERSONAL_" + ANDROID_ID.take(8).uppercase()`

Used to:
- Namespace personal scans in Firestore
- Filter history to show only current device's personal data

### 16.6 WakeLockManager

Lifecycle-aware power management:
- Acquires `PARTIAL_WAKE_LOCK` + `FLAG_KEEP_SCREEN_ON`
- 30-minute timeout safety net
- Auto-releases on Activity destroy

---

## 17. Handover & Future Migration Guide

### 17.1 Priority 1: Move Document Parsing to Backend

**Why:** PDFBox and Apache POI add ~15MB to APK size and consume significant memory during batch operations.

**How:**
1. Add Firebase Storage or AWS S3 to the project
2. In `ApplicantPortalActivity.submitApplication()`, upload the raw file bytes instead of extracting text
3. Create a Cloud Function / backend endpoint that:
   - Downloads the file
   - Extracts text using `PyMuPDF` (Python) or `pdf-parse` (Node.js)
   - Calls the LLM API
   - Writes results back to Firestore
4. Remove PDFBox and POI dependencies from `build.gradle.kts`
5. Remove `extractText()`, `cleanTextForAts()` methods from Activities

### 17.2 Priority 2: Replace Anonymous Auth with User Accounts

**Why:** Anonymous auth provides no identity persistence across devices.

**How:**
1. Enable Email/Password auth in Firebase Console
2. Replace `auth.signInAnonymously()` with `auth.createUserWithEmailAndPassword()` and `auth.signInWithEmailAndPassword()`
3. Add a login/registration screen
4. Store user UID and link org ownership to user accounts in Firestore instead of SharedPreferences
5. Update Firestore security rules to check `request.auth.uid` against org owner fields

### 17.3 Priority 3: Custom REST API Backend

**Why:** Direct Firestore access from mobile clients limits scalability and security.

**How:**
1. Build a REST API (Node.js/Express, Python/FastAPI, or Java/Spring Boot)
2. Create endpoints:
   ```
   POST   /api/auth/login
   POST   /api/auth/register
   GET    /api/organizations
   POST   /api/organizations
   DELETE /api/organizations/:id
   GET    /api/organizations/:orgId/jobs
   POST   /api/organizations/:orgId/jobs
   GET    /api/organizations/:orgId/jobs/:jobId/candidates
   POST   /api/organizations/:orgId/jobs/:jobId/candidates
   POST   /api/analyze                    # Triggers batch analysis server-side
   GET    /api/history
   ```
3. Replace `FirebaseService.kt` with a Retrofit-based `ApiService.kt`
4. Replace Firebase Auth with JWT tokens stored in `EncryptedSharedPreferences`
5. Move the LLM API call to the backend to protect the API key

### 17.4 Priority 4: Async Batch Processing

**Why:** Sequential on-device analysis blocks the UI and drains battery.

**How:**
1. Implement a job queue (AWS SQS, RabbitMQ, or Firebase Cloud Tasks)
2. When HR clicks "Analyze", create a batch job record
3. Backend workers process candidates in parallel
4. Mobile app polls for status or listens to Firestore real-time updates
5. Push notification when batch completes

---

## 18. Known Limitations & Scaling Notes

### 18.1 On-Device Processing Limits
- Batch analysis of 50+ resumes will cause significant battery drain
- Devices with <3GB RAM may crash with `OutOfMemoryError` during large PDF extraction
- Apache POI has known memory issues with complex .docx files containing embedded images

### 18.2 API Endpoint Naming Anomaly
The codebase defines the Retrofit interface as `CerebrasApi` and the key as `CEREBRAS_API_KEY`, but `BASE_URL` points to Google's Gemini proxy. These names are historical and should be updated to reflect the actual provider being used.

### 18.3 Local PIN Storage
- PIN is stored in plaintext SharedPreferences
- Factory reset or app uninstall clears all PIN data and org ownership
- No PIN recovery mechanism exists
- Multiple HR users on the same device share the same PIN

### 18.4 ProGuard / R8 Minification
Currently disabled (`isMinifyEnabled = false`). If enabled:
- Apache POI requires extensive keep rules for reflection-based XML parsing
- PDFBox requires keep rules for font and encoding classes
- Gson requires keep rules for all data model classes
- Failure to add these rules will cause runtime crashes

### 18.5 Firestore Costs
- Each candidate analysis triggers 1 read (fetch) + 1 write (update) per candidate
- Collection group queries for history count as reads across all subcollections
- Monitor usage in Firebase Console to avoid unexpected billing

---

## 19. Troubleshooting & FAQ

### Q: App crashes on launch with "FirebaseApp is not initialized"
**A:** Ensure `google-services.json` is placed in the `app/` directory and the Google Services plugin is applied in `build.gradle.kts`.

### Q: API returns 401 Unauthorized
**A:** Check that `CEREBRAS_API_KEY` in `local.properties` is correct and matches the provider in `BASE_URL`. Rebuild the project after changing the key (BuildConfig is generated at compile time).

### Q: PDF text extraction returns empty/garbled text
**A:** The PDF is likely image-based (scanned). The app will show a quality warning. Use a text-based PDF or convert using OCR tools before uploading.

### Q: Batch analysis stops midway
**A:** Check for:
1. API rate limits (429 errors) — increase `delay()` between calls
2. Device sleep — ensure WakeLock is functioning
3. Network timeout — increase OkHttp timeout values
4. Memory pressure — reduce `MAX_TEXT_CHARS`

### Q: Excel export fails
**A:** Ensure the user grants storage permission when the SAF file picker appears. Check that Apache POI dependencies are not being stripped by ProGuard.

### Q: History shows duplicates
**A:** Both local JSON and Firestore may contain the same scan. The current implementation does not deduplicate across sources.

### Q: App shows "Organization not found" when submitting
**A:** The org code and job ID must exactly match the Firestore document IDs (uppercase, underscores). Verify in Firebase Console → Firestore.

---

## 20. Deployment & Release Checklist

### Pre-Release

- [ ] Update `versionCode` and `versionName` in `build.gradle.kts`
- [ ] Set `isMinifyEnabled = true` and test with ProGuard rules
- [ ] Verify `google-services.json` is for the production Firebase project
- [ ] Test all flows on minimum API level (28) and latest API level (35)
- [ ] Verify Crashlytics is receiving reports
- [ ] Remove all `Log.d()` debug statements or wrap in `BuildConfig.DEBUG` checks
- [ ] Test with poor network conditions (Airplane mode toggle)
- [ ] Verify API key is not hardcoded anywhere in source files

### Release Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### App Bundle (for Play Store)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Signing

Configure signing in `build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = "release"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

---

## 21. Contributing Guidelines

### Branch Naming
- `feature/add-ocr-support`
- `fix/batch-analysis-crash`
- `refactor/move-parsing-backend`

### Commit Messages
Follow conventional commits:
```
feat: add Excel export for personal scans
fix: prevent WakeLock leak on Activity destroy
docs: update Firestore security rules
refactor: extract API config to separate file
```

### Code Style
- Use Kotlin idioms (data classes, extension functions, scope functions)
- Follow Material Design 3 color system (teal/slate palette)
- All UI text should be in `strings.xml` for localization readiness
- Coroutines should always use `lifecycleScope` or `viewModelScope`

---

## 22. License

```
Copyright (c) 2026 [Your Company Name]

This software and associated documentation files are proprietary and confidential.
Unauthorized copying, distribution, or modification is strictly prohibited
without prior written permission from the copyright holder.
```

---

> **For questions, issues, or handover support, contact:**  
> 📧 [your-email@company.com]  
> 📱 [Your Name / Lead Developer]  
> 🏢 [Company Name]
```
