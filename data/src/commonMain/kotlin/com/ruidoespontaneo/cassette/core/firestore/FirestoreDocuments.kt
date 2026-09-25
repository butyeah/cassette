package com.ruidoespontaneo.cassette.core.firestore

/**
 * One Firestore document as plain Kotlin values: strings, [Long] integers, [Double]s, [Boolean]s,
 * `null`, [List]s and nested `Map<String, Any?>`s — the same shapes the Android SDK's
 * `DocumentSnapshot.getData()` returns, so parsing code doesn't care which platform read it.
 */
class FirestoreDocument(val id: String, val fields: Map<String, Any?>)

/**
 * Read access to the public, read-only collections the offline pipeline uploads (`albumsByDay`,
 * `albumTracks` — see firestore.rules). Android reads them through the Firebase SDK; iOS through
 * [FirestoreRestDocuments], since reading public collections needs neither sign-in nor the SDK.
 */
interface FirestoreDocuments {

    /** Every document in [collection] whose fields equal all of [fieldsEqualTo]. */
    suspend fun query(collection: String, fieldsEqualTo: Map<String, Int>): List<FirestoreDocument>

    /** `null` when there's no document [id] in [collection]. */
    suspend fun get(collection: String, id: String): FirestoreDocument?
}

internal fun Map<String, Any?>.string(field: String): String? = this[field] as? String

internal fun Map<String, Any?>.int(field: String): Int? = (this[field] as? Number)?.toInt()

internal fun Map<String, Any?>.list(field: String): List<Any?> = this[field] as? List<Any?> ?: emptyList()

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.map(field: String): Map<String, Any?>? = this[field] as? Map<String, Any?>
