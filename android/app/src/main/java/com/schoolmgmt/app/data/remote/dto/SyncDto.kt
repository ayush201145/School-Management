package com.schoolmgmt.app.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * IMPORTANT CONSTRAINT on every Map<String, Any?> in this file
 * (SyncChangeDto.data, SyncPullResponse.tables, SyncConflictDto.clientData/serverData):
 *
 * Moshi can serialize Map<String, Any?> for genuinely dynamic JSON
 * (verified: it has built-in support for Map/List as a JsonElement
 * equivalent), BUT only when every value is one of JSON's own
 * primitive types at runtime: String, Double/Int, Boolean, List<*>,
 * Map<*, *>, or null. Moshi explicitly REFUSES java.* or android.*
 * platform types (e.g. java.util.Date) without a custom adapter.
 *
 * Practical effect for SyncRepository, which builds these maps from
 * Room entities: convert every value before putting it in the map —
 * - Kotlin enum fields (FeeStatus, PaymentMode, etc.) → value.name (String), never the raw enum
 * - Long timestamps → fine as-is (Moshi handles Long/Double numerics)
 * - Never put a raw java.util.Date, BigDecimal, or similar platform type in directly
 */

/** One change being pushed up to the server — mirrors syncService.js's expected shape exactly. */
@JsonClass(generateAdapter = true)
data class SyncChangeDto(
    val table: String,
    val id: String,
    val op: String, // "upsert" | "delete"
    val data: Map<String, Any?>? = null,
    val clientUpdatedAt: String,
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    val changes: List<SyncChangeDto>,
)

@JsonClass(generateAdapter = true)
data class SyncChangeResult(
    val table: String,
    val id: String,
    val status: String, // "applied" | "conflict" | "rejected" | "error"
    val op: String? = null,
    val reason: String? = null,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class SyncPushResponse(
    val appliedCount: Int,
    val conflictCount: Int,
    val errorCount: Int,
    val results: List<SyncChangeResult>,
)

@JsonClass(generateAdapter = true)
data class SyncPullResponse(
    val serverTime: String,
    val tables: Map<String, List<Map<String, Any?>>>,
)

@JsonClass(generateAdapter = true)
data class SyncConflictDto(
    val id: String,
    val tableName: String,
    val recordId: String,
    val clientData: Map<String, Any?>,
    val serverData: Map<String, Any?>,
    val clientUserId: String? = null,
    val status: String,
    val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class ResolveConflictRequest(
    val resolution: String, // "keep_server" | "keep_client"
)
