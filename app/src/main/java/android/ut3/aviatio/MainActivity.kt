package android.ut3.aviatio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    val PERMISSION_ID = 42

    val SOUND_SENSITIVITY = 50.0 // TODO

    private lateinit var mSensorManager: SensorManager;
    private var mProximity: Sensor? = null;
    private lateinit var mLight: Sensor;
    private val SENSOR_SENSITIVITY = 4
    private lateinit var vibrator: Vibrator;
    private var mediaPlayer: MediaPlayer? = null;

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var timerStartTime: Long = 0
    private val handler: Handler = Handler()

    private var rp: Float = -1f
    private var rl: Float =-1f;

    private val LIGHT_POCKET_TRESHOLD = 30f;
    private val PROX_POCKET_TRESHOLD = 1f;

    private lateinit var stopButton: Button;
    private lateinit var timerTv: TextView
    private lateinit var phoneInPocketTv: TextView

    private var alertIsStarted: Boolean = false;
    private var isInThePocket: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
        while (!checkPermissions()) {
            // onStop();
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        setUpAmbientSongListener();
        stopButton = findViewById(R.id.stopBtn);
        timerTv = findViewById(R.id.timerTv)
        phoneInPocketTv = findViewById(R.id.phoneInPocketTv)
        phoneInPocketTv.visibility = View.VISIBLE
        stopButton.isEnabled = false;
        stopButton.setOnClickListener {
            stopAlert()
        }
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mProximity == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert);
            mediaPlayer!!.isLooping = true
        }
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    override fun onResume() {
        super.onResume()
        if (mProximity != null) {
            mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
        initializeTimerTask()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    private fun startAlert() {
        if (!alertIsStarted && isInThePocket) {
            val pattern: LongArray = longArrayOf(0,200,0)
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(pattern,0);
            } else {
                fallbackEmitSong();
            }
            startTimer()
            runOnUiThread {
                stopButton.isEnabled = true
            }
            alertIsStarted = true;
        }
    }

    private fun fallbackEmitSong() {
        mediaPlayer?.start();
    }

    private fun setUpAmbientSongListener() {
        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        val threshold = 8.0
        val sensitivity = SOUND_SENSITIVITY

        val mPercussionDetector = PercussionOnsetDetector(22050f, 1024,
            object : OnsetHandler {
                override fun handleOnset(time: Double, salience: Double) {
                    println("Clap detected!")
                    startAlert();
                }
            }, sensitivity, threshold
        )

        dispatcher.addAudioProcessor(mPercussionDetector)
        Thread(dispatcher, "Audio Dispatcher").start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            rp = event.values[0];
           println("Prox: " + rp);
        }

        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            println("Light : " + event.values[0]);
            rl=event.values[0];
        }
        if((rp!=-1f || mProximity == null) && (rl!=-1f)){
            detect(rp, rl);
        }
    }

    private fun detect(prox: Float, light: Float) {
        if ((prox < PROX_POCKET_TRESHOLD || mProximity == null) && (light < LIGHT_POCKET_TRESHOLD)) {
            isInThePocket = true;
        } else if ((prox >= PROX_POCKET_TRESHOLD || mProximity == null) && light >= LIGHT_POCKET_TRESHOLD) {
            isInThePocket = false;
        }
        phoneInPocketTv.visibility = if (isInThePocket || alertIsStarted) View.GONE else View.VISIBLE
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE
            ),
            PERMISSION_ID
        )
    }

    private fun stopAlert() {
        if (alertIsStarted && stopButton.isEnabled) {
            stoptimertask()
            if (vibrator.hasVibrator()) {
                vibrator.cancel();
            } else {
                mediaPlayer?.pause();
            }
            runOnUiThread {
                stopButton.isEnabled = false
            }
            alertIsStarted = false;
        }
    }

    fun startTimer() {
        if (timer == null) {
            timerStartTime = System.currentTimeMillis()
            timer = Timer()
            initializeTimerTask()
            timer!!.schedule(timerTask, 0, 30)
        }
    }

    fun stoptimertask() { //stop the timer, if it's not already null
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    var millis: Long = (System.currentTimeMillis() - timerStartTime)
                    timerTv.setText(
                        (SimpleDateFormat("ss:SSS")).format(Date(millis))
                    )
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }
}
