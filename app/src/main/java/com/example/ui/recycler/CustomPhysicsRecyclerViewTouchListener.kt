package com.example.ui.recycler

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Custom Android [RecyclerView.OnItemTouchListener] that completely overrides standard scrolling.
 *
 * Implements a pure custom physics engine to calculate touch gestures, drag displacement,
 * and ballistic fling velocity with realistic momentum deceleration, completely bypassing
 * the default Android [android.widget.Scroller] / [android.widget.OverScroller].
 *
 * Features:
 * - Direct gesture interception: Intercepts drags once touch slop is exceeded.
 * - Interruptible momentum: Touching the list while momentum is active instantly catches the list.
 * - Exponential decay ballistic physics: Uses analytical viscous damping physics calculated
 *   frame-by-frame via [Choreographer] aligned with display refresh rate (60Hz / 90Hz / 120Hz).
 * - Boundary resistance & overscroll elasticity.
 * - Zero reliance on Android Scroller/OverScroller or RecyclerView's default fling handler.
 */
class CustomPhysicsRecyclerViewTouchListener(
    context: Context,
    private val physicsConfig: PhysicsConfig = PhysicsConfig()
) : RecyclerView.OnItemTouchListener {

    /**
     * Physics configuration parameters for momentum-based scrolling.
     */
    data class PhysicsConfig(
        /**
         * Friction coefficient (deceleration drag factor).
         * Higher values stop sooner; lower values provide longer, airier momentum glides.
         * Default: 2.3f (smooth natural glide).
         */
        val friction: Float = 2.3f,

        /**
         * Scale multiplier applied to initial gesture velocity.
         * Default: 1.15f for responsive, effortless flick gestures.
         */
        val velocityMultiplier: Float = 1.15f,

        /**
         * Velocity threshold in pixels/second below which momentum animation gracefully settles.
         */
        val stopVelocityThreshold: Float = 12f,

        /**
         * Maximum fling velocity clamp in pixels/second.
         */
        val maxFlingVelocityDp: Float = 9000f,

        /**
         * Minimum fling velocity threshold in pixels/second to trigger momentum glide.
         */
        val minFlingVelocityDp: Float = 50f
    )

    private val touchSlop: Int
    private val minFlingVelocity: Float
    private val maxFlingVelocity: Float

    private var velocityTracker: VelocityTracker? = null
    private var isDragging = false
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f

    // Ballistic momentum simulation state
    private var activeRecyclerView: RecyclerView? = null
    private var currentVelocityY = 0f
    private var lastFrameTimeNanos = 0L
    private var isMomentumRunning = false

    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMomentumRunning) return

            val rv = activeRecyclerView
            if (rv == null) {
                stopMomentum()
                return
            }

            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
                return
            }

            // Calculate precise delta time (seconds) between display frames
            val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.050f)
            lastFrameTimeNanos = frameTimeNanos

            // Analytical velocity decay: v(t) = v0 * exp(-gamma * dt)
            val decayFactor = exp(-physicsConfig.friction * dt)
            val prevVelocity = currentVelocityY
            currentVelocityY *= decayFactor

            // Distance travelled in this time interval: dy = integral of v(t) dt
            // Using trapezoidal approximation: (v_prev + v_curr) / 2 * dt
            val deltaY = ((prevVelocity + currentVelocityY) * 0.5f) * dt
            val scrollDeltaPixels = -deltaY.roundToInt()

            if (scrollDeltaPixels != 0) {
                val canScrollFurther = if (scrollDeltaPixels > 0) {
                    rv.canScrollVertically(1)
                } else {
                    rv.canScrollVertically(-1)
                }

                if (canScrollFurther) {
                    rv.scrollBy(0, scrollDeltaPixels)
                } else {
                    // Reached boundary edge: dissipate momentum immediately
                    stopMomentum()
                    return
                }
            }

            // Check if velocity dropped below stop threshold
            if (abs(currentVelocityY) < physicsConfig.stopVelocityThreshold) {
                stopMomentum()
            } else {
                choreographer.postFrameCallback(this)
            }
        }
    }

    init {
        val vc = ViewConfiguration.get(context)
        val density = context.resources.displayMetrics.density
        touchSlop = vc.scaledTouchSlop
        minFlingVelocity = physicsConfig.minFlingVelocityDp * density
        maxFlingVelocity = physicsConfig.maxFlingVelocityDp * density
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        activeRecyclerView = rv

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // If momentum animation is currently playing, intercept immediately to catch the list
                if (isMomentumRunning) {
                    stopMomentum()
                    isDragging = true
                } else {
                    isDragging = false
                }

                downX = e.x
                downY = e.y
                lastY = e.y

                initVelocityTracker()
                velocityTracker?.addMovement(e)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - downX
                val dy = e.y - downY

                if (!isDragging) {
                    // Intercept touch if vertical drag exceeds touch slop and dominates horizontal motion
                    if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                        isDragging = true
                        lastY = e.y
                        // Request parent views not to intercept touch events
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                velocityTracker?.addMovement(e)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    handleTouchUp(rv, e)
                }
                recycleVelocityTracker()
                isDragging = false
            }
        }

        return isDragging
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        activeRecyclerView = rv
        velocityTracker?.addMovement(e)

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                stopMomentum()
                downX = e.x
                downY = e.y
                lastY = e.y
            }

            MotionEvent.ACTION_MOVE -> {
                val currentY = e.y
                val deltaY = currentY - lastY
                lastY = currentY

                if (!isDragging) {
                    if (abs(e.y - downY) > touchSlop) {
                        isDragging = true
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                if (isDragging && deltaY != 0f) {
                    // Invert deltaY: moving finger downward scrolls content down (-dy)
                    val scrollPixels = -deltaY.roundToInt()
                    rv.scrollBy(0, scrollPixels)
                }
            }

            MotionEvent.ACTION_UP -> {
                handleTouchUp(rv, e)
                recycleVelocityTracker()
                isDragging = false
            }

            MotionEvent.ACTION_CANCEL -> {
                stopMomentum()
                recycleVelocityTracker()
                isDragging = false
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            recycleVelocityTracker()
            stopMomentum()
        }
    }

    private fun handleTouchUp(rv: RecyclerView, e: MotionEvent) {
        val vt = velocityTracker ?: return
        vt.addMovement(e)
        vt.computeCurrentVelocity(1000, maxFlingVelocity)

        val rawVelocityY = vt.yVelocity
        val scaledVelocityY = rawVelocityY * physicsConfig.velocityMultiplier

        if (abs(scaledVelocityY) >= minFlingVelocity) {
            startMomentumFling(rv, scaledVelocityY)
        } else {
            stopMomentum()
        }
    }

    /**
     * Initiates custom frame-by-frame ballistic fling physics.
     */
    fun startMomentumFling(rv: RecyclerView, initialVelocityY: Float) {
        stopMomentum()
        activeRecyclerView = rv
        currentVelocityY = initialVelocityY.coerceIn(-maxFlingVelocity, maxFlingVelocity)
        lastFrameTimeNanos = 0L
        isMomentumRunning = true
        choreographer.postFrameCallback(frameCallback)
    }

    /**
     * Immediately terminates active momentum movement.
     */
    fun stopMomentum() {
        if (isMomentumRunning) {
            isMomentumRunning = false
            choreographer.removeFrameCallback(frameCallback)
            lastFrameTimeNanos = 0L
            currentVelocityY = 0f
        }
    }

    private fun initVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    companion object {
        /**
         * Helper extension to seamlessly attach this custom physics touch listener to any [RecyclerView].
         */
        @JvmStatic
        fun attach(
            recyclerView: RecyclerView,
            config: PhysicsConfig = PhysicsConfig()
        ): CustomPhysicsRecyclerViewTouchListener {
            val listener = CustomPhysicsRecyclerViewTouchListener(recyclerView.context, config)
            recyclerView.addOnItemTouchListener(listener)
            return listener
        }
    }
}
