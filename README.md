markdown
# 🚀 ResumeAI Enterprise: ATS Resume Screener & Applicant Portal

> **Version:** 1.0.0  
> **Platform:** Android (API 28+)  
> **Language:** Kotlin  
> **Last Updated:** July 2026  
> **Status:** Production-Ready MVP

---

ResumeAI Enterprise is a comprehensive, dual-sided Android application designed to bridge the gap between job applicants and HR departments. It leverages Large Language Models (LLMs) to act as an intelligent Applicant Tracking System (ATS), providing deep semantic analysis of resumes against specific job descriptions.

---

## 📑 Table of Contents

1. [Overview](#1-overview)
2. [Key Features](#2-key-features)
3. [User Roles & Detailed Workflows](#3-user-roles--detailed-workflows)
4. [Screen-by-Screen Breakdown](#4-screen-by-screen-breakdown)
5. [Technology Stack & Dependencies](#5-technology-stack--dependencies)
6. [Project Structure](#6-project-structure)
7. [Data Models & Schema](#7-data-models--schema)
8. [AI / LLM Integration](#8-ai--llm-integration)
9. [Firebase Backend Architecture](#9-firebase-backend-architecture)
10. [Local Storage & Security](#10-local-storage--security)
11. [File Parsing Pipeline](#11-file-parsing-pipeline)
12. [Batch Processing Deep Dive](#12-batch-processing-deep-dive)
13. [Prerequisites & Complete Setup Guide](#13-prerequisites--complete-setup-guide)
14. [Configuration & Customization](#14-configuration--customization)
15. [Build, Sign & Deploy](#15-build-sign--deploy)
16. [Error Handling & Resilience](#16-error-handling--resilience)
17. [Testing Strategy](#17-testing-strategy)
18. [Troubleshooting & FAQ](#18-troubleshooting--faq)
19. [Handover & Future Migration Guide](#19-handover--future-migration-guide)
20. [Known Limitations & Scaling Notes](#20-known-limitations--scaling-notes)
21. [Contributing Guidelines](#21-contributing-guidelines)
22. [Changelog](#22-changelog)
23. [License](#23-license)
24. [Contact & Support](#24-contact--support)

---

## 1. Overview

The application serves two primary functions:

1. **For Applicants:** A personal ATS scanner to test resumes against job descriptions, identifying missing keywords, formatting issues, and overall match percentage before applying.
2. **For HR/Recruiters:** A secure, PIN-protected portal to manage organizations, post jobs, receive applicant submissions, run batch AI analysis on candidate pools, and export ranked results to Excel.

### Why This App Exists

Traditional ATS systems reject up to **75% of resumes** before a human ever sees them, often due to missing keywords or poor formatting. ResumeAI gives applicants the ability to pre-screen their resumes against real job descriptions using the same semantic analysis that enterprise ATS platforms use. Simultaneously, it gives HR teams a lightweight, mobile-first tool to manage candidate pipelines without expensive SaaS subscriptions.

---

## 2. Key Features

| Feature | Description |
| :--- | :--- |
| **AI-Powered Semantic Matching** | Goes beyond keyword counting; understands context, soft skills, and experience alignment using LLMs. |
| **Multi-Format Parsing** | Native on-device extraction for `.pdf`, `.docx`, and legacy `.doc` files via PDFBox and Apache POI. |
| **Batch Processing** | HR can analyze dozens of resumes sequentially with auto-retry logic and WakeLock management. |
| **Unified History** | Syncs personal scans and HR batch results into a single, searchable timeline. |
| **Enterprise Export** | One-click generation of formatted `.xlsx` Excel reports with ranked candidates. |
| **Offline Resilience** | Firebase offline persistence ensures the app functions in low-connectivity environments. |
| **Privacy-First** | Resumes are processed in-memory, truncated to prevent token overflow, and never permanently stored on-device. |
| **Role-Based Navigation** | Splash screen routes to Applicant or HR flows via a Role Selection screen. |
| **Candidate Reports** | Detailed per-candidate breakdown with matched/missing skills, formatting issues, and print support. |
| **Crash Reporting** | Firebase Crashlytics integration for production error monitoring. |

---

## 3. User Roles & Detailed Workflows

### 3.1 🧑‍💼 Applicant / Personal User

#### Flow A: Quick Resume Scan

Splash Screen
  → Role Selection ("I'm an Applicant")
    → Applicant Portal
      → "Check My Resume" card
        → MainActivity (Quick Scan Mode)
          → Step 1: Upload Resume (PDF/DOCX/DOC)
          → Step 2: Paste Job Description (min 20 chars)
          → Step 3: Tap "Run ATS Analysis"
            → [Text Extraction] → [AI Prompt] → [JSON Parse]
          → View Results:
            • ATS Score (0-100%)
            • Verdict (Strong / Moderate / Weak)
            • Matched & Missing Hard Skills
            • Matched & Missing Soft Skills
            • ATS Formatting Issues
            • AI Summary
          → Export to Excel OR Screen Another


#### Flow B: Submit Resume to HR

Splash Screen
  → Role Selection ("I'm an Applicant")
    → Applicant Portal
      → Enter Organization Code (from HR)
      → Enter Job Opening ID (from HR)
      → Enter Full Name & Email
      → Upload Resume (PDF/DOCX only, max 10MB)
      → Tap "Submit Application"
        → [Firebase Auth] → [Validate Org/Job exists]
        → [Save Candidate to Firestore]
        → [Increment CV Count]
      → Success Toast → Form Reset
```

### 3.2 🏢 HR / Recruiter (Admin)

```
Splash Screen
  → Role Selection ("I'm HR / Recruiter")
    → AdminActivity
      → PIN Gate:
        • First Time: Create 4-6 digit PIN
        • Returning: Enter existing PIN
      → View Owned Organizations
      → Create New Organization
        → Generates unique Org ID (e.g., ACME_CORP)
      → Tap Organization → JobDetailsActivity
        → View Job Openings
        → Create New Job Opening
          → Title, Description, Full Requirements (min 50 chars)
        → For each job:
          • "Analyze" → Batch AI analysis of all submitted CVs
          • "Results" → View ranked candidate list
          • "Close Job" → Delete job and all candidate data
      → JobResultsActivity
        → Stats Dashboard (Total, Avg Score, Strong, Moderate, Weak)
        → Ranked Candidate List
        → Tap Candidate → CandidateReportActivity (full breakdown)
        → Export All to Excel (.xlsx)
```

### 3.3 📜 History Flow (Both Roles)
```
Any Screen → Menu → "History"
  → HistoryActivity
    → Loads local personal scans (from device storage)
    → Loads cloud batch results (from Firestore collectionGroup)
    → Merges, deduplicates, sorts by score
    → Tap batch result → CandidateReportActivity
    → Tap personal result → Toast with summary
    → Menu → "Clear" → Wipe local history


## 4. Screen-by-Screen Breakdown

| Screen | Activity Class | Layout File | Purpose |
| :--- | :--- | :--- | :--- |
| **Splash** | `SplashActivity` | `activity_splash.xml` | Branded loading screen with fade-in animations. Holds for 1.5s then navigates to Role Selection. |
| **Role Selection** | `RoleSelectionActivity` | `activity_role_selection.xml` | Two-card layout letting user choose Applicant or HR path. |
| **Main Scanner** | `MainActivity` | `activity_main.xml` | 3-step wizard: Upload → Describe → Analyze. Displays full ATS results with skill chips and score ring. |
| **Applicant Portal** | `ApplicantPortalActivity` | `activity_applicant_portal.xml` | Submission form with Org Code, Job ID, personal info, and resume upload. Includes quick-action cards. |
| **HR Admin** | `AdminActivity` | `activity_admin.xml` | PIN-gated org management. Create/delete organizations. |
| **Job Details** | `JobDetailsActivity` | `activity_job_details.xml` | Manage job openings per org. Trigger batch analysis. |
| **Job Results** | `JobResultsActivity` | `activity_job_results.xml` | Stats dashboard + ranked candidate list + Excel export. |
| **Candidate Report** | `CandidateReportActivity` | `activity_candidate_report.xml` | Full candidate breakdown with print support via WebView. |
| **History** | `HistoryActivity` | `activity_history.xml` | Unified timeline of all scans (personal + batch). |

---

## 5. Technology Stack & Dependencies

### Core Stack

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | 1.9+ | Primary language |
| **Min SDK** | Android API 28 | — | Android 9.0 Pie |
| **Target SDK** | Android API 35 | — | Android 15 |
| **UI** | Material Design 3 | 1.12.0 | Components, theming |
| **View Binding** | AndroidX | — | Type-safe view access |
| **Async** | Kotlin Coroutines | 1.8.1 | Non-blocking operations |
| **Lifecycle** | Lifecycle Runtime KTX | 2.8.4 | Coroutine scoping |

### Networking & AI

| Library | Version | Purpose |
| :--- | :--- | :--- |
| Retrofit2 | 2.11.0 | HTTP client for LLM API |
| OkHttp3 | 4.12.0 | Connection pooling, interceptors, retry logic |
| Gson | (via Retrofit) | JSON serialization/deserialization |

### Firebase

| Library | Version | Purpose |
| :--- | :--- | :--- |
| Firebase BOM | 33.7.0 | Bill of Materials for version alignment |
| Firebase Firestore | (BOM) | NoSQL document database |
| Firebase Auth | (BOM) | Anonymous authentication |
| Firebase Crashlytics | (BOM) | Crash reporting |
| Firebase Analytics | (BOM) | Usage analytics |
| Crashlytics Gradle Plugin | 3.0.2 | Build-time symbol upload |

### Document Parsing

| Library | Version | Purpose |
| :--- | :--- | :--- |
| PDFBox (TomRoush) | 2.0.27.0 | PDF text extraction on Android |
| Apache POI | 5.3.0 | DOCX/DOC text extraction |
| POI Scratchpad | 5.3.0 | Legacy .doc format support |
| XMLBeans | 5.2.1 | POI transitive dependency |
| Commons IO | 2.16.1 | POI transitive dependency |
| Commons Codec | 1.17.0 | POI transitive dependency |
| Commons Collections4 | 4.4 | POI transitive dependency |
| Commons Compress | 1.26.2 | POI transitive dependency |
| Log4j API | 2.23.1 | POI transitive dependency |

### UI Extras

| Library | Version | Purpose |
| :--- | :--- | :--- |
| FlexboxLayout | 3.0.0 | Wrapping chip/tag layouts for skills |
| RecyclerView | 1.3.2 | List rendering |
| Core SplashScreen | 1.0.1 | Splash screen API |

--

## 6. Project Structure

app/
├── src/main/
│   ├── java/com/example/airesumescreener/
│   │   ├── ResumeAIApplication.kt          # Application class, Firebase init
│   │   ├── SplashActivity.kt               # Entry point, animated splash
│   │   ├── RoleSelectionActivity.kt        # Applicant vs HR chooser
│   │   ├── MainActivity.kt                 # Quick scan (3-step wizard)
│   │   ├── ApplicantPortalActivity.kt      # Submit resume to HR
│   │   ├── AdminActivity.kt                # HR org management (PIN-gated)
│   │   ├── AdminAuthManager.kt             # PIN & org ownership (SharedPreferences)
│   │   ├── JobDetailsActivity.kt           # Job openings + batch analysis
│   │   ├── JobResultsActivity.kt           # Ranked results + Excel export
│   │   ├── CandidateReportActivity.kt      # Full candidate report + print
│   │   ├── HistoryActivity.kt              # Unified scan history
│   │   ├── HistoryManager.kt               # Local JSON history persistence
│   │   ├── FirebaseService.kt              # All Firestore CRUD operations
│   │   ├── DeviceIdHelper.kt               # Device-unique ID generator
│   │   ├── ExcelExporter.kt                # Apache POI .xlsx generation
│   │   ├── WakeLockManager.kt              # CPU/screen wake lock for batch
│   │   ├── Models.kt                       # All data classes (Candidate, Org, Job, etc.)
│   │   ├── CandidateAdapter.kt             # RecyclerView adapter for candidates
│   │   ├── HistoryAdapter.kt               # RecyclerView adapter for history
│   │   ├── JobOpeningAdapter.kt            # RecyclerView adapter for jobs
│   │   └── OrganizationAdapter.kt          # RecyclerView adapter for orgs
│   │
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_splash.xml
│   │   │   ├── activity_role_selection.xml
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_applicant_portal.xml
│   │   │   ├── activity_admin.xml
│   │   │   ├── activity_job_details.xml
│   │   │   ├── activity_job_results.xml
│   │   │   ├── activity_candidate_report.xml
│   │   │   ├── activity_history.xml
│   │   │   ├── item_candidate_result.xml
│   │   │   ├── item_history.xml
│   │   │   ├── item_job_opening.xml
│   │   │   ├── item_organization.xml
│   │   │   └── dialog_batch_progress.xml
│   │   ├── menu/
│   │   │   ├── main_menu.xml
│   │   │   └── history_menu.xml
│   │   ├── drawable/
│   │   │   ├── app_logo.png
│   │   │   ├── gradient_background.xml
│   │   │   ├── circle_bg.xml
│   │   │   └── baseline_cloud_upload_24.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── xml/
│   │       ├── file_paths.xml
│   │       ├── backup_rules.xml
│   │       └── data_extraction_rules.xml
│   │
│   └── AndroidManifest.xml
│
├── build.gradle.kts                        # App-level build config
├── google-services.json                    # Firebase config (NOT in version control)
└── proguard-rules.pro                      # ProGuard rules (for future minification)

build.gradle.kts                            # Root-level build config
settings.gradle.kts                         # Module settings
local.properties                            # API keys (NOT in version control)
gradle.properties                           # Gradle JVM args
```

---

## 7. Data Models & Schema

### 7.1 Candidate (Primary Model)
```kotlin
@Parcelize
data class Candidate(
    val id: String = "",              // Unique ID (e.g., "portal_1721234567890")
    val fileName: String = "",        // Original resume filename
    val orgId: String = "",           // Parent organization ID
    val jobId: String = "",           // Parent job opening ID
    val resumeText: String = "",      // Extracted text (truncated to 15K-20K chars)
    var status: String = "pending",   // "pending" | "analyzed" | "Error"
    var score: Int = 0,               // ATS match score 0-100
    var name: String = "Unknown",     // Extracted from resume by AI
    var email: String = "",           // Extracted from resume by AI
    var phone: String = "",           // Extracted from resume by AI
    var address: String = "",         // Extracted from resume by AI
    var education: String = "",       // AI-generated summary
    var experience: String = "",      // AI-generated summary
    var summary: String = "",         // 1-2 sentence AI assessment
    var hardSkillsMatched: List<String> = emptyList(),
    var hardSkillsMissing: List<String> = emptyList(),
    var softSkillsMatched: List<String> = emptyList(),
    var softSkillsMissing: List<String> = emptyList(),
    var formattingIssues: List<String> = emptyList(),
    val uploadedAt: String = ""       // ISO timestamp string
) : Parcelable
```

### 7.2 Organization
```kotlin
data class Organization(
    @DocumentId val id: String = "",      // e.g., "ACME_CORP"
    val name: String = "",                // e.g., "Acme Corporation"
    @ServerTimestamp val createdAt: Date? = null
)
```

### 7.3 JobOpening
```kotlin
data class JobOpening(
    @DocumentId val id: String = "",      // e.g., "SENIOR_ANDROID_DEV"
    val orgId: String = "",               // Parent org ID
    val title: String = "",               // Display title
    val description: String = "",         // Brief description for UI
    val requirements: String = "",        // Full JD text for AI matching
    @ServerTimestamp val postedDate: Date? = null,
    var cvCount: Int = 0,                 // Number of submitted resumes
    var status: String = "open"           // "open" | "closed"
)
```

### 7.4 ScanRecord (Local History)
```kotlin
data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String = "Jul 19, 2026 14:30",
    val fileName: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>
)
```

### 7.5 HistoryItem (Unified Display)
```kotlin
data class HistoryItem(
    val id: String,
    val source: String,              // "personal" | "batch"
    val fileName: String,
    val date: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>,
    val orgName: String? = null,     // Only for batch results
    val jobTitle: String? = null,    // Only for batch results
    val candidate: Candidate? = null // Full data for batch results
)
```

### 7.6 AI API Models
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

## 8. AI / LLM Integration

### 8.1 API Configuration
| Setting | Current Value | Location |
| :--- | :--- | :--- |
| **Base URL** | `https://generativelanguage.googleapis.com/v1beta/openai/` | `MainActivity.kt` (top-level const) |
| **Model ID** | `gemini-3.5-flash` | `MainActivity.kt` (top-level const) |
| **API Key** | Loaded from `local.properties` | `BuildConfig.CEREBRAS_API_KEY` |
| **Auth Header** | `Bearer <API_KEY>` | Passed in Retrofit call |
| **Temperature** | `0.1` | Low for deterministic JSON output |
| **Max Tokens** | `2048` | Sufficient for structured JSON response |
| **Response Format** | `json_object` | Forces LLM to return valid JSON |

### 8.2 Prompt Template (Quick Scan)
```text
You are an expert ATS (Applicant Tracking System) analyzer.

JOB REQUIREMENTS:
{cleanedJobDescription}

CANDIDATE RESUME:
{cleanedResumeText}

Compare the resume against the job requirements and output ONLY JSON:
{
  "score": <int 0-100>,
  "name": "<extracted>",
  "email": "<extracted>",
  "phone": "<extracted>",
  "address": "<extracted>",
  "education": "<brief summary>",
  "experience": "<brief summary>",
  "hard_skills_matched": [],
  "hard_skills_missing": [],
  "soft_skills_matched": [],
  "soft_skills_missing": [],
  "formatting_issues": [],
  "summary": "<1-2 sentence assessment>"
}
```

### 8.3 Prompt Template (Batch HR Analysis)
The batch prompt is identical in structure but uses the **full job requirements** stored in Firestore (truncated to 15,000 chars) instead of a user-pasted description.

### 8.4 JSON Extraction
Since LLMs sometimes wrap JSON in markdown code blocks or add preamble text, the app uses a custom `extractJsonObject()` function that:
1. Finds the first `{` character
2. Tracks brace depth while respecting string literals and escape sequences
3. Returns the substring from the first `{` to the matching `}`

### 8.5 Retry Logic
* **Quick Scan:** Uses `RetryInterceptor` (OkHttp) with exponential backoff (2^n seconds) for up to 3 retries on 429/5xx errors.
* **Batch Analysis:** Manual retry loop with 10-second delay on 429 errors, up to 3 attempts per candidate.

### 8.6 Switching AI Providers
The app uses the **OpenAI-compatible chat completions format** (`/chat/completions`). This means you can switch to any provider that supports this format by changing only two constants:

| Provider | BASE_URL | MODEL_ID |
| :--- | :--- | :--- |
| **Google Gemini** (current) | `https://generativelanguage.googleapis.com/v1beta/openai/` | `gemini-3.5-flash` |
| **OpenAI** | `https://api.openai.com/v1/` | `gpt-4o-mini` |
| **Cerebras** | `https://api.cerebras.ai/v1/` | `llama-3.3-70b` |
| **Groq** | `https://api.groq.com/openai/v1/` | `llama-3.3-70b-versatile` |
| **Together AI** | `https://api.together.xyz/v1/` | `meta-llama/Llama-3.3-70B` |
| **Azure OpenAI** | `https://<resource>.openai.azure.com/openai/deployments/<dep>/` | `gpt-4o` |

---

## 9. Firebase Backend Architecture

### 9.1 Firestore Collection Hierarchy
```
organizations/                          (Collection)
  └── {orgId}                           (Document)  e.g., "ACME_CORP"
       ├── name: "Acme Corporation"
       ├── createdAt: <server timestamp>
       └── job_openings/                (Subcollection)
            └── {jobId}                 (Document)  e.g., "SENIOR_DEV"
                 ├── orgId: "ACME_CORP"
                 ├── title: "Senior Developer"
                 ├── description: "Brief description"
                 ├── requirements: "Full JD text..."
                 ├── postedDate: <server timestamp>
                 ├── cvCount: 12
                 ├── status: "open"
                 └── candidates/        (Subcollection)
                      └── {candidateId} (Document)  e.g., "portal_1721234567890"
                           ├── fileName: "resume.pdf"
                           ├── name: "John Doe"
                           ├── email: "john@example.com"
                           ├── score: 82
                           ├── status: "analyzed"
                           ├── hardSkillsMatched: ["Kotlin", "Android"]
                           ├── hardSkillsMissing: ["Compose"]
                           ├── softSkillsMatched: ["Leadership"]
                           ├── softSkillsMissing: ["Agile"]
                           ├── formattingIssues: ["Missing phone number"]
                           ├── summary: "Strong match..."
                           └── uploadedAt: "2026-07-19T14:30:00"
```

### 9.2 Personal Scans Storage
Personal quick scans are stored under a device-specific org ID:
```
organizations/
  └── PERSONAL_A1B2C3D4/               (Device-specific org)
       └── job_openings/
            └── QUICK_SCAN/
                 └── candidates/
                      └── personal_1721234567890/
```

### 9.3 Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /organizations/{orgId} {
      allow read, write: if request.auth != null;
      
      match /job_openings/{jobId} {
        allow read, write: if request.auth != null;
        
        match /candidates/{candidateId} {
          allow read, write: if request.auth != null;
        }
      }
    }
  }
}
`

> ⚠️ **Production Note:** The current rules allow any authenticated user to read/write any organization. For production, implement org-level access control using custom claims or a `members` subcollection.

### 9.4 FirebaseService.kt — Method Reference

| Method | Type | Description |
| :--- | :--- | :--- |
| `getOrganizations()` | Read | Fetches all orgs (cached 5 min) |
| `createOrganization(name)` | Write | Creates org with auto-generated ID |
| `deleteOrganization(orgId)` | Delete | Cascading delete of org, jobs, candidates |
| `getJobOpenings(orgId)` | Read | Fetches jobs for an org (cached 5 min) |
| `createJobOpening(orgId, title, desc, req)` | Write | Creates job with auto-generated ID |
| `getCandidates(orgId, jobId)` | Read | Fetches all candidates with manual deserialization |
| `updateCandidate(candidate)` | Write | Upserts candidate document |
| `updateJobStatus(orgId, jobId, status)` | Write | Updates job status field |
| `deleteJobCandidates(orgId, jobId)` | Delete | Removes all candidates for a job |
| `deleteJobOpening(orgId, jobId)` | Delete | Cascading delete of job + candidates |
| `getAllAnalyzedCandidates()` | Read | Collection group query for history |
| `getOrganizationName(orgId)` | Read | Helper for history display |
| `getJobTitle(orgId, jobId)` | Read | Helper for history display |

### 9.5 Caching Strategy
`FirebaseService` uses an in-memory `ConcurrentHashMap` cache with a 5-minute TTL for organization and job listing queries. Candidate queries are **never cached** to ensure real-time accuracy during batch analysis.

---

## 10. Local Storage & Security

### 10.1 AdminAuthManager (SharedPreferences)
| Key | Type | Purpose |
| :--- | :--- | :--- |
| `admin_pin` | String | 4-6 digit HR PIN (stored in plain text) |
| `pin_is_set` | Boolean | Whether PIN has been created |
| `owned_org_ids` | Set<String> | Org IDs this device owns |

> ⚠️ **Security Note:** PINs are stored in plain text `SharedPreferences`. For production, migrate to `EncryptedSharedPreferences` from AndroidX Security.

### 10.2 HistoryManager (Local JSON)
* Stores up to **30 most recent** personal scans in `scan_history.json`
* Uses Gson for serialization
* Located in app's internal storage (`context.openFileOutput`)
* Cleared via "Clear History" menu action

### 10.3 DeviceIdHelper
* Generates a device-unique ID using `Settings.Secure.ANDROID_ID`
* Format: `PERSONAL_<8-char-hash>`
* Used to namespace personal scans in Firestore

### 10.4 API Key Storage
* Stored in `local.properties` (excluded from version control via `.gitignore`)
* Injected into `BuildConfig.CEREBRAS_API_KEY` at build time
* Never exposed in logs or UI

---

## 11. File Parsing Pipeline

### 11.1 Supported Formats

| Format | MIME Type | Library | Notes |
| :--- | :--- | :--- | :--- |
| **PDF** | `application/pdf` | PDFBox (TomRoush) | Text-based PDFs only. Scanned/image PDFs will return empty text. |
| **DOCX** | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Apache POI (XWPF) | Modern Word format. Fully supported. |
| **DOC** | `application/msword` | Apache POI (HWPF) | Legacy Word format. Supported in MainActivity only (not Applicant Portal). |

### 11.2 Extraction Flow
```
User picks file via SAF (Storage Access Framework)
  → Validate extension (reject .doc in Applicant Portal)
  → Validate file size (max 10MB)
  → Open InputStream via ContentResolver
  → Route to appropriate parser:
      PDF  → PDDocument.load(stream) → PDFTextStripper.getText()
      DOCX → XWPFDocument(stream) → XWPFWordExtractor.getText()
      DOC  → HWPFDocument(stream) → WordExtractor.getText()
  → Clean text:
      • Normalize line endings (\r\n → \n)
      • Replace non-breaking spaces
      • Collapse multiple spaces/tabs
      • Remove excessive blank lines
      • Trim whitespace
  → Truncate to MAX_TEXT_CHARS (15,000 for portal, 40,000 for quick scan)
  → Quality check (min 50 words / 200 chars)
  → Send to AI
```

### 11.3 Known Parsing Limitations
* **Scanned PDFs:** Image-based PDFs will return empty or garbled text. The app shows a quality warning dialog.
* **Multi-column layouts:** Complex formatting may result in jumbled text order.
* **Password-protected files:** Will throw an exception and show an error toast.
* **Tables and charts:** POI and PDFBox extract text linearly; tabular data may lose structure.

---

## 12. Batch Processing Deep Dive

### 12.1 How It Works
When HR taps "Analyze" on a job opening:

1. **WakeLock Acquisition:** `WakeLockManager` acquires a `PARTIAL_WAKE_LOCK` (30 min timeout) and sets `FLAG_KEEP_SCREEN_ON` to prevent the OS from killing the process.
2. **Candidate Fetch:** All candidates with `status = "pending"` are loaded from Firestore.
3. **Sequential Processing:** Each candidate is processed one at a time:
   - Resume text is truncated to 20,000 chars
   - Job requirements are truncated to 15,000 chars
   - Prompt is constructed and sent to the LLM
   - Response is parsed as JSON
   - Candidate fields are updated in Firestore
   - 3-second delay between candidates (rate limit buffer)
4. **Progress Dialog:** A custom dialog shows current candidate name, progress bar, and count.
5. **Completion:** After all candidates are processed, the dialog is dismissed, WakeLock is released, and the results screen is launched.

### 12.2 Error Handling Per Candidate
* **Empty resume text:** Marked as `"Error"`, skipped
* **API failure (non-429):** Marked as `"Error"`, continues to next
* **API rate limit (429):** Retries up to 3 times with 10s delay
* **JSON parse failure:** Marked as `"Error"`, continues to next
* **Exception:** Caught, logged, marked as `"Error"`, continues

### 12.3 Performance Expectations
| Batch Size | Estimated Time | Notes |
| :--- | :--- | :--- |
| 1-10 | 1-3 min | Smooth, no issues |
| 10-30 | 3-10 min | Keep app in foreground |
| 30-50 | 10-20 min | WakeLock essential |
| 50-100 | 20-40 min | Risk of OOM on low-end devices |
| 100+ | 40+ min | Not recommended on-device |

---

## 13. Prerequisites & Complete Setup Guide

### Step 1: Environment
* **Android Studio** Ladybug (2024.2) or newer
* **JDK 17** or newer
* **Android SDK** API 35
* **Physical device or emulator** running Android 9.0 (API 28) or higher

### Step 2: Clone Repository
```bash
git clone <your-repo-url>
cd ResumeAI-Enterprise
```

### Step 3: Firebase Project Setup
1. Navigate to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add Project"** → Name it (e.g., "ResumeAI Enterprise")
3. Disable Google Analytics (optional) → Click Create
4. **Add Android App:**
   - Package name: `com.example.airesumescreener`
   - App nickname: `ResumeAI`
   - SHA-1: Get from Android Studio → Gradle → `signingReport`
5. Download `google-services.json` → Place in `app/` directory
6. **Enable Authentication:**
   - Go to Authentication → Sign-in method
   - Enable **Anonymous** sign-in
7. **Create Firestore Database:**
   - Go to Firestore Database → Click "Create Database"
   - Choose **Start in test mode** (we'll add rules next)
   - Select location closest to your users
8. **Apply Security Rules:**
   - Go to Firestore → Rules tab
   - Paste the rules from [Section 9.3](#93-firestore-security-rules)
   - Click **Publish**

### Step 4: API Key Setup
1. Obtain an API key from your chosen LLM provider
2. Open (or create) `local.properties` in the project root
3. Add:
```properties
CEREBRAS_API_KEY="your_actual_api_key_here"
```
4. Verify `BASE_URL` and `MODEL_ID` in `MainActivity.kt` match your provider (see [Section 8.6](#86-switching-ai-providers))

### Step 5: Build & Run
1. Open project in Android Studio
2. Wait for Gradle sync to complete
3. Select a device/emulator
4. Click **Run** (▶️)

### Step 6: First-Time HR Setup
1. Launch app → Select "I'm HR / Recruiter"
2. Create a 4-6 digit PIN when prompted
3. Tap "Create New Organization" → Enter company name
4. Note the generated Org ID (e.g., `ACME_CORP`)
5. Tap the organization → Create a Job Opening
6. Share the Org ID + Job ID with applicants


## 14. Configuration & Customization

### 14.1 All Configurable Constants

| Constant | File | Default | Description |
| :--- | :--- | :--- | :--- |
| `BASE_URL` | `MainActivity.kt` | `https://generativelanguage.googleapis.com/v1beta/openai/` | LLM API endpoint |
| `MODEL_ID` | `MainActivity.kt` | `gemini-3.5-flash` | LLM model identifier |
| `PERSONAL_JOB_ID` | `MainActivity.kt` | `QUICK_SCAN` | Firestore job ID for personal scans |
| `MAX_SIZE_BYTES` | `ApplicantPortalActivity.kt` | `10 * 1024 * 1024` (10MB) | Max resume file size |
| `MAX_TEXT_CHARS` | `ApplicantPortalActivity.kt` | `15000` | Max chars sent to AI (portal) |
| `maxChars` param | `MainActivity.kt` | `40000` | Max chars sent to AI (quick scan) |
| `TTL` | `FirebaseService.kt` | `5 * 60 * 1000` (5 min) | Cache time-to-live |
| History limit | `HistoryManager.kt` | `30` | Max local scan records |
| Batch delay | `JobDetailsActivity.kt` | `3000` (3s) | Delay between batch API calls |
| Batch retries | `JobDetailsActivity.kt` | `3` | Max retries per candidate |
| WakeLock timeout | `WakeLockManager.kt` | `30 * 60 * 1000` (30 min) | Max batch processing time |
| Min PIN length | `AdminActivity.kt` | `4` | Minimum PIN digits |
| Max PIN length | `AdminActivity.kt` | `6` | Maximum PIN digits |
| Min job title | `JobDetailsActivity.kt` | `3` | Minimum job title characters |
| Min requirements | `JobDetailsActivity.kt` | `50` | Minimum job requirements characters |
| Min JD length | `MainActivity.kt` | `20` | Minimum job description characters |

### 14.2 Theming & Branding
* **Primary Color:** `#0F766E` (Teal 700) — defined inline in layouts
* **App Bar Color:** `#134E4A` (Teal 900)
* **Background:** `@drawable/gradient_background`
* **App Logo:** `@drawable/app_logo` (replace with your brand logo)
* **App Name:** `@string/app_name` in `strings.xml`

To rebrand:
1. Replace `app_logo.png` in `res/drawable/`
2. Update `app_name` in `res/values/strings.xml`
3. Search-replace color hex codes across XML layouts
4. Update `applicationId` in `build.gradle.kts` if changing package name

---

## 15. Build, Sign & Deploy

### 15.1 Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 15.2 Release Build (Unsigned)
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### 15.3 Signed Release (for Play Store)
1. Generate a keystore:
```bash
keytool -genkey -v -keystore resumeai-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias resumeai
```
2. Add to `local.properties`:
```properties
RELEASE_STORE_FILE=../resumeai-release.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=resumeai
RELEASE_KEY_PASSWORD=your_password
```
3. Add signing config to `build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE", ""))
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false  // Keep false due to POI/PDFBox reflection
        }
    }
}
```
4. Build AAB for Play Store:
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### 15.4 ProGuard Warning
> ⚠️ **Do NOT enable `isMinifyEnabled = true`** without adding extensive ProGuard rules for Apache POI, PDFBox, and Gson. These libraries rely heavily on reflection and will crash if minified without proper `-keep` rules.

---

## 16. Error Handling & Resilience

### 16.1 Network Errors
| Scenario | Behavior |
| :--- | :--- |
| No internet | Firebase offline cache serves cached data; AI calls fail with toast |
| API timeout (120s) | OkHttp throws `SocketTimeoutException`; caught and shown as toast |
| API rate limit (429) | Auto-retry with backoff (quick scan) or 10s delay (batch) |
| API server error (5xx) | Retry up to 3 times (quick scan); mark as Error (batch) |
| Invalid API key (401) | Toast with error code; user must check `local.properties` |

### 16.2 File Errors
| Scenario | Behavior |
| :--- | :--- |
| Unsupported format | Toast: "Unsupported file type" |
| File too large (>10MB) | Toast: "File is too large" |
| Legacy .doc (portal) | Toast: "Re-save as PDF or .docx" |
| Empty extraction | Warning dialog: "Extraction Quality Warning" |
| Corrupt file | Exception caught; toast with error message |

### 16.3 Firebase Errors
| Scenario | Behavior |
| :--- | :--- |
| Auth failure | Logged to Crashlytics; app continues with local features |
| Firestore write failure | Toast with error; data saved locally as fallback |
| Org/Job not found | Validation toast: "Job not found in organization" |
| Deserialization error | Logged per-document; other candidates still load |

### 16.4 Crash Reporting
* Firebase Crashlytics is enabled in **release builds only**
* All AI parsing errors and Firebase failures are logged via `FirebaseCrashlytics.getInstance().recordException(e)`
* Debug builds log to Logcat only

---

## 17. Testing Strategy

### 17.1 Manual Testing Checklist

**Applicant Flow:**
- [ ] Upload PDF → verify text extraction
- [ ] Upload DOCX → verify text extraction
- [ ] Upload DOC → verify text extraction (MainActivity only)
- [ ] Upload image-based PDF → verify quality warning
- [ ] Upload file > 10MB → verify rejection
- [ ] Paste JD < 20 chars → verify analyze button disabled
- [ ] Run analysis → verify score, skills, summary
- [ ] Export to Excel → verify file opens correctly
- [ ] Submit to HR with invalid org code → verify error toast
- [ ] Submit to HR with valid org/job → verify success

**HR Flow:**
- [ ] First-time PIN creation → verify 4-6 digit validation
- [ ] Returning PIN entry → verify correct/incorrect handling
- [ ] Create organization → verify Firestore document
- [ ] Create job with requirements < 50 chars → verify rejection
- [ ] Batch analyze 5 candidates → verify all scored
- [ ] View results → verify ranking by score
- [ ] Export results → verify Excel content
- [ ] Close job → verify cascading delete
- [ ] Delete organization → verify cascading delete

**History:**
- [ ] Personal scan appears in history
- [ ] Batch result appears in history
- [ ] Tap batch result → opens full report
- [ ] Clear history → verify local data wiped

### 17.2 Automated Testing
Currently, the project includes JUnit and Espresso dependencies but no automated tests. Recommended additions:
* **Unit Tests:** `FirebaseService` caching logic, `extractJsonObject()`, `cleanTextForAts()`
* **Instrumented Tests:** File picker flow, form validation
* **Integration Tests:** End-to-end AI analysis with a test API key

---

## 18. Troubleshooting & FAQ

### Q: App crashes on launch with "FirebaseApp is not initialized"
**A:** Ensure `google-services.json` is in the `app/` directory and the Google Services plugin is applied in `build.gradle.kts`.

### Q: AI analysis returns "API Error 401"
**A:** Your API key is invalid or expired. Check `local.properties` and verify the key matches the `BASE_URL` provider.

### Q: AI analysis returns "API Error 429"
**A:** You've hit the rate limit. Wait a few minutes and try again. Consider upgrading your API plan or switching to a provider with higher limits.

### Q: PDF extraction returns empty text
**A:** The PDF is likely image-based (scanned). The app cannot perform OCR. Ask the user to provide a text-based PDF or DOCX file.

### Q: Batch analysis stops midway
**A:** The device may have killed the process due to memory pressure. Try a smaller batch or use a device with more RAM. Check Logcat for `OutOfMemoryError`.

### Q: HR can't see their organization after reinstalling
**A:** Org ownership is stored locally in `SharedPreferences`. Reinstalling wipes this data. The org still exists in Firestore but must be re-claimed. **Migration path:** Move to cloud-based auth.

### Q: Excel export produces a corrupt file
**A:** Ensure the device has a file manager that supports `.xlsx`. The file is generated using Apache POI and is standards-compliant.

### Q: App shows "Online" but Firebase calls fail
**A:** The "Online" indicator is static (not a real network check). Verify actual connectivity and Firebase project configuration.

### Q: Build fails with "META-INF/DEPENDENCIES" error
**A:** The `packaging` block in `build.gradle.kts` should already exclude these files. If the error persists, add the conflicting path to the `excludes` list.

---

## 19. Handover & Future Migration Guide

### 19.1 Priority 1: Move Document Parsing to Backend
**Current State:** PDFBox and Apache POI run on-device, increasing APK size by ~15MB and risking OOM crashes.
**Migration Steps:**
1. Remove POI/PDFBox dependencies from `build.gradle.kts`
2. Add Firebase Storage or AWS S3 dependency
3. Upload raw file bytes from `ApplicantPortalActivity` and `MainActivity`
4. Create a backend service (Node.js/Python) that:
   - Downloads the file from storage
   - Extracts text using `PyMuPDF` or `pdf-parse`
   - Calls the LLM API
   - Writes results back to Firestore
5. Update the mobile app to trigger the backend function via HTTPS callable or REST

### 19.2 Priority 2: Replace Local PIN with Cloud Auth
**Current State:** HR PIN is in plain-text `SharedPreferences`.
**Migration Steps:**
1. Enable Email/Password auth in Firebase Console
2. Replace `AdminAuthManager` PIN dialogs with Firebase `signInWithEmailAndPassword()`
3. Store org ownership in a Firestore `admin_users` collection instead of local prefs
4. Update Firestore security rules to check `request.auth.uid` against org membership
5. Optionally integrate SSO (Okta, Auth0, Firebase Auth with SAML)

### 19.3 Priority 3: Migrate to Custom Backend (Node/PostgreSQL)
**Migration Steps:**
1. **Database:** Create PostgreSQL tables mirroring the Firestore hierarchy
2. **API:** Build REST endpoints using Express/Fastify/NestJS
3. **Auth:** Implement JWT-based authentication
4. **Mobile:** Replace `FirebaseService.kt` with a Retrofit interface:
```kotlin
interface ResumeApiService {
    @GET("organizations")
    suspend fun getOrgs(@Header("Authorization") token: String): List<Organization>
    
    @POST("organizations/{orgId}/jobs/{jobId}/analyze")
    suspend fun triggerBatchAnalysis(
        @Header("Authorization") token: String,
        @Path("orgId") orgId: String,
        @Path("jobId") jobId: String
    ): Response<BatchJobResponse>
}
```
5. **Storage:** Replace Firebase Storage with S3 presigned URLs
6. **Real-time:** Replace Firestore listeners with WebSocket or SSE

### 19.4 Priority 4: Async Batch Processing
**Current State:** Batch analysis blocks the UI thread (with WakeLock).
**Migration Steps:**
1. Implement a backend message queue (RabbitMQ, AWS SQS, Bull)
2. Mobile app sends a single "start batch" API call
3. Backend processes candidates asynchronously
4. Mobile app polls for status or receives push notifications (FCM)
5. Remove `WakeLockManager` from the mobile app

### 19.5 Priority 5: Add OCR for Scanned PDFs
**Options:**
* **On-device:** ML Kit Text Recognition (adds ~5MB to APK)
* **Backend:** Tesseract OCR or AWS Textract
* **API:** Google Cloud Vision API

---

## 20. Known Limitations & Scaling Notes

| Limitation | Impact | Severity | Mitigation |
| :--- | :--- | :--- | :--- |
| On-device parsing | Battery drain, OOM risk | 🟡 Medium | Move to backend |
| Plain-text PIN storage | Security risk if device compromised | 🟡 Medium | Use EncryptedSharedPreferences |
| Device-tied org ownership | Data loss on reinstall | 🔴 High | Move to cloud auth |
| No real-time sync | Stale data in multi-device HR teams | 🟡 Medium | Add Firestore snapshot listeners |
| Static "Online" indicator | Misleading UX | 🟢 Low | Implement real connectivity check |
| No OCR | Scanned PDFs fail | 🟡 Medium | Add ML Kit or backend OCR |
| Sequential batch processing | Slow for large batches | 🟡 Medium | Parallelize or move to backend |
| No minification | Large APK (~25MB+) | 🟢 Low | Add ProGuard rules for POI/PDFBox |
| Single API key | No per-user billing | 🟡 Medium | Implement backend proxy |
| No pagination | Firestore queries load all docs | 🟢 Low | Add pagination for 100+ candidates |

---

## 21. Contributing Guidelines

### Code Style
* Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
* Use ViewBinding (no `findViewById`)
* All network calls must be in `Dispatchers.IO`
* All UI updates must be on `Dispatchers.Main`
* Use `lifecycleScope.launch` for Activity-scoped coroutines

### Branching
```
main          ← Production-ready
├── develop   ← Integration branch
│   ├── feature/ocr-support
│   ├── feature/cloud-auth
│   └── fix/batch-oom-crash
└── release/v1.1.0
```

### Commit Messages
```
feat: add OCR support for scanned PDFs
fix: resolve OOM crash during batch analysis of 50+ resumes
docs: update README with migration guide
refactor: extract AI prompt to separate class
```

### Pull Request Process
1. Create feature branch from `develop`
2. Implement changes with tests
3. Submit PR with description and screenshots
4. Require 1 approval before merge
5. Squash merge into `develop`

---

## 22. Changelog

### v1.0.0 (July 2026)
* Initial release
* Applicant quick scan with AI-powered ATS analysis
* Applicant portal for submitting resumes to HR
* HR admin portal with PIN authentication
* Organization and job opening management
* Batch AI analysis with WakeLock and retry logic
* Candidate ranking and detailed reports
* Excel export for HR stakeholders
* Unified scan history (personal + batch)
* Firebase Firestore backend with offline persistence
* Firebase Crashlytics integration
* Support for PDF, DOCX, and DOC file formats
