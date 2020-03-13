package android.ut3.aviatio.repository

import android.ut3.aviatio.model.StoredScore
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ScoreRepository {

    var firestoreDB = FirebaseFirestore.getInstance()
    var scoresCollectionReference: CollectionReference = firestoreDB.collection("scores")

    fun getStoredScoreReference(): CollectionReference {
        return scoresCollectionReference
    }

    fun saveScore(storedScore: StoredScore): Task<DocumentReference> {
        return scoresCollectionReference.add(storedScore)
    }
}