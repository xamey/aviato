package android.ut3.aviatio.model

import com.google.firebase.Timestamp

data class RankedScore(var rank: Int, var name: String, var date: Timestamp, var time: Int) {
}