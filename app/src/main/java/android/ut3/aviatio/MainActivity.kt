package android.ut3.aviatio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat

import android.os.Vibrator
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import android.os.Build
import android.os.VibrationEffect
import android.hardware.SensorManager
import android.widget.Toast


class MainActivity : AppCompatActivity(), SensorEventListener {

    val PERMISSION_ID = 42

    val SENSITIVITY = 67.5 // TODO

    private lateinit var mSensorManager: SensorManager;
    private var mProximity: Sensor? = null;
    private lateinit var mLight: Sensor;
    private val SENSOR_SENSITIVITY = 4
    private lateinit var vibrator: Vibrator;

    private var rp: Float = -1f
    private var rl: Float =-1f;

    private val LIGHT_POCKET_TRESHOLD = 30f;
    private val PROX_POCKET_TRESHOLD = 1f;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
        if (!checkPermissions()) {
            onStop();
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        setUpAmbientSongListener();

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    override fun onResume() {
        super.onResume()
        if (mProximity != null) {
            mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    private fun vibrate() {
        val pattern: LongArray = longArrayOf(0,200,0)
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(pattern,0);
        } else {
            fallbackEmitSong();
        }
    }

    private fun fallbackEmitSong() {
        // TODO
    }

    private fun setUpAmbientSongListener() {
        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

        val threshold = 8.0
        val sensitivity = SENSITIVITY

        val mPercussionDetector = PercussionOnsetDetector(22050f, 1024,
            object : OnsetHandler {

                override fun handleOnset(time: Double, salience: Double) {
                    println("Clap detected!")
                    vibrate();
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

        if((rp!=-1f) && (rl!=-1f)){
            detect(rp, rl);
        }
    }

    private fun detect(prox: Float, light: Float) {
        if ((prox < PROX_POCKET_TRESHOLD) && (light < LIGHT_POCKET_TRESHOLD)) {
            println("In the pocket")
        } else if (prox >= PROX_POCKET_TRESHOLD && light >= LIGHT_POCKET_TRESHOLD) {
            println("Not in the pocket")
        }
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
