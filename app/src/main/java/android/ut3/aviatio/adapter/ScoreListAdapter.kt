package android.ut3.aviatio.adapter

import android.ut3.aviatio.R
import android.ut3.aviatio.helper.getHumanTimeFormatFromMilliseconds
import android.ut3.aviatio.model.RankedScore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ScoreListAdapter(private val scoresList: List<RankedScore>) :
    RecyclerView.Adapter<ScoreListAdapter.ViewHolder>() {

    private val random : Random = Random()
    private val listAvatars : Array<Int> = arrayOf(R.drawable.ic_woman, R.drawable.ic_woman2, R.drawable.ic_man, R.drawable.ic_man2, R.drawable.ic_man3)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.cardview_score, parent,
            false
        )
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scoresList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var rankedScore = scoresList.get(position)
        holder.bind(rankedScore)
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(rankedScore: RankedScore) {
            (itemView.findViewById(R.id.rank) as TextView).setText(rankedScore.rank.toString())
            (itemView.findViewById(R.id.name) as TextView).setText(rankedScore.name)
            (itemView.findViewById(R.id.time) as TextView).setText(
                getHumanTimeFormatFromMilliseconds(rankedScore.time)
            )
            (itemView.findViewById(R.id.icon_card) as ImageView).setImageResource(listAvatars[random.nextInt(listAvatars.size)])
        }
    }
}