package android.ut3.aviatio.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.ut3.aviatio.R
import android.ut3.aviatio.model.Bullet
import android.ut3.aviatio.model.GameState
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import be.tarsos.dsp.AudioDispatcher

class LaunchGameActivity : AppCompatActivity(), SensorEventListener {

    val SOUND_SENSITIVITY = 70.0 // TODO

    val BULLET_SIZE = 80;

    val MAX_SCORE = 10

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
    private var rl: Float = -1f;

    private val LIGHT_POCKET_TRESHOLD = 30f;
    private val PROX_POCKET_TRESHOLD = 1f;

    private lateinit var startGameButton: FloatingActionButton;
    private lateinit var timerTv: TextView
    private lateinit var phoneInPocketTv: TextView
    private lateinit var scoreTv: TextView

    private var alertIsStarted: Boolean = false;
    private var isInThePocket: Boolean = false

    private lateinit var textureView: TextureView;
    private var canvas: Canvas? = null;
    private var gameTimer: Timer? = null;
    private var bullets: MutableList<Bullet> = ArrayList()

    private var isDrawing: Boolean = false;

    private var bulletsToDelete: MutableList<Bullet> = ArrayList();
    private var bulletPicture: Bitmap? = null;
    private var isDeleting: Boolean = false;

    private var soundDispatcher: AudioDispatcher? = null
    private var soundThread: Thread? = null

    private var nbTouche: Int = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        setContentView(R.layout.activity_launch_game)


        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        bulletPicture = BitmapFactory.decodeResource(resources, R.drawable.bullet);

        setUpAmbientSongListener();
        startGameButton = findViewById(R.id.startGame);
        timerTv = findViewById(R.id.timerTv)
        textureView = findViewById(R.id.textureView)
        phoneInPocketTv = findViewById(R.id.logTv)
        startGameWrapper = findViewById(R.id.startGameWrapper)
        phoneInPocketTv.visibility = View.VISIBLE
        startGameButton.isEnabled = false;
        scoreTv = findViewById(R.id.scoreTv)
        scoreTv.text = String.format("0/%d", MAX_SCORE);
        startGameButton.visibility = View.GONE
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
        setUpAmbientSongListener();
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL)

        textureView.setOnTouchListener (object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event != null && event!!.action == MotionEvent.ACTION_DOWN) {
                    onGameTouched(event.getX(), event.getY())
                    return true;
                }
                return false;
            }
        });

        initializeTimerTask()
    }

    private fun onGameTouched(x: Float, y: Float) {
        for (bullet in bullets) {
            if (bullet.x < x && x < bullet.x + BULLET_SIZE
                && bullet.y < y && y < bullet.y + BULLET_SIZE
            ) {
                incrementScore();
                return removeBullet(bullet);
            }
        }
    }

    private fun incrementScore() {
        nbTouche = nbTouche + 1;
        scoreTv.text = String.format("%d/%d", nbTouche, MAX_SCORE);
        if (nbTouche >= MAX_SCORE) {
            endGame()
        }
    }

    private fun removeTouched() {
        bullets = bullets.filter { b -> !bulletsToDelete.contains(b)}.toMutableList();
        bulletsToDelete = ArrayList();
    }

    private fun removeBullet(bullet: Bullet) {
        bulletsToDelete.add(bullet);
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
            bullets.add(
                Bullet(
                    random.nextFloat() * maxWidth,
                    (random.nextFloat() * maxHeight) - (maxHeight / 4) * 3
                )
            )
        }
    }

    private fun drawBullets(canvas: Canvas) {
        var paint = Paint();
        paint.setColor(Color.BLACK)
        paint.strokeWidth = 50f
        canvas.drawColor(resources.getColor(R.color.colorWhite))
        for (bullet in bullets) {
            bullet.y = bullet.y + 12
            canvas?.drawBitmap(bulletPicture!! ,null, RectF(bullet.x, bullet.y, bullet.x + BULLET_SIZE, bullet.y + BULLET_SIZE), paint)        }
        removeInvisible(canvas);
        removeTouched();
    }

    private fun removeInvisible(canvas: Canvas) {
        bullets = bullets.filter { b -> b.y < canvas.height }.toMutableList();
    }

    override fun onPause() {
        super.onPause()
        if (timer != null) {
            timer!!.cancel()
        }
        if (mediaPlayer != null) {
            mediaPlayer!!.pause()
        }
        if (gameTimer != null) {
            gameTimer!!.cancel()
        }
        if (soundDispatcher != null) {
            soundDispatcher!!.stop()
            soundDispatcher = null
        }
        if (soundThread != null) {
            soundThread = null
        }
        mSensorManager.unregisterListener(this)
        nbTouche = 0
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
            val pattern: LongArray = longArrayOf(0, 200, 0)
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(pattern, 0);
            } else {
                fallbackEmitSong();
            }
            startTimer()
            runOnUiThread {
                startGameButton.isEnabled = true
                startGameButton.visibility = View.VISIBLE

            }
            state = GameState.WAITING_FOR_CLICK_GAME;
        }
    }

    private fun fallbackEmitSong() {
        mediaPlayer?.start();
    }

    private fun setUpAmbientSongListener() {
        if (soundDispatcher == null && soundThread == null) {
            soundDispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            val threshold = 8.0
            val mPercussionDetector = PercussionOnsetDetector(
                22050f, 1024,
                object : OnsetHandler {
                    override fun handleOnset(time: Double, salience: Double) {
                        startAlert();
                    }
                }, SOUND_SENSITIVITY, threshold
            )
            soundDispatcher!!.addAudioProcessor(mPercussionDetector)
            soundThread = Thread(soundDispatcher, "Audio Dispatcher")
            soundThread!!.start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            rp = event.values[0];
        }

        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            rl = event.values[0];
        }
        if ((rp != -1f || mProximity == null) && (rl != -1f)) {
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
            GameState.IN_POCKET -> state =
                if (isInThePocket) GameState.IN_POCKET else GameState.WAITING
            GameState.PLAYING, GameState.WAITING_FOR_CLICK_GAME -> {
                // nothing
            }
        }
        phoneInPocketTv.visibility = if (state == GameState.WAITING) View.VISIBLE else View.GONE
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
            val intent = Intent(this, VictoryActivity::class.java);
            intent.putExtra("score", scoreInMillis.toInt())
            startActivity(intent)
            finish()

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

}