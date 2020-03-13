package android.ut3.aviatio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import java.text.SimpleDateFormat
import java.util.*
import android.ut3.aviatio.di.scoreRepository
import android.ut3.aviatio.di.scoreViewModel
import android.ut3.aviatio.model.GameState
import android.widget.RelativeLayout
import org.koin.core.context.startKoin

class MainActivity : AppCompatActivity(), SensorEventListener {

    val PERMISSION_ID = 42

    val SOUND_SENSITIVITY = 67.5 // TODO

    private var state: GameState = GameState.WAITING;

    private lateinit var mSensorManager: SensorManager;
    private var mProximity: Sensor? = null;
    private lateinit var mLight: Sensor;
    private val SENSOR_SENSITIVITY = 4
    private lateinit var vibrator: Vibrator;
    private var mediaPlayer: MediaPlayer? = null;
    private lateinit var startGameWrapper: RelativeLayout

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var timerStartTime: Long = 0
    private val handler: Handler = Handler()

    private var rp: Float = -1f
    private var rl: Float =-1f;

    private val LIGHT_POCKET_TRESHOLD = 30f;
    private val PROX_POCKET_TRESHOLD = 1f;

    private lateinit var startGameButton: Button;
    private lateinit var timerTv: TextView
    private lateinit var phoneInPocketTv: TextView

    private var alertIsStarted: Boolean = false;
    private var isInThePocket: Boolean = false

    private lateinit var textureView: TextureView;
    private var canvas: Canvas? = null;
    private var gameTimer: Timer? = null;
    private var bullets: MutableList<Bullet> = ArrayList()

    private var isDrawing: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startKoin {
            modules(
                listOf(
                    scoreViewModel,
                    scoreRepository
                )
            )
        }
//        startActivity(Intent(this, ShowScoresActivity::class.java))
////        val intent = Intent(this, VictoryActivity::class.java)
////        intent.putExtra("score", 2500)
////        startActivity(intent)
//        finish()

        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        requestPermissions()
        while (!checkPermissions()) {
            // onStop();
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        setUpAmbientSongListener();
        startGameButton = findViewById(R.id.startGame);
        timerTv = findViewById(R.id.timerTv)
        textureView = findViewById(R.id.textureView)
        phoneInPocketTv = findViewById(R.id.logTv)
        startGameWrapper = findViewById(R.id.startGameWrapper)
        phoneInPocketTv.visibility = View.VISIBLE
        startGameButton.isEnabled = false;
        startGameButton.setOnClickListener {
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

    private fun changePos() {
        isDrawing = true;
        canvas = textureView.lockCanvas();
        if (canvas != null) {
            if (bullets.size < 10) {
                generateBullets(10 - bullets.size, canvas!!.height * 1f, canvas!!.width * 1f)
            }
            drawBullets(canvas!!)
        }
        textureView.unlockCanvasAndPost(canvas);
        isDrawing = false;
    }

    private fun generateBullets(size: Int, maxHeight: Float, maxWidth: Float) {
        var random = Random();
        repeat(size) {
            bullets.add(Bullet(
                random.nextFloat() * maxWidth,
                (random.nextFloat() * maxHeight) - (maxHeight / 4) * 3
            ))
        }
    }

    private fun drawBullets(canvas: Canvas) {
        var paint = Paint();
        paint.setColor(Color.BLACK)
        paint.strokeWidth = 50f
        canvas.drawColor(resources.getColor(R.color.colorWhite))
        for (bullet in bullets) {
            bullet.y += 12
            canvas?.drawCircle(bullet.x,bullet.y, 50f, paint)
        }
        removeInvisible(canvas);
    }

    private fun removeInvisible(canvas: Canvas) {
        bullets = bullets.filter { b -> b.y < canvas.height }.toMutableList();
    }

    override fun onPause() {
        super.onPause()
        if (timer != null) {
            timer!!.cancel()
        }
        if (gameTimer != null) {
            gameTimer!!.cancel()
        }
        mSensorManager.unregisterListener(this)
    }

    private fun startGame() {
        if (state == GameState.WAITING_FOR_CLICK_GAME) {
            if (gameTimer != null) {
                gameTimer!!.cancel()
                gameTimer = null
            }
            if (gameTimer == null) {
                gameTimer = Timer()
                isDrawing = false
                gameTimer!!.schedule(object : TimerTask() {
                    override fun run() {
                        if (!isDrawing) {
                            changePos()
                        }
                    }
                }, 0, 20)
            }
            state = GameState.PLAYING;
        }
    }

    private fun startAlert() {
        if (state == GameState.IN_POCKET) {
            val pattern: LongArray = longArrayOf(0,200,0)
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(pattern,0);
            } else {
                fallbackEmitSong();
            }
            startTimer()
            runOnUiThread {
                startGameButton.isEnabled = true
            }
            state = GameState.WAITING_FOR_CLICK_GAME;
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
        }

        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            rl=event.values[0];
        }
        if((rp!=-1f || mProximity == null) && (rl!=-1f)){
            checkIfInPocket(rp, rl);
        }
    }

    private fun checkIfInPocket(prox: Float, light: Float) {
        if ((prox < PROX_POCKET_TRESHOLD || mProximity == null) && (light < LIGHT_POCKET_TRESHOLD)) {
            isInThePocket = true;
        } else if ((prox >= PROX_POCKET_TRESHOLD || mProximity == null) && light >= LIGHT_POCKET_TRESHOLD) {
            isInThePocket = false;
        }
        when (state) {
            GameState.WAITING -> if (isInThePocket) state = GameState.IN_POCKET
            GameState.IN_POCKET -> state = if (isInThePocket) GameState.IN_POCKET else GameState.WAITING
            GameState.PLAYING, GameState.WAITING_FOR_CLICK_GAME ->  {
                // nothing
            }
        }
        phoneInPocketTv.visibility = if (state == GameState.WAITING) View.VISIBLE else View.GONE
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

    private fun endGame() {
        if (state == GameState.PLAYING) {
            stoptimertask()
            gameTimer!!.cancel()
            var scoreInMillis: Long = (System.currentTimeMillis() - timerStartTime)
            // todo open activity with score
            state = GameState.WAITING
            phoneInPocketTv.visibility = if (state == GameState.WAITING) View.VISIBLE else View.GONE
            startGameWrapper.visibility = View.VISIBLE
        }
    }

    private fun stopAlert() {
        if (state == GameState.WAITING_FOR_CLICK_GAME && startGameButton.isEnabled) {
            if (vibrator.hasVibrator()) {
                vibrator.cancel();
            } else {
                mediaPlayer?.pause();
            }
            runOnUiThread {
                startGameButton.isEnabled = false
            }
            startGameWrapper.visibility = View.GONE
            startGame()
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
