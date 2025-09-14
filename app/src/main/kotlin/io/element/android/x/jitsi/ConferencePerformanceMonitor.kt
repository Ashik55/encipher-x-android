/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.jitsi

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Real-time performance monitoring system for instant conference optimization.
 * 
 * Tracks critical performance metrics to ensure WhatsApp-like smooth video calling
 * with sub-500ms conference launch times.
 */
object ConferencePerformanceMonitor {
    
    private const val TAG = "ConferencePerfMonitor"
    
    // Performance thresholds (in milliseconds)
    private const val TARGET_CONFERENCE_LAUNCH_TIME = 500L
    private const val TARGET_AUDIO_INIT_TIME = 200L
    private const val TARGET_VIDEO_INIT_TIME = 300L
    private const val TARGET_NETWORK_CONNECTION_TIME = 1000L
    private const val TARGET_REACT_NATIVE_LOAD_TIME = 100L
    
    // Performance metrics storage
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val isMonitoring = AtomicReference(false)
    
    // Current session tracking
    private val sessionStartTime = AtomicLong(0L)
    private val currentSessionId = AtomicReference<String?>(null)
    
    data class PerformanceMetric(
        val name: String,
        val value: Long,
        val timestamp: Long = System.currentTimeMillis(),
        val threshold: Long = 0L,
        val isGood: Boolean = true
    )
    
    data class PerformanceReport(
        val sessionId: String,
        val totalTime: Long,
        val metrics: Map<String, PerformanceMetric>,
        val isPerformanceGood: Boolean,
        val warnings: List<String>,
        val recommendations: List<String>
    )
    
    /**
     * Starts performance monitoring for a new conference session.
     */
    fun startMonitoring(sessionId: String) {
        if (isMonitoring.get()) {
            Log.w(TAG, "Monitoring already active, stopping previous session")
            stopMonitoring()
        }
        
        sessionStartTime.set(System.currentTimeMillis())
        currentSessionId.set(sessionId)
        isMonitoring.set(true)
        
        Log.d(TAG, "🚀 Started performance monitoring for session: $sessionId")
    }
    
    /**
     * Stops performance monitoring and generates final report.
     */
    fun stopMonitoring(): PerformanceReport? {
        if (!isMonitoring.get()) {
            Log.w(TAG, "No active monitoring session to stop")
            return null
        }
        
        val sessionId = currentSessionId.get() ?: "unknown"
        val totalTime = System.currentTimeMillis() - sessionStartTime.get()
        
        val report = generatePerformanceReport(sessionId, totalTime)
        
        isMonitoring.set(false)
        currentSessionId.set(null)
        sessionStartTime.set(0L)
        
        Log.d(TAG, "📊 Performance monitoring stopped for session: $sessionId")
        Log.d(TAG, "Total session time: ${totalTime}ms")
        
        return report
    }
    
    /**
     * Records a performance metric with automatic threshold checking.
     */
    fun recordMetric(name: String, value: Long, threshold: Long = 0L) {
        if (!isMonitoring.get()) {
            Log.w(TAG, "No active monitoring session, ignoring metric: $name")
            return
        }
        
        val effectiveThreshold = if (threshold > 0) threshold else getDefaultThreshold(name)
        val isGood = value <= effectiveThreshold
        
        val metric = PerformanceMetric(
            name = name,
            value = value,
            threshold = effectiveThreshold,
            isGood = isGood
        )
        
        metrics[name] = metric
        
        if (isGood) {
            Log.d(TAG, "✅ $name: ${value}ms (threshold: ${effectiveThreshold}ms)")
        } else {
            Log.w(TAG, "⚠️ $name: ${value}ms (threshold: ${effectiveThreshold}ms) - SLOW!")
        }
    }
    
    /**
     * Records conference launch time.
     */
    fun recordConferenceLaunchTime(timeMs: Long) {
        recordMetric("conference_launch", timeMs, TARGET_CONFERENCE_LAUNCH_TIME)
    }
    
    /**
     * Records audio initialization time.
     */
    fun recordAudioInitTime(timeMs: Long) {
        recordMetric("audio_init", timeMs, TARGET_AUDIO_INIT_TIME)
    }
    
    /**
     * Records video initialization time.
     */
    fun recordVideoInitTime(timeMs: Long) {
        recordMetric("video_init", timeMs, TARGET_VIDEO_INIT_TIME)
    }
    
