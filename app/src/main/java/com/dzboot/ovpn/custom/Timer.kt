package com.dzboot.ovpn.custom

import android.os.*
import com.dzboot.ovpn.helpers.Utils


abstract class Timer {

    companion object {
        private const val MSG = 1
        private const val PROLONG_DURATION = 30 * 60 * 1000L
        private const val INTERVAL = 1000L
    }

    abstract fun onTick(millisLeft: Long)
    abstract fun onFinish()

    private var totalDuration: Long = 0
    private var millisLeft: Long = 0
    private var stopTimeInFuture: Long = 0
    private var pauseTime: Long = 0
    private var isCancelled = false
    private var isPaused = false

    /**
     * Cancel the countdown.
     *
     * Do not call it from inside CountDownTimer threads
     */
    fun cancel() {
        handler.removeMessages(MSG)
        isCancelled = true
    }

    @Synchronized
    fun start(initialDurationMinutes: Int): Timer {
        val initialDuration = initialDurationMinutes * 60 * 1000L
        totalDuration = initialDuration
        stopTimeInFuture = SystemClock.elapsedRealtime() + initialDuration
        handler.sendMessage(handler.obtainMessage(MSG))
        isCancelled = false
        isPaused = false
        return this
    }

    fun pause(): Long {
        pauseTime = stopTimeInFuture - SystemClock.elapsedRealtime()
        isPaused = true
        return pauseTime
    }

    fun resume(): Long {
        stopTimeInFuture = pauseTime + SystemClock.elapsedRealtime()
        isPaused = false
        handler.sendMessage(handler.obtainMessage(MSG))
        return pauseTime
    }

    fun prolong() {
        if (!isCancelled) {
            stopTimeInFuture += PROLONG_DURATION
            totalDuration += PROLONG_DURATION
        }
    }

    fun getProgress() = (100 * millisLeft.div(totalDuration.toFloat())).toInt()

    fun getTimeLeftString() = Utils.millisToTime(millisLeft / 1000)

    // handles counting down
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            synchronized(this@Timer) {
                if (!isPaused) {
                    millisLeft = stopTimeInFuture - SystemClock.elapsedRealtime()
                    if (millisLeft <= 0) {
                        onFinish()
                    } else if (millisLeft < INTERVAL) {
                        // no tick, just delay until done
                        sendMessageDelayed(obtainMessage(MSG), millisLeft)
                    } else {
                        val lastTickStart = SystemClock.elapsedRealtime()
                        onTick(millisLeft)

                        // take into account user's onTick taking time to execute
                        var delay =
                            lastTickStart + INTERVAL - SystemClock.elapsedRealtime()

                        // special case: user's onTick took more than interval to
                        // complete, skip to next interval
                        while (delay < 0) delay += INTERVAL
                        if (!isCancelled) {
                            sendMessageDelayed(obtainMessage(MSG), delay)
                        }
                    }
                }
            }
        }
    }
}