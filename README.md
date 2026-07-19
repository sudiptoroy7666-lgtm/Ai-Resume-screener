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
