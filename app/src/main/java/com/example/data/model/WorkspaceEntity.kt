package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#4F46E5",
    val iconName: String = "folder",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
