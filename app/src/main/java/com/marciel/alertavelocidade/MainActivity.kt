package com.marciel.alertavelocidade

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var txtSpeed: TextView
    private lateinit var txtLimite: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnIniciar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = findViewById(R.id.root)
        txtSpeed = findViewById(R.id.txtSpeed)
        txtLimite = findViewById(R.id.txtLimite)
        txtStatus = findViewById(R.id.txtStatus)
        btnIniciar = findViewById(R.id.btnIniciar)

        findViewById<Button>(R.id.btnMenos).setOnClickListener { mudarLimite(-5) }
        findViewById<Button>(R.id.btnMais).setOnClickListener { mudarLimite(+5) }
        btnIniciar.setOnClickListener {
            if (SpeedState.rodando) parar() else iniciarComPermissao()
        }
    }

    override fun onStart() {
        super.onStart()
        SpeedState.listener = { atualizarTela() }
        atualizarTela()
    }

    override fun onStop() {
        super.onStop()
        SpeedState.listener = null
    }

    private fun mudarLimite(delta: Int) {
        val novo = (SpeedState.limite(this) + delta).coerceIn(10, 200)
        SpeedState.salvarLimite(this, novo)
        atualizarTela()
    }

    private fun atualizarTela() {
        val limite = SpeedState.limite(this)
        txtLimite.text = limite.toString()

        val v = SpeedState.velocidade
        txtSpeed.text = if (SpeedState.rodando && v != null) v.roundToInt().toString() else "--"

        val acima = SpeedState.rodando && v != null && v > limite
        root.setBackgroundColor(if (acima) Color.parseColor("#FFB71C1C") else Color.parseColor("#FF121212"))

        txtStatus.text = when {
            !SpeedState.rodando -> "Parado"
            v == null -> "Procurando sinal de GPS…"
            acima -> "ACIMA DO LIMITE!"
            else -> "Monitorando"
        }
        btnIniciar.text = if (SpeedState.rodando) "Parar" else "Iniciar"
    }

    private fun iniciarComPermissao() {
        val faltando = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            faltando += Manifest.permission.ACCESS_FINE_LOCATION
            faltando += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            faltando += Manifest.permission.POST_NOTIFICATIONS
        }
        if (faltando.isEmpty()) iniciar() else requestPermissions(faltando.toTypedArray(), REQ_PERM)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_PERM) return
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            iniciar()
        } else {
            Toast.makeText(this, "Sem permissão de localização o app não consegue medir a velocidade.", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciar() {
        startForegroundService(Intent(this, SpeedService::class.java))
    }

    private fun parar() {
        stopService(Intent(this, SpeedService::class.java))
    }

    companion object {
        private const val REQ_PERM = 1
    }
}
