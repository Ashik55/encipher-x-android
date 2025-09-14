/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.jitsi

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.libraries.designsystem.theme.ElementThemeApp
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.features.enterprise.api.EnterpriseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Demo screen showcasing instant Jitsi conference performance with real-time metrics.
 * 
 * Displays performance monitoring, launch statistics, and optimization status
 * for WhatsApp-like smooth video calling experience.
 */
@Composable
fun InstantJitsiDemoScreen(
    context: Context,
    appPreferencesStore: AppPreferencesStore,
    enterpriseService: EnterpriseService,
    onLaunchConference: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    var performanceMetrics by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var launchStatistics by remember { mutableStateOf<InstantJitsiLauncher.LaunchStatistics?>(null) }
    var isPreloaded by remember { mutableStateOf(false) }
    var performanceReport by remember { mutableStateOf<ConferencePerformanceMonitor.PerformanceReport?>(null) }
    var refreshCounter by remember { mutableStateOf(0) }
    
    // Auto-refresh metrics every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            performanceMetrics = InstantConferenceApp.getPerformanceMetrics()
            launchStatistics = InstantJitsiLauncher.getLaunchStatistics()
            isPreloaded = InstantConferenceApp.isPreloaded()
            val currentMetrics = ConferencePerformanceMonitor.getCurrentMetrics()
            performanceReport = ConferencePerformanceMonitor.PerformanceReport(
                sessionId = "demo_session",
                totalTime = 0L,
                metrics = currentMetrics,
                isPerformanceGood = ConferencePerformanceMonitor.isPerformanceGood(),
                warnings = ConferencePerformanceMonitor.getPerformanceWarnings(),
                recommendations = ConferencePerformanceMonitor.getPerformanceRecommendations()
            )
            refreshCounter++
            delay(2000)
        }
    }
    
    // Simplified theme for compatibility
    MaterialTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Instant Jitsi Demo") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                InstantConferenceApp.startAggressivePreloading(context)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Overview
                item {
                    StatusOverviewCard(
                        isPreloaded = isPreloaded,
                        launchCount = launchStatistics?.launchCount ?: 0L,
                        lastLaunchTime = launchStatistics?.lastLaunchTime ?: 0L
                    )
                }
                
                // Performance Metrics
                item {
                    PerformanceMetricsCard(performanceMetrics = performanceMetrics)
                }
                
                // Launch Statistics
                item {
                    LaunchStatisticsCard(launchStatistics = launchStatistics)
                }
                
                // Performance Report
                item {
                    PerformanceReportCard(performanceReport = performanceReport)
                }
                
                // Quick Actions
                item {
                    QuickActionsCard(
                        isPreloaded = isPreloaded,
                        onLaunchAudioCall = { onLaunchConference("DemoAudioRoom", "Demo User", true) },
                        onLaunchVideoCall = { onLaunchConference("DemoVideoRoom", "Demo User", false) }
                    )
                }
                
                // Optimization Configuration
                item {
                    OptimizationConfigCard()
                }
                
                // Performance Warnings
                val report = performanceReport
                if (report?.warnings?.isNotEmpty() == true) {
                    item {
                        PerformanceWarningsCard(warnings = report.warnings)
                    }
                }
                
                // Performance Recommendations
                if (report?.recommendations?.isNotEmpty() == true) {
                    item {
                        PerformanceRecommendationsCard(recommendations = report.recommendations)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusOverviewCard(
    isPreloaded: Boolean,
    launchCount: Long,
    lastLaunchTime: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPreloaded) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isPreloaded) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = if (isPreloaded) "System Ready" else "Preloading...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "Launches: $launchCount",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            if (lastLaunchTime > 0) {
                Text(
                    text = "Last Launch: ${lastLaunchTime}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsCard(performanceMetrics: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            if (performanceMetrics.isEmpty()) {
                Text(
                    text = "No metrics available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                performanceMetrics.forEach { (metric, time) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = metric.replace("_", " ").uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (time >= 0) "${time}ms" else "FAILED",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (time >= 0) Color.White else Color(0xFFFFCDD2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchStatisticsCard(launchStatistics: InstantJitsiLauncher.LaunchStatistics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Launch Statistics",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            if (launchStatistics == null) {
                Text(
                    text = "No statistics available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = "Total Launches: ${launchStatistics.launchCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Text(
                    text = "Last Launch: ${if (launchStatistics.lastLaunchTime > 0) "${launchStatistics.lastLaunchTime}ms" else "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Text(
                    text = "System Ready: ${launchStatistics.isReady}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Text(
                    text = "Currently Launching: ${launchStatistics.isLaunching}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                if (launchStatistics.preloadTime > 0) {
                    Text(
                        text = "Preload Time: ${launchStatistics.preloadTime}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceReportCard(performanceReport: ConferencePerformanceMonitor.PerformanceReport?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (performanceReport?.isPerformanceGood == true) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Report",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            val report = performanceReport
            if (report == null) {
                Text(
                    text = "No report available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = "Performance: ${if (report.isPerformanceGood) "GOOD" else "NEEDS IMPROVEMENT"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Total Time: ${report.totalTime}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Text(
                    text = "Warnings: ${report.warnings.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Text(
                    text = "Recommendations: ${report.recommendations.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    isPreloaded: Boolean,
    onLaunchAudioCall: () -> Unit,
    onLaunchVideoCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF607D8B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunchAudioCall,
                    enabled = isPreloaded,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Call")
                }
                
                Button(
                    onClick = onLaunchVideoCall,
                    enabled = isPreloaded,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Video Call")
                }
            }
        }
    }
}

@Composable
private fun OptimizationConfigCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF795548))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Optimization Configuration",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = ConferenceOptimizationConfig.getOptimizationSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun PerformanceWarningsCard(warnings: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Warnings",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            warnings.forEach { warning ->
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PerformanceRecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Recommendations",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            recommendations.forEach { recommendation ->
                Text(
                    text = "• $recommendation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}
