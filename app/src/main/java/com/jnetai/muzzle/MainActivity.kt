package com.jnetai.muzzle

import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "Muzzle"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/Muzzle"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        scoreText = TextView(this).apply {
            text = "Score: 0  |  Breed: Golden Retriever"
            setTextColor(0xFFFF8844.toInt())
            textSize = 16f
            setPadding(32, 32, 32, 8)
            typeface = Typeface.MONOSPACE
        }

        gameView = GameView(this, ::updateScore)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFFF8844.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(scoreText)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(score: Int) {
        runOnUiThread {
            val breed = gameView.getBreedName()
            scoreText.text = "Score: $score  |  Breed: $breed"
        }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Muzzle"
            setTextColor(0xFFFF8844.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF884422.toInt())
            setTextColor(0xFFFF8844.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Muzzle")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

data class DogBreed(val name: String, val emoji: String, val quirk: String, val color: Int)
data class Scenario(val description: String, val correctSequence: List<String>, val wrongMessage: String, val correctMessage: String)

class GameView(context: Context, private val scoreCallback: (Int) -> Unit) : View(context) {
    companion object {
        const val TAG = "GameView"
        const val BUBBLE_HEIGHT = 56f
        const val BUBBLE_PADDING = 8f
        const val SLOT_WIDTH = 100f
        const val SLOT_HEIGHT = 60f
    }

    val allThoughts = listOf("BARK", "SIT", "CHASE", "STAY", "GROWL", "FETCH", "EAT", "SLEEP", "WHINE", "DIG", "JUMP", "SNIFF")

    val breeds = listOf(
        DogBreed("Golden Retriever", "\uD83E\uDDBA", "Always friendly, 3-slot puzzles", 0xFFFFAA44.toInt()),
        DogBreed("Chihuahua", "\uD83D\uDC36", "Nervous! Extra thought options", 0xFFFF8844.toInt()),
        DogBreed("Husky", "\uD83D\uDC3A", "Stubborn - may ignore calm commands", 0xFFFF6644.toInt()),
        DogBreed("Bulldog", "\uD83D\uDC3B", "Lazy, fewer thoughts needed", 0xFFCC7744.toInt()),
        DogBreed("Border Collie", "\uD83D\uDC15", "Smartest, needs precise order", 0xFFFFAA22.toInt())
    )

    val allScenarios = listOf(
        Scenario(
            "Mailman is at the front door!",
            listOf("SIT", "STAY"),
            "The dog chased the mailman down the street! \uD83D\uDCE8\uD83C\uDFC3",
            "Good dog! Sat calmly at the door. \uD83D\uDC36\uD83D\uDC4D"
        ),
        Scenario(
            "A squirrel is in the backyard!",
            listOf("SIT", "STAY"),
            "The dog dug up the flower bed chasing it! \uD83C\uDF3B\uD83D\uDC3F",
            "Watched the squirrel calmly. \uD83D\uDC3F\uD83D\uDC40"
        ),
        Scenario(
            "A stranger is knocking loudly!",
            listOf("BARK", "GROWL", "SIT"),
            "Peed on the welcome mat from fear! \uD83D\uDCA7",
            "Alert barked, then settled down. Good guard! \uD83D\uDEE1\uFE0F"
        ),
        Scenario(
            "Your food fell on the kitchen floor!",
            listOf("STAY"),
            "Gobbled it up and got sick! \uD83E\uDD22",
            "Left it alone like a good pup! \uD83C\uDF56"
        ),
        Scenario(
            "Another dog is barking at the fence!",
            listOf("SIT", "STAY"),
            "Broke through the fence to fight! \uD83D\uDCA5",
            "Kept cool. Just a fence buddy. \uD83E\uDD1D"
        ),
        Scenario(
            "Owner just came home from work!",
            listOf("JUMP", "BARK"),
            "Knocked over the lamp with excitement! \uD83D\uDCA1\uD83D\uDCA5",
            "Happy greeting! Wagging tail chaos. \uD83D\uDC36\u2764\uFE0F"
        ),
        Scenario(
            "A toddler is pulling your tail!",
            listOf("SIT", "STAY"),
            "Snapped at the kid! Time out. \uD83D\uDE20",
            "Wise patience. Good with kids! \uD83D\uDC76\uD83D\uDC36"
        ),
        Scenario(
            "It's 3 AM and you hear a noise outside!",
            listOf("BARK", "GROWL", "SNIFF", "BARK"),
            "Slept through it, burglars took the TV! \uD83D\uDCFA",
            "Protected the house like a hero! \uD83C\uDF19\uD83D\uDC36"
        ),
        Scenario(
            "The vet is approaching with a needle!",
            listOf("SIT", "STAY"),
            "Hid under the table and wouldn't come out! \uD83D\uDE31",
            "Brave pup! Took it like a champ. \uD83D\uDC89\uD83D\uDCAA"
        ),
        Scenario(
            "A cat is sitting on your bed!",
            listOf("SNIFF", "SIT"),
            "Chased the cat through the house - vase destroyed! \uD83C\uDFFA\uD83D\uDC3E",
            "Made a new friend... maybe. \uD83D\uDC08\uD83D\uDC36"
        ),
        Scenario(
            "Someone threw a tennis ball!",
            listOf("FETCH", "SIT"),
            "Ran off with the ball and never came back! \uD83C\uDFBE\uD83C\uDFC3",
            "Retrieved it perfectly! Good fetch. \uD83C\uDFBE\uD83D\uDC4D"
        ),
        Scenario(
            "Dinner time - food bowl is full!",
            listOf("SIT", "EAT"),
            "Knocked the bowl over eating too fast! \uD83E\uDD63",
            "Patiently waited, then ate nicely. \uD83C\uDF56\uD83D\uDC36"
        )
    )

    private val bgPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
    private val bubblePaint = Paint().apply { style = Paint.Style.FILL }
    private val bubbleBorderPaint = Paint().apply { color = 0xFFFF8844.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f }
    private val bubbleTextPaint = Paint().apply { color = 0xFFFFCC88.toInt(); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val slotPaint = Paint().apply { color = 0xFF151528.toInt(); style = Paint.Style.FILL }
    private val slotBorderPaint = Paint().apply { color = 0xFF333355.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f; pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f) }
    private val slotFilledBorderPaint = Paint().apply { color = 0xFFFF8844.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val slotTextPaint = Paint().apply { color = 0xFFFFCC88.toInt(); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val scenarioTextPaint = Paint().apply { color = 0xFFCCCCCC.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    private val breedTextPaint = Paint().apply { color = 0xFFFFAA66.toInt(); textSize = 18f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE; isAntiAlias = true }
    private val resultTextPaint = Paint().apply { textSize = 32f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    private val resultSubPaint = Paint().apply { color = 0xFFAAAAAA.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val overlayPaint = Paint().apply { color = 0xBB000000.toInt(); style = Paint.Style.FILL }
    private val buttonPaint = Paint().apply { color = 0xFFFF8844.toInt(); style = Paint.Style.FILL }
    private val buttonTextPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    private val dogEmojiPaint = Paint().apply { textSize = 64f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val breedUnlockPaint = Paint().apply { color = 0xFFFF8844.toInt(); textSize = 16f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE; isAntiAlias = true }

    private var score = 0
    private var currentBreedIndex = 0
    private var currentScenarioIndex = 0
    private var scenariosUsed = mutableListOf<Int>()
    private var currentThoughts = mutableListOf<String>()
    private var sequenceSlots = mutableListOf<String?>()
    private var draggedBubble: Bubble? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var resultMessage = ""
    private var resultSubMessage = ""
    private var showingResult = false
    private var resultIsCorrect = false
    private var resultTimer = 0L
    private var feedbackAlpha = 1f
    private var gameOver = false
    private var shakeOffsetX = 0f
    private var shakeStartTime = 0L
    private val bubbles = mutableListOf<Bubble>()
    private val random = Random()

    init {
        setupLevel()
    }

    data class Bubble(val x: Float, val y: Float, val w: Float, val h: Float, val text: String, val sourceIndex: Int = -1)

    data class SubmitButton(val x: Float, val y: Float, val w: Float, val h: Float)

    private val submitButton = SubmitButton(0f, 0f, 120f, 48f)

    fun getBreedName(): String = breeds[currentBreedIndex].name

    private fun setupLevel() {
        val breed = breeds[currentBreedIndex]
        val scenario = getScenario()

        val sequenceLen = when (breed.name) {
            "Golden Retriever" -> scenario.correctSequence.size.coerceIn(2, 3)
            "Chihuahua" -> scenario.correctSequence.size.coerceIn(3, 4)
            "Husky" -> scenario.correctSequence.size.coerceIn(2, 3)
            "Bulldog" -> scenario.correctSequence.size.coerceIn(1, 2)
            "Border Collie" -> scenario.correctSequence.size.coerceIn(3, 5)
            else -> 3
        }

        val correctSeq = scenario.correctSequence.take(sequenceLen)
        sequenceSlots = MutableList(sequenceLen) { null }

        currentThoughts = correctSeq.toMutableList()
        val pool = allThoughts.filter { it !in correctSeq }
        pool.shuffled().take(when (breed.name) {
            "Chihuahua" -> 4
            "Bulldog" -> 2
            else -> 3
        }).forEach { currentThoughts.add(it) }
        currentThoughts.shuffle()

        bubbles.clear()
        resultMessage = ""
        resultSubMessage = ""
        showingResult = false
        draggedBubble = null
        shakeOffsetX = 0f
        feedbackAlpha = 1f
        invalidate()
    }

    private fun getScenario(): Scenario {
        if (scenariosUsed.size >= allScenarios.size) {
            scenariosUsed.clear()
        }
        var idx = random.nextInt(allScenarios.size)
        var tries = 0
        while (idx in scenariosUsed && tries < allScenarios.size) {
            idx = (idx + 1) % allScenarios.size
            tries++
        }
        scenariosUsed.add(idx)
        currentScenarioIndex = idx
        return allScenarios[idx]
    }

    private fun layoutBubbles(viewWidth: Float, viewHeight: Float) {
        bubbles.clear()
        val padding = 16f
        val totalBubbles = currentThoughts.size
        val maxPerRow = 4
        val rows = (totalBubbles + maxPerRow - 1) / maxPerRow
        val bubbleW = minOf((viewWidth - padding * 2) / maxPerRow, 140f)
        val startY = 20f
        var idx = 0
        for (row in 0 until rows) {
            val countInRow = minOf(maxPerRow, totalBubbles - row * maxPerRow)
            val rowWidth = countInRow * bubbleW
            val startX = (viewWidth - rowWidth) / 2f
            for (col in 0 until countInRow) {
                if (idx < totalBubbles) {
                    val bx = startX + col * bubbleW + BUBBLE_PADDING
                    val by = startY + row * (BUBBLE_HEIGHT + BUBBLE_PADDING)
                    bubbles.add(Bubble(bx, by, bubbleW - BUBBLE_PADDING * 2, BUBBLE_HEIGHT, currentThoughts[idx], idx))
                    idx++
                }
            }
        }
    }

    private fun getSlotPositions(viewWidth: Float, viewHeight: Float): List<Pair<Float, Float>> {
        val totalWidth = sequenceSlots.size * SLOT_WIDTH + (sequenceSlots.size - 1) * 12f
        val startX = (viewWidth - totalWidth) / 2f
        val slotY = viewHeight * 0.50f
        return sequenceSlots.indices.map { i ->
            Pair(startX + i * (SLOT_WIDTH + 12f), slotY)
        }
    }

    private fun getSubmitButtonRect(viewWidth: Float, viewHeight: Float): RectF {
        val cx = viewWidth / 2f
        val by = viewHeight * 0.72f
        return RectF(cx - submitButton.w / 2, by, cx + submitButton.w / 2, by + submitButton.h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                restart()
            }
            return true
        }
        if (showingResult) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val elapsed = System.currentTimeMillis() - resultTimer
                if (elapsed > 1000) {
                    showingResult = false
                    nextLevel()
                }
            }
            return true
        }

        val vw = width.toFloat()
        val vh = height.toFloat()

        val submitRect = getSubmitButtonRect(vw, vh)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                layoutBubbles(vw, vh)
                for (b in bubbles) {
                    if (event.x >= b.x && event.x <= b.x + b.w &&
                        event.y >= b.y && event.y <= b.y + b.h) {
                        if (draggedBubble == null) {
                            draggedBubble = b
                            dragOffsetX = event.x - b.x
                            dragOffsetY = event.y - b.y
                            currentThoughts[b.sourceIndex] = ""
                            return true
                        }
                    }
                }

                val slotPositions = getSlotPositions(vw, vh)
                for ((i, pos) in slotPositions.withIndex()) {
                    val sx = pos.first
                    val sy = pos.second
                    if (event.x >= sx && event.x <= sx + SLOT_WIDTH &&
                        event.y >= sy && event.y <= sy + SLOT_HEIGHT) {
                        val filled = sequenceSlots[i]
                        if (filled != null) {
                            val originalIdx = currentThoughts.indexOfFirst { it.isEmpty() || it == "" }
                            if (originalIdx >= 0) {
                                currentThoughts[originalIdx] = filled
                            } else {
                                currentThoughts.add(filled)
                            }
                            sequenceSlots[i] = null
                            return true
                        }
                    }
                }

                if (submitRect.contains(event.x, event.y)) {
                    submitSequence()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                draggedBubble?.let { b ->
                    val newX = event.x - dragOffsetX
                    val newY = event.y - dragOffsetY

                    val slotPositions = getSlotPositions(vw, vh)
                    var droppedInSlot = false
                    for ((i, pos) in slotPositions.withIndex()) {
                        val sx = pos.first
                        val sy = pos.second
                        if (newX + b.w / 2 >= sx && newX + b.w / 2 <= sx + SLOT_WIDTH &&
                            newY + b.h / 2 >= sy && newY + b.h / 2 <= sy + SLOT_HEIGHT) {
                            if (sequenceSlots[i] == null) {
                                sequenceSlots[i] = b.text
                                draggedBubble = null
                                droppedInSlot = true
                                invalidate()
                                return true
                            }
                        }
                    }

                    if (!droppedInSlot) {
                        val nb = Bubble(newX, newY, b.w, b.h, b.text, b.sourceIndex)
                        draggedBubble = nb
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedBubble?.let { b ->
                    val returned = false
                    for ((i, t) in currentThoughts.withIndex()) {
                        if (t.isEmpty()) {
                            currentThoughts[i] = b.text
                            break
                        }
                    }
                    draggedBubble = null
                    invalidate()
                }
            }
        }
        return true
    }

    private fun submitSequence() {
        val filled = sequenceSlots.filterNotNull()
        if (filled.size < sequenceSlots.size) {
            resultMessage = "Fill all slots!"
            resultSubMessage = "Drag bubbles into every slot."
            showingResult = true
            resultIsCorrect = false
            resultTimer = System.currentTimeMillis()
            feedbackAlpha = 1f
            invalidate()
            return
        }

        val scenario = allScenarios[currentScenarioIndex]
        val correctSeq = scenario.correctSequence.take(sequenceSlots.size)

        val breed = breeds[currentBreedIndex]
        val correct = if (breed.name == "Husky" && random.nextFloat() < 0.2f) {
            false
        } else {
            filled == correctSeq
        }

        if (correct) {
            score += 10
            resultMessage = scenario.correctMessage
            resultSubMessage = "Score +10"
            resultIsCorrect = true
            scoreCallback(score)

            if (score > 0 && score % 50 == 0 && currentBreedIndex < breeds.size - 1) {
                currentBreedIndex++
                resultSubMessage = "Score +10  |  Unlocked: ${breeds[currentBreedIndex].name}!"
            }
        } else {
            score = maxOf(0, score - 5)
            resultMessage = scenario.wrongMessage
            resultSubMessage = "Score -5"
            resultIsCorrect = false
            shakeStartTime = System.currentTimeMillis()
            scoreCallback(score)
        }

        showingResult = true
        resultTimer = System.currentTimeMillis()
        feedbackAlpha = 1f
        invalidate()
    }

    private fun nextLevel() {
        if (currentBreedIndex >= breeds.size - 1 && score >= 150) {
            gameOver = true
            scoreCallback(score)
            invalidate()
            return
        }
        setupLevel()
        layoutBubbles(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vw = width.toFloat()
        val vh = height.toFloat()
        canvas.drawRect(0f, 0f, vw, vh, bgPaint)

        val scenario = allScenarios[currentScenarioIndex]
        val breed = breeds[currentBreedIndex]

        canvas.save()
        if (shakeStartTime > 0 && System.currentTimeMillis() - shakeStartTime < 400) {
            val elapsed = (System.currentTimeMillis() - shakeStartTime).toFloat() / 400f
            shakeOffsetX = (sin(elapsed * 20f) * (12f * (1f - elapsed))).toFloat()
            canvas.translate(shakeOffsetX, 0f)
        } else {
            shakeOffsetX = 0f
        }

        val scenarioTextY = 8f + breeds.size * 2f
        scenarioTextPaint.textSize = minOf(26f, vw / 22f)
        wrapText(canvas, scenario.description, vw / 2f, scenarioTextY + 60f, vw - 40f, scenarioTextPaint)

        val breedLineY = scenarioTextY + 120f
        breedTextPaint.textSize = minOf(16f, vw / 30f)
        canvas.drawText("${breed.emoji}  ${breed.name}  -  ${breed.quirk}", vw / 2f, breedLineY, breedTextPaint)

        layoutBubbles(vw, vh)

        for (b in bubbles) {
            if (b.text.isEmpty()) continue
            val isDragged = draggedBubble?.sourceIndex == b.sourceIndex
            if (isDragged) continue

            val bx = b.x
            val by = b.y
            val radius = 18f
            bubblePaint.color = 0xFF1E2538.toInt()
            canvas.drawRoundRect(bx, by, bx + b.w, by + b.h, radius, radius, bubblePaint)
            canvas.drawRoundRect(bx, by, bx + b.w, by + b.h, radius, radius, bubbleBorderPaint)
            canvas.drawText(b.text, bx + b.w / 2, by + b.h / 2 + 8f, bubbleTextPaint)
        }

        val slotPositions = getSlotPositions(vw, vh)
        for ((i, pos) in slotPositions.withIndex()) {
            val sx = pos.first
            val sy = pos.second
            val radius = 14f
            if (sequenceSlots[i] != null) {
                canvas.drawRoundRect(sx, sy, sx + SLOT_WIDTH, sy + SLOT_HEIGHT, radius, radius, slotPaint)
                canvas.drawRoundRect(sx, sy, sx + SLOT_WIDTH, sy + SLOT_HEIGHT, radius, radius, slotFilledBorderPaint)
                canvas.drawText(sequenceSlots[i]!!, sx + SLOT_WIDTH / 2, sy + SLOT_HEIGHT / 2 + 8f, slotTextPaint)
            } else {
                slotBorderPaint.color = 0xFF333355.toInt()
                canvas.drawRoundRect(sx, sy, sx + SLOT_WIDTH, sy + SLOT_HEIGHT, radius, radius, slotPaint)
                canvas.drawRoundRect(sx, sy, sx + SLOT_WIDTH, sy + SLOT_HEIGHT, radius, radius, slotBorderPaint)
            }
            val numberPaint = Paint().apply {
                color = 0xFF555566.toInt()
                textSize = 14f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }
            canvas.drawText("${i + 1}", sx + SLOT_WIDTH / 2, sy - 6f, numberPaint)
        }

        val submitRect = getSubmitButtonRect(vw, vh)
        val filledCount = sequenceSlots.count { it != null }
        buttonPaint.color = if (filledCount == sequenceSlots.size) 0xFFFF8844.toInt() else 0xFF553322.toInt()
        val btnRadius = 12f
        canvas.drawRoundRect(submitRect, btnRadius, btnRadius, buttonPaint)
        canvas.drawText("Submit", submitRect.centerX(), submitRect.centerY() + 7f, buttonTextPaint)

        draggedBubble?.let { b ->
            val bx = b.x
            val by = b.y
            bubblePaint.color = 0xFF2A3045.toInt()
            val radius = 18f
            bubbleBorderPaint.color = 0xFFFFAA66.toInt()
            bubbleBorderPaint.strokeWidth = 3.5f
            canvas.drawRoundRect(bx, by, bx + b.w, by + b.h, radius, radius, bubblePaint)
            canvas.drawRoundRect(bx, by, bx + b.w, by + b.h, radius, radius, bubbleBorderPaint)
            canvas.drawText(b.text, bx + b.w / 2, by + b.h / 2 + 8f, bubbleTextPaint)
            bubbleBorderPaint.color = 0xFFFF8844.toInt()
            bubbleBorderPaint.strokeWidth = 2.5f
        }

        canvas.restore()

        if (showingResult) {
            val elapsed = System.currentTimeMillis() - resultTimer
            val alphaProgress = (elapsed / 800f).coerceIn(0f, 1f)
            val overlayAlpha = (0xAA * (1f - alphaProgress * 0.5f)).toInt()
            overlayPaint.color = Color.argb(overlayAlpha, 0, 0, 0)
            canvas.drawRect(0f, 0f, vw, vh, overlayPaint)

            resultTextPaint.color = if (resultIsCorrect) 0xFF44FF44.toInt() else 0xFFFF3344.toInt()
            val resultY = vh / 2f - 30f

            resultTextPaint.textSize = minOf(30f, vw / 15f)
            resultSubPaint.textSize = minOf(18f, vw / 25f)

            wrapText(canvas, resultMessage, vw / 2f, resultY, vw - 60f, resultTextPaint)
            canvas.drawText(resultSubMessage, vw / 2f, resultY + 60f, resultSubPaint)

            if (elapsed > 2000) {
                val tapPaint = Paint().apply {
                    color = 0xFF888888.toInt()
                    textSize = 16f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("Tap to continue", vw / 2f, resultY + 90f, tapPaint)
            }
        }

        if (gameOver) {
            overlayPaint.color = 0xCC000000.toInt()
            canvas.drawRect(0f, 0f, vw, vh, overlayPaint)

            val gameOverPaint = Paint().apply {
                color = 0xFFFF8844.toInt()
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText("You Did It!", vw / 2f, vh / 2f - 40, gameOverPaint)

            val finalScorePaint = Paint().apply {
                color = 0xFFCCCCCC.toInt()
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Final Score: $score", vw / 2f, vh / 2f + 20, finalScorePaint)

            val allBreedsPaint = Paint().apply {
                color = 0xFF8888AA.toInt()
                textSize = 18f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("All breeds unlocked! \uD83C\uDFC6", vw / 2f, vh / 2f + 58, allBreedsPaint)

            val restartTextPaint = Paint().apply {
                color = 0xFF666688.toInt()
                textSize = 20f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Tap to play again", vw / 2f, vh / 2f + 100, restartTextPaint)
        }
    }

    private fun wrapText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())

        val lineHeight = paint.textSize + 4f
        val totalHeight = lineHeight * lines.size
        var currentY = y - totalHeight / 2f + lineHeight / 2f

        for (line in lines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += lineHeight
        }
    }

    fun restart() {
        score = 0
        currentBreedIndex = 0
        scenariosUsed.clear()
        gameOver = false
        showingResult = false
        draggedBubble = null
        shakeOffsetX = 0f
        scoreCallback(0)
        setupLevel()
        layoutBubbles(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutBubbles(w.toFloat(), h.toFloat())
    }
}
