package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.collector.HardwareCollector
import com.example.data.model.HardwareMetrics
import com.example.data.model.OverlayConfig
import com.example.data.preferences.PreferencesManager
import com.example.ui.components.FloatingOverlayView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingMonitorService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var samplingJob: Job? = null

    private lateinit var hardwareCollector: HardwareCollector
    private lateinit var prefsManager: PreferencesManager
    private lateinit var alertManager: com.example.data.collector.HardwareAlertManager

    private val currentMetrics = MutableStateFlow(HardwareMetrics())
    private val currentConfig = MutableStateFlow(OverlayConfig())

    // Drag tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        hardwareCollector = HardwareCollector(this)
        prefsManager = PreferencesManager(this)
        alertManager = com.example.data.collector.HardwareAlertManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("GPU Monitor 浮窗运行中", "正在实时监控硬件状态"))

        observeConfig()
        setupFloatingWindow()
        startMetricsSampling()
    }

    private fun observeConfig() {
        serviceScope.launch {
            prefsManager.configFlow.collect { config ->
                currentConfig.value = config
                // Re-apply layout params if transparency or position changed externally
                windowLayoutParams?.let { params ->
                    params.x = config.posX
                    params.y = config.posY
                    try {
                        floatingView?.let { windowManager?.updateViewLayout(it, params) }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val initialConfig = prefsManager.loadConfig()
        currentConfig.value = initialConfig

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialConfig.posX
            y = initialConfig.posY
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingMonitorService)
            setViewTreeViewModelStoreOwner(this@FloatingMonitorService)
            setViewTreeSavedStateRegistryOwner(this@FloatingMonitorService)

            setContent {
                MyApplicationTheme {
                    val metricsState = currentMetrics.value
                    val configState = currentConfig.value

                    FloatingOverlayView(
                        metrics = metricsState,
                        config = configState,
                        onDragDelta = { dx, dy ->
                            this@FloatingMonitorService.windowLayoutParams?.let { lp ->
                                lp.x = (lp.x + dx.toInt()).coerceAtLeast(0)
                                lp.y = (lp.y + dy.toInt()).coerceAtLeast(0)
                                try {
                                    windowManager?.updateViewLayout(floatingView, lp)
                                    prefsManager.updatePosition(lp.x, lp.y)
                                } catch (_: Exception) {}
                            }
                        },
                        onToggleCollapse = {
                            val newCollapsed = !configState.isCollapsed
                            prefsManager.updateCollapsed(newCollapsed)
                        },
                        onClose = {
                            stopSelf()
                        },
                        onToggleLock = {
                            val updated = configState.copy(isLocked = !configState.isLocked)
                            prefsManager.updateConfig(updated)
                        },
                        isInteractivePreview = false
                    )
                }
            }

            // Also attach raw touch listener for smooth WindowManager level dragging
            setOnTouchListener { _, event ->
                val config = currentConfig.value
                if (config.isLocked) return@setOnTouchListener false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = this@FloatingMonitorService.windowLayoutParams?.x ?: 0
                        initialY = this@FloatingMonitorService.windowLayoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (abs(dx) > 6 || abs(dy) > 6 || isDragging) {
                            isDragging = true
                            this@FloatingMonitorService.windowLayoutParams?.let { lp ->
                                lp.x = (initialX + dx).coerceAtLeast(0)
                                lp.y = (initialY + dy).coerceAtLeast(0)
                                try {
                                    windowManager?.updateViewLayout(floatingView, lp)
                                } catch (_: Exception) {}
                            }
                            true
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            this@FloatingMonitorService.windowLayoutParams?.let { lp ->
                                prefsManager.updatePosition(lp.x, lp.y)
                            }
                            isDragging = false
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
        }

        try {
            windowManager?.addView(floatingView, windowLayoutParams)
        } catch (_: Exception) {}
    }

    private fun startMetricsSampling() {
        samplingJob?.cancel()
        samplingJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val metrics = hardwareCollector.sampleMetrics()
                currentMetrics.value = metrics

                // Evaluate threshold alerts
                alertManager.evaluateMetrics(metrics, currentConfig.value)

                // Check interval
                val interval = currentConfig.value.updateIntervalMs.coerceIn(300L, 5000L)
                delay(interval)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPU & 硬件监控浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供屏幕悬浮硬件状态监控"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingMonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "关闭浮窗", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        samplingJob?.cancel()
        serviceScope.cancel()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null

        // Update prefs state to not enabled
        val current = prefsManager.loadConfig()
        prefsManager.updateConfig(current.copy(isOverlayEnabled = false))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "gpu_monitor_channel"
        const val NOTIFICATION_ID = 9527
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_FLOATING"

        fun startService(context: Context) {
            val intent = Intent(context, FloatingMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
