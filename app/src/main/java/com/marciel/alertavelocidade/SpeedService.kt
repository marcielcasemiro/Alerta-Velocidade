package com.marciel.alertavelocidade

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.roundToInt

class SpeedService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private var tone: ToneGenerator? = null
    private var ultimoAlerta = 0L
    private var ultimaLocalizacao: Location? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        criarCanal()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tone = try { ToneGenerator(AudioManager.STREAM_ALARM, 100) } catch (e: RuntimeException) { null }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = montarNotificacao("Procurando sinal de GPS…")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
            return START_NOT_STICKY
        }

        SpeedState.velocidade = null
        SpeedState.rodando = true
        return START_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        tone?.release()
        tone = null
        SpeedState.rodando = false
        SpeedState.velocidade = null
        super.onDestroy()
    }

    override fun onLocationChanged(location: Location) {
        val kmh = calcularKmh(location)
        ultimaLocalizacao = location
        SpeedState.velocidade = kmh

        val limite = SpeedState.limite(this)
        val texto = "${kmh.roundToInt()} km/h  (limite $limite)"
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, montarNotificacao(texto))

        if (kmh > limite) alertar()
    }

    /** Usa a velocidade informada pelo GPS; se não houver, calcula pela distância entre leituras. */
    private fun calcularKmh(atual: Location): Float {
        if (atual.hasSpeed()) return atual.speed * 3.6f
        val anterior = ultimaLocalizacao ?: return 0f
        val segundos = (atual.elapsedRealtimeNanos - anterior.elapsedRealtimeNanos) / 1e9f
        if (segundos <= 0f) return SpeedState.velocidade ?: 0f
        return atual.distanceTo(anterior) / segundos * 3.6f
    }

    private fun alertar() {
        val agora = SystemClock.elapsedRealtime()
        if (agora - ultimoAlerta < INTERVALO_ALERTA_MS) return
        ultimoAlerta = agora

        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)

        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }

    private fun criarCanal() {
        val canal = NotificationChannel(CANAL_ID, "Monitoramento de velocidade", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun montarNotificacao(texto: String): Notification {
        val abrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.ic_speed)
            .setContentTitle("Alerta Velocidade")
            .setContentText(texto)
            .setContentIntent(abrir)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    // Necessários em Android < 10 (métodos abstratos nas versões antigas).
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    companion object {
        private const val CANAL_ID = "velocidade"
        private const val NOTIF_ID = 1
        private const val INTERVALO_ALERTA_MS = 3000L
    }
}
