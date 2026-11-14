package com.marciel.alertavelocidade

import android.content.Context

/** Estado compartilhado entre a tela e o serviço (tudo roda na thread principal). */
object SpeedState {
    private const val PREFS = "alerta_prefs"
    private const val KEY_LIMITE = "limite"

    var rodando = false
        set(value) { field = value; notificar() }

    /** Velocidade atual em km/h, ou null se ainda não há sinal de GPS. */
    var velocidade: Float? = null
        set(value) { field = value; notificar() }

    var listener: (() -> Unit)? = null

    private fun notificar() { listener?.invoke() }

    fun limite(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_LIMITE, 60)

    fun salvarLimite(ctx: Context, valor: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_LIMITE, valor).apply()
    }
}
