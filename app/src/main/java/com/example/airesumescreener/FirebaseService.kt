package com.example.airesumescreener

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

object FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cache = ConcurrentHashMap<String, Pair<Long, Any>>()
    private const val TTL = 5 * 60 * 1000L
    private const val TAG = "FirebaseService"

    /**
     * Ensures the user is authenticated before any Firestore operation.
     * Uses anonymous auth so no user login is required.
     */
    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
                Log.d(TAG, "✅ Anonymous auth successful: ${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Auth failed: ${e.message}")
                throw e
            }
        }
    }

    private inline fun <reified T> cached(key: String): T? {
        val c = cache[key] ?: return null
        return if (System.currentTimeMillis() - c.first < TTL) c.second as? T
        else { cache.remove(key); null }
    }
    private fun put(key: String, v: Any) { cache[key] = System.currentTimeMillis() to v }

    suspend fun getOrganizations(): List<Organization> {
        cached<List<Organization>>("orgs")?.let { return it }
        return try {
            ensureAuthenticated()
            val r = db.collection("organizations").orderBy("createdAt").get().await().toObjects<Organization>()
            put("orgs", r); r
        } catch (e: Exception) {
            Log.e(TAG, "getOrganizations error: ${e.message}")
            emptyList()
        }
    }

    suspend fun createOrganization(name: String): Result<String> = try {
        ensureAuthenticated()
        val id = name.trim().uppercase().replace(Regex("[^A-Z0-9]"), "_")
        if (db.collection("organizations").document(id).get().await().exists())
            Result.failure(Exception("Organization already exists"))
        else {
            db.collection("organizations").document(id)
                .set(Organization(id = id, name = name.trim())).await()
            cache.remove("orgs"); Result.success(id)
        }
    } catch (e: Exception) {
        Log.e(TAG, "createOrganization error: ${e.message}")
        Result.failure(e)
    }

    suspend fun deleteOrganization(orgId: String): Result<Unit> = try {
        ensureAuthenticated()
        val jobs = db.collection("organizations").document(orgId)
            .collection("job_openings").get().await()
        for (j in jobs) {
            db.collection("organizations").document(orgId)
                .collection("job_openings").document(j.id)
                .collection("candidates").get().await()
                .forEach { it.reference.delete().await() }
            j.reference.delete().await()
        }
        db.collection("organizations").document(orgId).delete().await()
        cache.remove("orgs"); Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "deleteOrganization error: ${e.message}")
        Result.failure(e)
    }

    suspend fun getJobOpenings(orgId: String): List<JobOpening> {
        cached<List<JobOpening>>("jobs_$orgId")?.let { return it }
        return try {
            ensureAuthenticated()
            val r = db.collection("organizations").document(orgId)
                .collection("job_openings").orderBy("postedDate").get().await()
                .toObjects<JobOpening>()
            put("jobs_$orgId", r); r
        } catch (e: Exception) {
            Log.e(TAG, "getJobOpenings error: ${e.message}")
            emptyList()
        }
    }

    suspend fun createJobOpening(orgId: String, title: String, desc: String, requirements: String): Result<String> = try {
        ensureAuthenticated()
        val id = title.trim().uppercase().replace(Regex("[^A-Z0-9]"), "_")

        if (db.collection("organizations").document(orgId)
                .collection("job_openings").document(id).get().await().exists())
            Result.failure(Exception("Job ID already exists"))
        else {
            db.collection("organizations").document(orgId)
                .collection("job_openings").document(id)
                .set(JobOpening(
                    id = id,
                    orgId = orgId,
                    title = title.trim(),
                    description = desc.trim(),
                    requirements = requirements.trim()  // ← NEW FIELD
                ))
                .await()
            cache.remove("jobs_$orgId")
            Result.success(id)
        }
    } catch (e: Exception) {
        Log.e(TAG, "createJobOpening error: ${e.message}")
        Result.failure(e)
    }

    suspend fun getCandidates(orgId: String, jobId: String): List<Candidate> {
        return try {
            ensureAuthenticated()

            val snapshot = db.collection("organizations").document(orgId)
                .collection("job_openings").document(jobId)
                .collection("candidates")
                .get()
                .await()

            Log.d(TAG, "Found ${snapshot.documents.size} candidate documents")

            if (snapshot.documents.isEmpty()) {
                return emptyList()
            }

            // Manually deserialize to catch field-level errors
            val candidates = mutableListOf<Candidate>()
            for (doc in snapshot.documents) {
                try {
                    val candidate = doc.toObject(Candidate::class.java)
                    if (candidate != null) {
                        // Ensure ID is set from document ID if not in document
                        val finalCandidate = if (candidate.id.isBlank()) {
                            candidate.copy(id = doc.id)
                        } else {
                            candidate
                        }
                        candidates.add(finalCandidate)
                        Log.d(TAG, "✓ Deserialized candidate: ${finalCandidate.name} (${finalCandidate.id})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Failed to deserialize document ${doc.id}: ${e.message}", e)
                    // Continue with other documents instead of failing entirely
                }
            }

            Log.d(TAG, "Successfully deserialized ${candidates.size} candidates")
            candidates

        } catch (e: Exception) {
            Log.e(TAG, "getCandidates error: ${e.message}", e)
            emptyList()
        }
    }
    suspend fun updateCandidate(c: Candidate) {
        try {
            ensureAuthenticated()
            db.collection("organizations").document(c.orgId)
                .collection("job_openings").document(c.jobId)
                .collection("candidates").document(c.id).set(c).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateCandidate error: ${e.message}")
        }
    }

    suspend fun updateJobStatus(orgId: String, jobId: String, status: String) {
        try {
            ensureAuthenticated()
            db.collection("organizations").document(orgId)
                .collection("job_openings").document(jobId)
                .update("status", status).await()
            cache.remove("jobs_$orgId")
        } catch (e: Exception) {
            Log.e(TAG, "updateJobStatus error: ${e.message}")
        }
    }

    suspend fun deleteJobCandidates(orgId: String, jobId: String): Int {
        return try {
            ensureAuthenticated()
            val candidates = getCandidates(orgId, jobId)
            candidates.forEach { c ->
                try {
                    db.collection("organizations").document(orgId)
                        .collection("job_openings").document(jobId)
                        .collection("candidates").document(c.id).delete().await()
                } catch (_: Exception) {}
            }
            candidates.size
        } catch (e: Exception) {
            Log.e(TAG, "deleteJobCandidates error: ${e.message}")
            0
        }
    }

    // Add these methods inside the FirebaseService object

    /**
     * Deletes the entire job opening and all its candidates from Firestore.
     * Called when a job is closed.
     */
    suspend fun deleteJobOpening(orgId: String, jobId: String): Result<Unit> = try {
        ensureAuthenticated()
        // Delete all candidates first
        val candidates = getCandidates(orgId, jobId)
        candidates.forEach { c ->
            try {
                db.collection("organizations").document(orgId)
                    .collection("job_openings").document(jobId)
                    .collection("candidates").document(c.id).delete().await()
            } catch (_: Exception) {}
        }
        // Delete the job opening document itself
        db.collection("organizations").document(orgId)
            .collection("job_openings").document(jobId).delete().await()
        cache.remove("jobs_$orgId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "deleteJobOpening error: ${e.message}")
        Result.failure(e)
    }

    /**
     * Fetches ALL analyzed candidates from ALL organizations and jobs.
     * Used to populate unified history in HistoryActivity.
     */
    suspend fun getAllAnalyzedCandidates(): List<Candidate> = try {
        ensureAuthenticated()
        // Use collection group query to fetch from all "candidates" subcollections
        db.collectionGroup("candidates")
            .whereEqualTo("status", "analyzed")
            .get()
            .await()
            .toObjects<Candidate>()
    } catch (e: Exception) {
        Log.e(TAG, "getAllAnalyzedCandidates error: ${e.message}")
        emptyList()
    }

    /**
     * Fetches organization name by ID (helper for history display).
     */
    suspend fun getOrganizationName(orgId: String): String = try {
        ensureAuthenticated()
        val doc = db.collection("organizations").document(orgId).get().await()
        doc.getString("name") ?: orgId
    } catch (e: Exception) {
        Log.e(TAG, "getOrganizationName error: ${e.message}")
        orgId
    }

    /**
     * Fetches job title by org and job ID (helper for history display).
     */
    suspend fun getJobTitle(orgId: String, jobId: String): String = try {
        ensureAuthenticated()
        val doc = db.collection("organizations").document(orgId)
            .collection("job_openings").document(jobId).get().await()
        doc.getString("title") ?: jobId
    } catch (e: Exception) {
        Log.e(TAG, "getJobTitle error: ${e.message}")
        jobId
    }
}