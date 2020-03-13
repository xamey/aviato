package android.ut3.aviatio.helper

fun getHumanTimeFormatFromMilliseconds(milliseconds: Int): String {
    var message = ""
        val millisecondes = (milliseconds % 1000)/100
        val seconds = (milliseconds / 1000)% 60
        val minutes = (milliseconds / (1000 * 60) % 60)
        val hours = (milliseconds / (1000 * 60 * 60) % 24)
         if (hours == 0 && minutes != 0) {
            message = String.format("%d minutes, %d secondes et %d millisecondes", minutes, seconds, millisecondes)
        } else if (hours == 0 && minutes == 0) {
            message = String.format("%d secondes et %d millisecondes", seconds, millisecondes)
        } else {
            message = String.format(
                "%d heures, %d minutes, %d secondes et %d millisecondes",
                hours,
                minutes,
                seconds,
                millisecondes
            )
        }
    return message
}