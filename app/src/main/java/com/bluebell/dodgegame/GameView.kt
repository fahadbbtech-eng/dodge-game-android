package com.bluebell.dodgegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.random.Random

/**
 * ============================================================
 * LESSON 1: What is a "game"? At its core, every video game is
 * just a loop that repeats ~60 times per second and does 3 things:
 *   1. UPDATE  - move things, check collisions, update the score
 *   2. DRAW    - paint the current state of the game to the screen
 *   3. WAIT    - pause briefly so it doesn't run faster than the
 *                screen can display (usually ~60 frames per second)
 * This is called the "game loop". Everything else is built on top
 * of it. This whole file IS that loop, plus the game's rules.
 * ============================================================
 *
 * The game itself: falling blocks drop from the top of the screen.
 * You drag your finger to slide a paddle left/right at the bottom.
 * Every block you avoid gives you a point. Touch a block -> game over.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    // The background thread that runs our game loop, separate from
    // the UI thread so drawing never freezes the rest of the app.
    private var gameThread: Thread? = null
    private var isPlaying = false

    // --- Paddle (the player) ---
    private var paddleX = 400f
    private val paddleWidth = 220f
    private val paddleHeight = 40f

    // --- Falling blocks (the obstacles) ---
    data class Block(var x: Float, var y: Float, val size: Float, var speed: Float)
    private val blocks = mutableListOf<Block>()
    private var framesSinceLastBlock = 0

    private var score = 0
    private var gameOver = false

    private val paint = Paint().apply { isAntiAlias = true }

    init {
        holder.addCallback(this)
    }

    // --- SurfaceHolder.Callback: Android calls these automatically ---

    override fun surfaceCreated(holder: SurfaceHolder) {
        isPlaying = true
        gameThread = Thread(this)
        gameThread!!.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isPlaying = false
        gameThread?.join()
    }

    /**
     * LESSON 2: This "run()" method IS the game loop mentioned above.
     * It's called once, and it keeps looping on its own until isPlaying
     * becomes false (e.g. when you close the app).
     */
    override fun run() {
        while (isPlaying) {
            update()
            draw()
            // Sleep ~16ms so we aim for roughly 60 updates per second.
            Thread.sleep(16)
        }
    }

    /** LESSON 3: UPDATE - move everything and check the rules. */
    private fun update() {
        if (gameOver) return

        // Spawn a new block every ~40 frames (roughly every 0.6 seconds).
        framesSinceLastBlock++
        if (framesSinceLastBlock > 40) {
            framesSinceLastBlock = 0
            val size = Random.nextInt(50, 100).toFloat()
            val x = Random.nextFloat() * (width - size).coerceAtLeast(1f)
            val speed = Random.nextInt(10, 20).toFloat()
            blocks.add(Block(x, -size, size, speed))
        }

        val paddleTop = height - 150f
        val paddleRect = RectF(paddleX, paddleTop, paddleX + paddleWidth, paddleTop + paddleHeight)

        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            block.y += block.speed

            val blockRect = RectF(block.x, block.y, block.x + block.size, block.y + block.size)

            // LESSON 4: Collision detection. RectF.intersect() checks whether
            // two rectangles overlap -- this is how almost every 2D game
            // detects "did the player touch the obstacle?"
            if (RectF.intersects(paddleRect, blockRect)) {
                gameOver = true
            }

            // Block fell past the bottom of the screen without being hit:
            // remove it and award a point for successfully dodging it.
            if (block.y > height) {
                iterator.remove()
                score++
            }
        }
    }

    /** LESSON 5: DRAW - paint the current state to the screen. */
    private fun draw() {
        if (!holder.surface.isValid) return
        val canvas: Canvas = holder.lockCanvas()

        // Background
        canvas.drawColor(Color.rgb(20, 20, 30))

        // Score text
        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("Score: $score", 40f, 100f, paint)

        // Falling blocks
        paint.color = Color.rgb(230, 90, 90)
        for (block in blocks) {
            canvas.drawRect(block.x, block.y, block.x + block.size, block.y + block.size, paint)
        }

        // Paddle
        paint.color = Color.rgb(90, 200, 230)
        val paddleTop = height - 150f
        canvas.drawRect(paddleX, paddleTop, paddleX + paddleWidth, paddleTop + paddleHeight, paint)

        if (gameOver) {
            paint.color = Color.WHITE
            paint.textSize = 80f
            canvas.drawText("GAME OVER", width / 2f - 260f, height / 2f, paint)
            paint.textSize = 45f
            canvas.drawText("Tap to restart", width / 2f - 170f, height / 2f + 70f, paint)
        }

        holder.unlockCanvasAndPost(canvas)
    }

    /**
     * LESSON 6: Touch input. Android sends every finger movement to this
     * method. We use the finger's X position to slide the paddle, and a
     * simple tap to restart after game over.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Reset everything for a new game.
                blocks.clear()
                score = 0
                gameOver = false
            }
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                paddleX = (event.x - paddleWidth / 2).coerceIn(0f, (width - paddleWidth).coerceAtLeast(0f))
            }
        }
        return true
    }
}
