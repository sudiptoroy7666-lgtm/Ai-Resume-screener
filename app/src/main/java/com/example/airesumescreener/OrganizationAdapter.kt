package com.example.airesumescreener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airesumescreener.databinding.ItemOrganizationBinding

class OrganizationAdapter(
    private val organizations: List<Organization>,
    private val onOrgClick: (Organization) -> Unit,
    private val onOrgDelete: (Organization) -> Unit
) : RecyclerView.Adapter<OrganizationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemOrganizationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrganizationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val org = organizations[position]
        with(holder.binding) {
            tvOrgName.text = org.name
            tvOrgId.text = "ID: ${org.id}"
            tvAction.text = "Manage jobs →"

            root.setOnClickListener { onOrgClick(org) }
            btnDelete.setOnClickListener { onOrgDelete(org) }
        }
    }

    override fun getItemCount() = organizations.size
}