package com.example.airesumescreener

import android.content.Context
import android.content.SharedPreferences

object AdminAuthManager {
    private const val PREFS_NAME = "admin_auth"
    private const val KEY_ADMIN_PIN = "admin_pin"
    private const val KEY_ORG_IDS = "owned_org_ids"
    private const val KEY_PIN_SET = "pin_is_set"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ===== PIN MANAGEMENT =====

    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PIN_SET, false)
    }

    fun setPin(context: Context, pin: String) {
        getPrefs(context).edit()
            .putString(KEY_ADMIN_PIN, pin)
            .putBoolean(KEY_PIN_SET, true)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val storedPin = getPrefs(context).getString(KEY_ADMIN_PIN, "") ?: ""
        return storedPin == pin
    }

    fun resetPin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ADMIN_PIN)
            .putBoolean(KEY_PIN_SET, false)
            .apply()
    }

    // ===== ORG OWNERSHIP =====

    fun getOwnedOrgIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_ORG_IDS, emptySet()) ?: emptySet()
    }

    fun addOwnedOrg(context: Context, orgId: String) {
        val current = getOwnedOrgIds(context).toMutableSet()
        current.add(orgId)
        getPrefs(context).edit()
            .putStringSet(KEY_ORG_IDS, current)
            .apply()
    }

    fun removeOwnedOrg(context: Context, orgId: String) {
        val current = getOwnedOrgIds(context).toMutableSet()
        current.remove(orgId)
        getPrefs(context).edit()
            .putStringSet(KEY_ORG_IDS, current)
            .apply()
    }

    fun ownsOrg(context: Context, orgId: String): Boolean {
        return getOwnedOrgIds(context).contains(orgId)
    }
}