package com.ruidoespontaneo.cassette.core.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** [FirestoreDocuments] through the Firebase Android SDK. */
class FirebaseFirestoreDocuments @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreDocuments {

    override suspend fun query(collection: String, fieldsEqualTo: Map<String, Int>): List<FirestoreDocument> {
        var query: Query = firestore.collection(collection)
        for ((field, value) in fieldsEqualTo) {
            query = query.whereEqualTo(field, value)
        }
        return query.get().await().documents.map { FirestoreDocument(it.id, it.data.orEmpty()) }
    }

    override suspend fun get(collection: String, id: String): FirestoreDocument? {
        val snapshot = firestore.collection(collection).document(id).get().await()
        return if (snapshot.exists()) FirestoreDocument(id, snapshot.data.orEmpty()) else null
    }
}
