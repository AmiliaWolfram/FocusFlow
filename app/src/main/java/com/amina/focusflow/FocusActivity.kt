package com.amina.focusflow

// Focus session: таймер + данные от light sensor и accelerometer.
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class FocusActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager;
    private var accel: Sensor? = null;
    private var light: Sensor? = null;
    private var lastMoveLog = 0L;
    private var lightLevel = -1f;
    private var sensorStatus: TextView? = null
    private var running = false;
    private var left = 0;
    private var total = 0;
    private var distractions = 0;
    private lateinit var timer: TextView;
    private lateinit var task: Task;
    private val h = Handler(Looper.getMainLooper())
    private val tick = object: Runnable {
        override fun run() {
            if (running && left>0) {
                left--;
                showTime();
                h.postDelayed(this, 1000)
            }
            else if (running)finishSession()
        }
    }
    override fun onCreate(b: Bundle?) {
        super.onCreate(b);
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager;
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        val id = intent.getLongExtra("TASK_ID", -1);
        task = DataManager.loadTasks(this).find {
            it.id == id
        }
        ?: run {
            showTaskPicker();
            return
        }
        ;
        total = task.duration*60;
        left = total;
        render()
    }
    private fun showTaskPicker() {
        val root = FrameLayout(this);
        val(sc, body) = Ui.screen(this, botanical = false);
        Ui.header(body,"Start focus","Choose one task to give your attention to.")
        val tasks = DataManager.loadTasks(this).filter {
            !it.completed
        }
        .sortedWith(compareBy<Task> {
            it.date
        }
        .thenBy {
            it.time
        }
        )
        if (tasks.isEmpty()) {
            val empty = Ui.card(this);
            empty.addView(Ui.text(this,"No active tasks", 16f, true));
            empty.addView(Ui.text(this,"Create a task first, then start a focused session.", 12f, false, Ui.muted), Ui.lp(Ui.dp(this, 6)));
            body.addView(empty, Ui.lp(Ui.dp(this, 18)));
            val add = Ui.button(this,"Create task", Ui.orange);
            add.setOnClickListener {
                startActivity(android.content.Intent(this, TaskActivity::class.java))
            }
            ;
            body.addView(add, Ui.lp(Ui.dp(this, 12)))
        }
        else tasks.forEach {
            t -> val c = Ui.card(this);
            val r = Ui.row(this);
            r.addView(Ui.icon(this, Ui.categoryIcon(t.category), Ui.green, 22));
            r.addView(Ui.spacer(this, 12));
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL;
                addView(Ui.text(this@FocusActivity, t.title, 15f, true));
                addView(Ui.text(this@FocusActivity,"${t.category} · ${t.duration} min", 12f, false, Ui.muted), Ui.lp(Ui.dp(this@FocusActivity, 4)))
            }
            ;
            r.addView(col, LinearLayout.LayoutParams(0, -2, 1f));
            r.addView(Ui.icon(this, R.drawable.ic_play, Ui.orange, 20));
            c.addView(r);
            c.setOnClickListener {
                startActivity(android.content.Intent(this, FocusActivity::class.java).putExtra("TASK_ID", t.id));
                finish()
            }
            ;
            body.addView(c, Ui.lp(Ui.dp(this, 10)))
        }
        Ui.attachNav(root, sc, this,"Focus");
        setContentView(root)
    }
    private fun render() {
        val(sc, body) = Ui.screen(this, botanical = false);
        body.gravity = Gravity.CENTER_HORIZONTAL
        val top = Ui.row(this);
        val back = Ui.icon(this, R.drawable.ic_back, Ui.dark, 24);
        back.setOnClickListener {
            finish()
        }
        ;
        top.addView(back);
        top.addView(Ui.spacer(this, 14));
        top.addView(Ui.text(this,"Focus session", 22f, true), LinearLayout.LayoutParams(0, -2, 1f));
        body.addView(top, LinearLayout.LayoutParams(-1, -2));
        body.addView(Ui.text(this, task.title, 13f, false, Ui.muted).apply {
            gravity = Gravity.CENTER
        }
        , Ui.lp(Ui.dp(this, 20)))
        val circle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL;
                setColor(Ui.surface);
                setStroke(Ui.dp(this@FocusActivity, 10), Ui.sage)
            }
        }
        ;
        timer = Ui.text(this,"", 42f, true).apply {
            gravity = Gravity.CENTER
        }
        ;
        circle.addView(timer, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        val sub = Ui.text(this,"focus time", 12f, false, Ui.muted).apply {
            gravity = Gravity.CENTER
        }
        ;
        circle.addView(sub, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
            topMargin = Ui.dp(this@FocusActivity, 62)
        }
        );
        body.addView(circle, LinearLayout.LayoutParams(Ui.dp(this, 245), Ui.dp(this, 245)).apply {
            topMargin = Ui.dp(this@FocusActivity, 32)
        }
        );
        showTime()
        val controls = Ui.row(this);
        val start = Ui.button(this,"Start", Ui.green);
        start.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
        start.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.WHITE);
        start.compoundDrawablePadding = Ui.dp(this, 8);
        val finish = Ui.button(this,"Finish", Ui.orange);
        finish.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0);
        finish.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.WHITE);
        finish.compoundDrawablePadding = Ui.dp(this, 8);
        controls.addView(start, LinearLayout.LayoutParams(0, -2, 1f));
        controls.addView(finish, LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = Ui.dp(this@FocusActivity, 10)
        }
        );
        body.addView(controls, Ui.lp(Ui.dp(this, 28)));
        start.setOnClickListener {
            running = !running;
            start.text = if (running)"Pause" else"Resume";
            start.setCompoundDrawablesWithIntrinsicBounds(if (running)R.drawable.ic_pause else R.drawable.ic_play, 0, 0, 0);
            start.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Color.WHITE);
            if (running)h.post(tick)
        }
        ;
        finish.setOnClickListener {
            finishSession()
        }
        val calm = Ui.card(this, Ui.surface, 22);
        calm.addView(Ui.text(this,"Stay with one thing", 15f, true));
        calm.addView(Ui.text(this,"Keep the phone still and let this session be enough.", 12f, false, Ui.muted), Ui.lp(Ui.dp(this, 6)));
        body.addView(calm, Ui.lp(Ui.dp(this, 28)));
        sensorStatus = Ui.text(this,"Focus environment · light: measuring · movement: calm", 11.5f, false, Ui.muted).apply {
            gravity = Gravity.CENTER
        }
        ;
        body.addView(sensorStatus, Ui.lp(Ui.dp(this, 10)))
        val dist = Ui.button(this,"I got distracted", Ui.surface).apply {
            setTextColor(Ui.orange);
            background = Ui.bg(Ui.surface, Ui.dp(this@FocusActivity, 18).toFloat(), 1, Ui.line);
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alert, 0, 0, 0);
            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(Ui.orange);
            compoundDrawablePadding = Ui.dp(this@FocusActivity, 8)
        }
        ;
        dist.setOnClickListener {
            distractions++;
            Toast.makeText(this@FocusActivity,"Distractions: $distractions", Toast.LENGTH_SHORT).show()
        }
        ;
        body.addView(dist, Ui.lp(Ui.dp(this, 12)));
        setContentView(sc)
    }
    private fun showTime() {
        timer.text ="%02d:%02d".format(left/60, left%60)
    }
    private fun finishSession() {
        running = false;
        h.removeCallbacks(tick);
        val elapsed = total-left;
        val used = elapsed/60;
        if (used>0) {
            DataManager.addSession(this, task.id, used, distractions);
            Toast.makeText(this,"$used focused minutes saved", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this,"Session was too short to save", Toast.LENGTH_SHORT).show()
        }
        ;
        finish()
    }
    override fun onResume() {
        super.onResume();
        accel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        ;
        light?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    override fun onPause() {
        sensorManager.unregisterListener(this);
        super.onPause()
    }
// Light sensor показывает освещение, accelerometer использую только для заметного движения телефона.
    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type == Sensor.TYPE_LIGHT) {
            lightLevel = e.values[0];
            updateSensorText(false)
        }
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER && running) {
            val x = e.values[0];
            val y = e.values[1];
            val z = e.values[2];
            val g = sqrt(x*x+y*y+z*z)/SensorManager.GRAVITY_EARTH;
            val now = System.currentTimeMillis();
            if (g>1.45f && now-lastMoveLog>8000) {
// Threshold + cooldown: одно движение не должно считаться несколько раз.
                distractions++;
                lastMoveLog = now;
                updateSensorText(true)
            }
        }
    }
    private fun updateSensorText(moved: Boolean) {
        val lightText = if (lightLevel<0)"measuring" else"${lightLevel.toInt()} lux";
        sensorStatus?.text ="Focus environment · light: $lightText · movement: ${if(moved)"detected" else "calm"}"
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    override fun onDestroy() {
        h.removeCallbacks(tick);
        sensorManager.unregisterListener(this);
        super.onDestroy()
    }
}
