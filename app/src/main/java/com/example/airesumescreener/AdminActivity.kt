package com.example.airesumescreener

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airesumescreener.databinding.ActivityAdminBinding
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var orgAdapter: OrganizationAdapter
    private val organizations = mutableListOf<Organization>()
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        orgAdapter = OrganizationAdapter(
            organizations,
            onOrgClick = { org -> openOrgJobs(org) },
            onOrgDelete = { org -> deleteOrg(org) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = orgAdapter

        binding.btnAddOrg.setOnClickListener { showCreateOrgDialog() }

        // Gate access with PIN
        if (!AdminAuthManager.isPinSet(this)) {
            showSetPinDialog()
        } else {
            showVerifyPinDialog()
        }
    }

    // ===== PIN SETUP (First Time) =====

    private fun showSetPinDialog() {
        val input = EditText(this).apply {
            hint = "Create a 4-6 digit admin PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(40, 20, 40, 20)
            maxLines = 1
        }

        AlertDialog.Builder(this)
            .setTitle("Set Admin PIN")
            .setMessage("This PIN protects your HR portal. Only you will see your organizations on this device.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Set PIN") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.length in 4..6) {
                    AdminAuthManager.setPin(this, pin)
                    Toast.makeText(this, "✅ Admin PIN set", Toast.LENGTH_SHORT).show()
                    isAuthenticated = true
                    loadOrganizations()
                } else {
                    Toast.makeText(this, "PIN must be 4-6 digits", Toast.LENGTH_SHORT).show()
                    showSetPinDialog()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    // ===== PIN VERIFY (Returning User) =====

    private fun showVerifyPinDialog() {
        val input = EditText(this).apply {
            hint = "Enter your admin PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(40, 20, 40, 20)
            maxLines = 1
        }

        AlertDialog.Builder(this)
            .setTitle("HR Admin Portal")
            .setMessage("Enter your admin PIN to continue.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                val pin = input.text.toString().trim()
                if (AdminAuthManager.verifyPin(this, pin)) {
                    isAuthenticated = true
                    loadOrganizations()
                } else {
                    Toast.makeText(this, "❌ Incorrect PIN", Toast.LENGTH_SHORT).show()
                    showVerifyPinDialog()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    // ===== LOAD ONLY OWNED ORGS =====

    private fun loadOrganizations() {
        if (!isAuthenticated) return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            // Get ALL orgs from Firestore
            val allOrgs = FirebaseService.getOrganizations()

            // Filter to only show orgs THIS device owns
            val ownedIds = AdminAuthManager.getOwnedOrgIds(this@AdminActivity)
            val myOrgs = allOrgs.filter { ownedIds.contains(it.id) }

            organizations.clear()
            organizations.addAll(myOrgs)
            orgAdapter.notifyDataSetChanged()

            binding.progressBar.visibility = View.GONE
            if (organizations.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    // ===== CREATE ORG (Register Ownership) =====

    private fun showCreateOrgDialog() {
        val input = EditText(this).apply {
            hint = "Organization Name (e.g., Acme Corp)"
            setPadding(40, 20, 40, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Create Organization")
            .setMessage("Enter a unique organization name. Applicants will use this code to submit resumes.")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.length >= 3) {
                    createOrganization(name)
                } else {
                    Toast.makeText(this, "Name must be at least 3 characters", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createOrganization(name: String) {
        lifecycleScope.launch {
            val result = FirebaseService.createOrganization(name)
            result.fold(
                onSuccess = { orgId ->
                    // Register ownership locally
                    AdminAuthManager.addOwnedOrg(this@AdminActivity, orgId)
                    Toast.makeText(this@AdminActivity, "Organization created: $orgId", Toast.LENGTH_SHORT).show()
                    loadOrganizations()
                },
                onFailure = {
                    Toast.makeText(this@AdminActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ===== OPEN JOBS =====

    private fun openOrgJobs(org: Organization) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("ORG_ID", org.id)
            putExtra("ORG_NAME", org.name)
        }
        startActivity(intent)
    }

    // ===== DELETE ORG (Remove Ownership) =====

    private fun deleteOrg(org: Organization) {
        AlertDialog.Builder(this)
            .setTitle("Delete Organization")
            .setMessage("Delete '${org.name}' and ALL its data from the cloud? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FirebaseService.deleteOrganization(org.id)
                    // Remove local ownership
                    AdminAuthManager.removeOwnedOrg(this@AdminActivity, org.id)
                    Toast.makeText(this@AdminActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    loadOrganizations()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (isAuthenticated) {
            loadOrganizations()
        }
    }
}