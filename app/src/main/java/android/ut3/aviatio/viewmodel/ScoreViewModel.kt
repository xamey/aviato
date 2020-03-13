package android.ut3.aviatio.viewmodel

import android.content.ContentValues
import android.ut3.aviatio.model.Score
import android.ut3.aviatio.model.StoredScore
import android.ut3.aviatio.repository.ScoreRepository
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import java.util.*

class ScoreViewModel(
    private val scoreRepository: ScoreRepository
) : ViewModel() {

    private var storedScores: MutableLiveData<List<StoredScore>> = MutableLiveData()

    fun getStoredScore(): LiveData<List<StoredScore>> {
        scoreRepository.getStoredScoreReference()
            .addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen to scoreRepository failed.", e)
                    storedScores.value = null
                    return@EventListener
                }
                var savedStoredScores: MutableList<StoredScore> = mutableListOf()
                for (doc in value!!) {
                    var storedScore = doc.toObject(StoredScore::class.java)
                    savedStoredScores.add(storedScore)
                }
                storedScores.value = savedStoredScores
            })
        return storedScores
    }

    fun saveScore(score: Score) {
        val toStoreScore = StoredScore(
            Nom = score.Nom,
            Temps = score.Temps,
            Date = Timestamp(Date())
        )
        scoreRepository.saveScore(toStoreScore)
    }

}