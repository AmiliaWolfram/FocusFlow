package com.amina.focusflow

// Home screen: короткая сводка по задачам, focus time и streak.
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        render()
    }
    override fun onResume() {
        super.onResume();
        render()
    }
    private fun render() {
        val root = FrameLayout(this)
// Botanical background оставляю только здесь — на остальных экранах он отвлекал.
        val backgroundArt = ImageView(this).apply {
            setImageResource(R.drawable.home_botanical_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.78f
        }
        root.addView(backgroundArt, FrameLayout.LayoutParams(-1, -1))
        val (sc, body) = Ui.screen(this)
        sc.setBackgroundColor(Color.TRANSPARENT)
        body.setBackgroundColor(Color.TRANSPARENT)
// Ui.screen() already handles system insets, поэтому второй padding здесь не нужен.
        val allTasks = DataManager.loadTasks(this)
        val today = allTasks.filter {
            !it.completed && (it.date.isBlank() || it.date == DataManager.today())
        }
        val next = today.firstOrNull()
        body.addView(buildHeader())
        body.addView(buildHero(next), Ui.lp(Ui.dp(this, 10)))
        body.addView(buildQuickStats(allTasks), Ui.lp(Ui.dp(this, 14)))
        body.addView(Ui.sectionTitle(this,"Today","See all") {
            startActivity(Intent(this, TasksActivity::class.java))
        }
        , Ui.lp(Ui.dp(this, 25)))
        body.addView(buildTodayList(today), Ui.lp(Ui.dp(this, 10)))
        body.addView(Ui.sectionTitle(this,"This week","Insights") {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        , Ui.lp(Ui.dp(this, 24)))
        body.addView(buildWeekCard(), Ui.lp(Ui.dp(this, 10)))
        Ui.attachNav(root, sc, this,"Home")
        setContentView(root)
    }
    private fun buildHeader(): FrameLayout {
        val frame = FrameLayout(this)
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val greeting = Ui.row(this)
        greeting.addView(Ui.icon(this, R.drawable.ic_sun, Ui.orange, 24))
        greeting.addView(Ui.spacer(this, 10))
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetingText = when (hour) {
            in 5..11 ->"Good morning,"
            in 12..16 ->"Good afternoon,"
            in 17..21 ->"Good evening,"
            else ->"Good night,"
        }
        greeting.addView(Ui.serif(this, greetingText, 19f, false, Color.parseColor("#65716A")))
        copy.addView(greeting)
        copy.addView(Ui.serif(this,"Amina", 43f, true), Ui.lp(Ui.dp(this, 0)))
        copy.addView(Ui.serif(this,"Make today count.", 17f, false, Color.parseColor("#65716A")), Ui.lp(Ui.dp(this, 0)))
        frame.addView(copy, FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.TOP))
        frame.layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(this, 112))
        return frame
    }
    private fun buildHero(next: Task?): FrameLayout {
        val hero = FrameLayout(this).apply {
            background = Ui.bg(Color.parseColor("#145C43"), Ui.dp(this@MainActivity, 28).toFloat())
            clipToOutline = true
        }
        hero.addView(HeroPatternView(this), FrameLayout.LayoutParams(-1, -1))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 17), Ui.dp(this@MainActivity, 22), Ui.dp(this@MainActivity, 17))
        }
        val top = Ui.row(this)
        top.addView(Ui.text(this,"TODAY’S FOCUS", 12f, true, Color.WHITE).apply {
            letterSpacing = .14f
        }
        , LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(Ui.icon(this, R.drawable.ic_target, Color.parseColor("#8DB09D"), 36))
        content.addView(top)
        content.addView(Ui.serif(this, next?.title ?:"Choose your focus", 32f, true, Color.WHITE), Ui.lp(Ui.dp(this, 2)))
        val meta = Ui.row(this)
        if (next != null) {
            val circle = FrameLayout(this).apply {
                background = Ui.bg(Color.parseColor("#3D7A60"), Ui.dp(this@MainActivity, 17).toFloat())
                addView(Ui.icon(this@MainActivity, Ui.categoryIcon(next.category), Color.WHITE, 18), FrameLayout.LayoutParams(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 18), Gravity.CENTER))
            }
            meta.addView(circle, LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)))
            meta.addView(Ui.spacer(this, 11))
            meta.addView(Ui.text(this,"${next.category}  ·  ${next.duration} min", 16f, false, Color.WHITE))
        }
        else {
            meta.addView(Ui.text(this,"Pick one meaningful thing and begin.", 15f, false, Color.parseColor("#EAF2EC")))
        }
        content.addView(meta, Ui.lp(Ui.dp(this, 4)))
        val pct = next?.let {
            (it.focusedMinutes * 100 / it.duration.coerceAtLeast(1)).coerceIn(0, 100)
        }
        ?: 0
        val progressRow = Ui.row(this)
        val progress = Ui.progress(this, pct, Ui.white).apply {
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#4C846C"))
        }
        progressRow.addView(progress, LinearLayout.LayoutParams(0, Ui.dp(this, 8), 1f))
        progressRow.addView(Ui.spacer(this, 14))
        progressRow.addView(Ui.text(this,"$pct%", 14f, true, Color.WHITE))
        content.addView(progressRow, Ui.lp(Ui.dp(this, 11)))
        val go = Ui.button(this, if (next == null)"Add a task" else"Start focus", Ui.orange).apply {
            textSize = 16f
            setPadding(Ui.dp(this@MainActivity, 18), 0, Ui.dp(this@MainActivity, 18), 0)
            setCompoundDrawablesWithIntrinsicBounds(if (next == null) R.drawable.ic_add else R.drawable.ic_play, 0, 0, 0)
            compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
            compoundDrawablePadding = Ui.dp(this@MainActivity, 12)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, if (next == null) TaskActivity::class.java else FocusActivity::class.java).apply {
                    next?.let {
                        putExtra("TASK_ID", it.id)
                    }
                }
                )
            }
        }
        content.addView(go, LinearLayout.LayoutParams(-1, Ui.dp(this, 54)).apply {
            topMargin = Ui.dp(this@MainActivity, 12)
        }
        )
        hero.addView(content, FrameLayout.LayoutParams(-1, -2))
        return hero
    }
    private fun buildQuickStats(tasks: List<Task>): LinearLayout {
        val stats = Ui.row(this).apply {
            background = Ui.bg(Ui.surface, Ui.dp(this@MainActivity, 24).toFloat(), 1, Ui.line)
            setPadding(
            Ui.dp(this@MainActivity, 8),
            Ui.dp(this@MainActivity, 10),
            Ui.dp(this@MainActivity, 8),
            Ui.dp(this@MainActivity, 10)
            )
            gravity = Gravity.CENTER_VERTICAL
        }
        val streak = DataManager.streak(this)
        val focused = DataManager.totalFocus(this)
        val completed = tasks.count {
            it.completed
        }
// Keep metric labels short so they do not wrap on smaller screens.
        val data = listOf(
        Triple(R.drawable.ic_flame, streak.toString(), if (streak == 1)"day" else"days"),
        Triple(R.drawable.ic_clock, focused.toString(),"min"),
        Triple(R.drawable.ic_check, completed.toString(), if (completed == 1)"task" else"tasks")
        )
        data.forEachIndexed {
            i, (ico, number, label) ->
            if (i > 0) {
                stats.addView(
                View(this).apply {
                    setBackgroundColor(Ui.line)
                }
                ,
                LinearLayout.LayoutParams(1, Ui.dp(this@MainActivity, 34)).apply {
                    marginStart = Ui.dp(this@MainActivity, 3)
                    marginEnd = Ui.dp(this@MainActivity, 3)
                }
                )
            }
            val item = Ui.row(this).apply {
                gravity = Gravity.CENTER
                setPadding(Ui.dp(this@MainActivity, 4), 0, Ui.dp(this@MainActivity, 4), 0)
            }
            val circle = FrameLayout(this).apply {
                background = Ui.bg(
                if (i == 0) Ui.sand else Ui.paleGreen,
                Ui.dp(this@MainActivity, 18).toFloat()
                )
                addView(
                Ui.icon(this@MainActivity, ico, if (i == 0) Ui.orange else Ui.green, 17),
                FrameLayout.LayoutParams(
                Ui.dp(this@MainActivity, 17),
                Ui.dp(this@MainActivity, 17),
                Gravity.CENTER
                )
                )
            }
            item.addView(circle, LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)))
            item.addView(Ui.spacer(this, 7))
            val phrase = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            phrase.addView(Ui.serif(this, number, 18f, true))
            phrase.addView(Ui.spacer(this, 4))
            phrase.addView(Ui.text(this, label, 11f, false, Ui.muted).apply {
                maxLines = 1
                isSingleLine = true
            }
            )
            item.addView(phrase)
            stats.addView(item, LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f))
        }
        return stats
    }
    private fun buildTodayList(today: List<Task>): LinearLayout {
        val list = Ui.card(this, Ui.surface, 25).apply {
            setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 6), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 6))
        }
        if (today.isEmpty()) {
            list.addView(Ui.text(this,"Your day is open", 15f, true).apply {
                gravity = Gravity.CENTER;
                setPadding(0, Ui.dp(this@MainActivity, 19), 0, 0)
            }
            )
            list.addView(Ui.text(this,"Add a task when you’re ready.", 13f, false, Ui.muted).apply {
                gravity = Gravity.CENTER;
                setPadding(0, Ui.dp(this@MainActivity, 5), 0, Ui.dp(this@MainActivity, 19))
            }
            )
        }
        else {
            today.take(3).forEachIndexed {
                i, task ->
                if (i > 0) list.addView(View(this).apply {
                    setBackgroundColor(Ui.line)
                }
                , LinearLayout.LayoutParams(-1, 1).apply {
                    marginStart = Ui.dp(this@MainActivity, 62)
                }
                )
                list.addView(taskRow(task))
            }
        }
        return list
    }
    private fun taskRow(t: Task): LinearLayout {
        val r = Ui.row(this).apply {
            setPadding(0, Ui.dp(this@MainActivity, 9), 0, Ui.dp(this@MainActivity, 9))
            setOnClickListener {
                startActivity(Intent(this@MainActivity, TaskActivity::class.java).putExtra("TASK_ID", t.id))
            }
        }
        val tileColor = when {
            t.category.contains("sport", true) -> Color.parseColor("#E1EBDD")
            t.category.contains("personal", true) -> Color.parseColor("#FFF0E2")
            else -> Color.parseColor("#E4EFF2")
        }
        val ib = FrameLayout(this).apply {
            background = Ui.bg(tileColor, Ui.dp(this@MainActivity, 15).toFloat())
            addView(Ui.icon(this@MainActivity, Ui.categoryIcon(t.category), Ui.dark, 21), FrameLayout.LayoutParams(Ui.dp(this@MainActivity, 21), Ui.dp(this@MainActivity, 21), Gravity.CENTER))
        }
        r.addView(ib, LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)))
        r.addView(Ui.spacer(this, 12))
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Ui.text(this@MainActivity, t.title, 15f, true))
            addView(Ui.text(this@MainActivity,"${t.category}  ·  ${t.time.ifBlank { "Any time" }}", 12f, false, Ui.muted), Ui.lp(Ui.dp(this@MainActivity, 4)))
        }
        r.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(Ui.chip(this,"${t.duration} min", Color.parseColor("#F0F1EC"), Ui.dark))
        r.addView(Ui.spacer(this, 7))
        r.addView(Ui.icon(this, R.drawable.ic_chevron, Color.parseColor("#66706A"), 16))
        return r
    }
    private fun buildWeekCard(): LinearLayout {
        val week = Ui.card(this, Ui.surface, 25).apply {
            setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 15))
        }
        val row = Ui.row(this)
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val scoreInfo = Ui.row(this@MainActivity)
            scoreInfo.addView(Ui.text(this@MainActivity,"Focus score", 13f, true))
            scoreInfo.addView(Ui.text(this@MainActivity,"  ⓘ", 13f, true, Ui.muted).apply {
                setPadding(Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 4), Ui.dp(this@MainActivity, 8), Ui.dp(this@MainActivity, 4))
                setOnClickListener {
                    Ui.showFocusScoreInfo(this@MainActivity, DataManager.focusScoreBreakdown(this@MainActivity))
                }
            }
            )
            addView(scoreInfo)
            val score = Ui.row(this@MainActivity)
            score.addView(Ui.serif(this@MainActivity, DataManager.productivityScore(this@MainActivity).toString(), 38f, true))
            score.addView(Ui.text(this@MainActivity," / 100", 18f, false, Ui.muted))
            addView(score, Ui.lp(Ui.dp(this@MainActivity, 5)))
        }
        row.addView(left, LinearLayout.LayoutParams(0, -2, .82f))
        row.addView(WeekBarsView(this), LinearLayout.LayoutParams(0, Ui.dp(this, 78), 1.18f))
        week.addView(row)
        week.addView(View(this).apply {
            setBackgroundColor(Ui.line)
        }
        , LinearLayout.LayoutParams(-1, 1).apply {
            topMargin = Ui.dp(this@MainActivity, 13)
        }
        )
        val foot = Ui.row(this).apply {
            setPadding(0, Ui.dp(this@MainActivity, 13), 0, 0)
        }
        foot.addView(Ui.icon(this, R.drawable.ic_clock, Ui.green, 18))
        foot.addView(Ui.text(this,"  ${DataManager.totalFocus(this)} min focused", 12f, false), LinearLayout.LayoutParams(0, -2, 1f))
        foot.addView(Ui.icon(this, R.drawable.ic_flame, Ui.orange, 18))
        foot.addView(Ui.text(this,"  ${DataManager.streak(this)} day streak", 12f, false))
        week.addView(foot)
        return week
    }
}
private class BotanicalView(context: Context) : View(context) {
    private val stem = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#78906F");
        strokeWidth = 3f;
        style = Paint.Style.STROKE;
        strokeCap = Paint.Cap.ROUND
    }
    private val leaf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A7B59A");
        style = Paint.Style.FILL
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat();
        val h = height.toFloat()
        val p = Path().apply {
            moveTo(w*.95f, h*.98f);
            cubicTo(w*.82f, h*.65f, w*.62f, h*.42f, w*.32f, h*.15f)
        }
        c.drawPath(p, stem)
        drawLeaf(c, w*.72f, h*.57f, 44f, 19f, -28f);
        drawLeaf(c, w*.60f, h*.43f, 50f, 21f, 18f)
        drawLeaf(c, w*.50f, h*.31f, 43f, 18f, -32f);
        drawLeaf(c, w*.38f, h*.19f, 47f, 19f, 16f)
        drawLeaf(c, w*.83f, h*.72f, 39f, 17f, 16f);
        drawLeaf(c, w*.88f, h*.82f, 45f, 18f, -28f)
    }
    private fun drawLeaf(c: Canvas, x: Float, y: Float, l: Float, ww: Float, deg: Float) {
        c.save();
        c.rotate(deg, x, y);
        val p = Path().apply {
            moveTo(x-l/2, y);
            quadTo(x, y-ww, x+l/2, y);
            quadTo(x, y+ww, x-l/2, y);
            close()
        }
        ;
        c.drawPath(p, leaf);
        c.restore()
    }
}
private class HeroPatternView(context: Context) : View(context) {
    private val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0E4C38");
        alpha = 120
    }
    private val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#27694F");
        alpha = 80
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c);
        val w = width.toFloat();
        val h = height.toFloat();
        c.drawOval(RectF(w*.55f, -h*.22f, w*1.16f, h*.58f), p2);
        c.drawOval(RectF(w*.63f, h*.35f, w*1.22f, h*1.16f), p1)
    }
}
private class WeekBarsView(context: Context): View(context) {
    private val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DFE8DA")
    }
    private val active = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6828")
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#737B76");
        textSize = Ui.dp(context, 10).toFloat();
        textAlign = Paint.Align.CENTER;
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val values = intArrayOf(24, 38, 32, 72, 28, 45, 55);
    private val days = arrayOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    override fun onDraw(c: Canvas) {
        super.onDraw(c);
        val gap = width/7f;
        val maxH = height*.56f;
        for (i in 0..6) {
            val x = gap*(i+.5f);
            val bh = maxH*(values[i]/72f);
            val top = height*.62f-bh;
            val r = Ui.dp(context, 6).toFloat();
            c.drawRoundRect(x-Ui.dp(context, 7), top, x+Ui.dp(context, 7), height*.62f, r, r, if (i == 3)active else normal);
            label.typeface = Typeface.create("sans-serif", if (i == 3)Typeface.BOLD else Typeface.NORMAL);
            label.color = if (i == 3)Color.parseColor("#26342A") else Color.parseColor("#737B76");
            c.drawText(days[i], x, height*.90f, label)
        }
    }
}