    /**
     * Records network connection time.
     */
    fun recordNetworkConnectionTime(timeMs: Long) {
        recordMetric("network_connection", timeMs, TARGET_NETWORK_CONNECTION_TIME)
    }
    
    /**
     * Records React Native load time.
     */
    fun recordReactNativeLoadTime(timeMs: Long) {
        recordMetric("react_native_load", timeMs, TARGET_REACT_NATIVE_LOAD_TIME)
    }
    
    /**
     * Records custom performance metric.
     */
    fun recordCustomMetric(name: String, timeMs: Long, threshold: Long = 0L) {
        recordMetric(name, timeMs, threshold)
    }
    
    /**
     * Gets current performance metrics.
     */
    fun getCurrentMetrics(): Map<String, PerformanceMetric> = metrics.toMap()
    
    /**
     * Checks if current performance is within acceptable thresholds.
     */
    fun isPerformanceGood(): Boolean {
        return metrics.values.all { it.isGood }
    }
    
    /**
     * Gets performance warnings for metrics that exceed thresholds.
     */
    fun getPerformanceWarnings(): List<String> {
        return metrics.values
            .filter { !it.isGood }
            .map { "${it.name}: ${it.value}ms (threshold: ${it.threshold}ms)" }
    }
    
    /**
     * Gets performance recommendations based on current metrics.
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        metrics.forEach { (name, metric) ->
            if (!metric.isGood) {
                when (name) {
                    "conference_launch" -> recommendations.add("Consider increasing preloading aggressiveness")
                    "audio_init" -> recommendations.add("Pre-warm audio systems more aggressively")
                    "video_init" -> recommendations.add("Pre-warm video systems more aggressively")
                    "network_connection" -> recommendations.add("Improve network preloading strategy")
                    "react_native_load" -> recommendations.add("Optimize React Native preloading")
                    else -> recommendations.add("Optimize $name performance")
                }
            }
        }
        
        return recommendations
    }
    
    /**
     * Generates comprehensive performance report.
     */
    private fun generatePerformanceReport(sessionId: String, totalTime: Long): PerformanceReport {
        val warnings = getPerformanceWarnings()
        val recommendations = getPerformanceRecommendations()
        val isGood = isPerformanceGood()
        
        Log.d(TAG, "📊 Performance Report Generated:")
        Log.d(TAG, "  Session: $sessionId")
        Log.d(TAG, "  Total Time: ${totalTime}ms")
        Log.d(TAG, "  Performance Good: $isGood")
        Log.d(TAG, "  Warnings: ${warnings.size}")
        Log.d(TAG, "  Recommendations: ${recommendations.size}")
        
        return PerformanceReport(
            sessionId = sessionId,
            totalTime = totalTime,
            metrics = metrics.toMap(),
            isPerformanceGood = isGood,
            warnings = warnings,
            recommendations = recommendations
        )
    }
    
    /**
     * Gets default threshold for a metric name.
     */
    private fun getDefaultThreshold(name: String): Long {
        return when (name) {
            "conference_launch" -> TARGET_CONFERENCE_LAUNCH_TIME
            "audio_init" -> TARGET_AUDIO_INIT_TIME
            "video_init" -> TARGET_VIDEO_INIT_TIME
            "network_connection" -> TARGET_NETWORK_CONNECTION_TIME
            "react_native_load" -> TARGET_REACT_NATIVE_LOAD_TIME
            else -> 1000L // Default 1 second threshold
        }
    }
    
    /**
     * Clears all performance metrics (useful for testing).
     */
    fun clearMetrics() {
        metrics.clear()
        Log.d(TAG, "🧹 Performance metrics cleared")
    }
    
    /**
     * Gets performance summary for display.
     */
    fun getPerformanceSummary(): String {
        if (metrics.isEmpty()) {
            return "No performance data available"
        }
        
        val goodMetrics = metrics.values.count { it.isGood }
        val totalMetrics = metrics.size
        val performancePercentage = (goodMetrics * 100) / totalMetrics
        
        return "Performance: $performancePercentage% ($goodMetrics/$totalMetrics metrics within threshold)"
    }
    
    /**
     * Checks if monitoring is currently active.
     */
    fun isMonitoring(): Boolean = isMonitoring.get()
    
    /**
     * Gets current session ID.
     */
    fun getCurrentSessionId(): String? = currentSessionId.get()
}
