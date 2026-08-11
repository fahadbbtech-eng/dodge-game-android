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

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

      private var gameThread: Thread? = null
      private var isPlaying = false

      private var paddleX = 400f
      private val paddleWidth = 220f
      private val paddleHeight = 40f

      data class Block(var x: Float, var y: Float, val size: Float, var speed: Float)
          private val blocks = mutableListOf<Block>()
              private var framesSinceLastBlock = 0

      private var score = 0
      private var gameOver = false

      private val paint = Paint().apply { isAntiAlias = true }

          init {
                    holder.addCallback(this)
          }

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

                          override fun run() {
                                    while (isPlaying) {
                                                  update()
                                                              draw()
                                                                          Thread.sleep(16)
                                    }
                          }

                              private fun update() {
                                        if (gameOver) return

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

                                                                                  if (RectF.intersects(paddleRect, blockRect)) {
                                                                                                    gameOver = true
                                                                                  }

                                                                                              if (block.y > height) {
                                                                                                                iterator.remove()
                                                                                                                                score++
                                                                                              }
                                                        }
                              }

                                  private fun draw() {
                                            if (!holder.surface.isValid) return
                                            val canvas: Canvas = holder.lockCanvas()

                                                    canvas.drawColor(Color.rgb(20, 20, 30))

                                                            paint.color = Color.WHITE
                                            paint.textSize = 60f
                                            canvas.drawText("Score: " + score, 40f, 100f, paint)

                                                    paint.color = Color.rgb(230, 90, 90)
                                                            for (block in blocks) {
                                                                          canvas.drawRect(block.x, block.y, block.x + block.size, block.y + block.size, paint)
                                                            }

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

                                      override fun onTouchEvent(event: MotionEvent): Boolean {
                                                if (gameOver) {
                                                              if (event.action == MotionEvent.ACTION_DOWN) {
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
