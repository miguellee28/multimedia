package com.example.diariomultimedia.ui

import android.widget.MediaController
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.diariomultimedia.R
import com.example.diariomultimedia.data.Entrada
import com.example.diariomultimedia.viemodels.DiarioViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "com.utp.diariomultimedia.EXTRA_ID"
    }

    private val vm: DiarioViewModel by viewModels()
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var seekBar: SeekBar? = null
    private var tvTiempoActual: TextView? = null
    private var tvTiempoTotal: TextView? = null
    private var draggingSeekBar = false

    private val updaterRunnable = object : Runnable {
        override fun run() {
            player?.let { mp ->
                if (!draggingSeekBar && mp.isPlaying) {
                    val pos = mp.currentPosition
                    seekBar?.progress = pos
                    tvTiempoActual?.text = formatTime(pos)
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id == -1L) { finish(); return }

        vm.cargarPorId(id)

        lifecycleScope.launch {
            vm.entradaSeleccionada.collect { entrada ->
                entrada ?: return@collect
                mostrar(entrada)
            }
        }
    }

    private fun mostrar(entrada: Entrada) {
        val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
        supportActionBar?.title = entrada.titulo

        findViewById<TextView>(R.id.tvTituloDetalle).text = entrada.titulo
        findViewById<TextView>(R.id.tvFechaDetalle).text  = sdf.format(Date(entrada.fecha))

        // ── Foto ──────────────────────────────────────────────────────────
        val imgFoto    = findViewById<ImageView>(R.id.imgFotoDetalle)
        val sectionFoto = findViewById<View>(R.id.sectionFoto)
        if (entrada.rutaFoto != null) {
            sectionFoto.visibility = View.VISIBLE
            Glide.with(this)
                .load(Uri.parse(entrada.rutaFoto))
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgFoto)
        } else {
            sectionFoto.visibility = View.GONE
        }

        // ── Audio ─────────────────────────────────────────────────────────
        val sectionAudio = findViewById<View>(R.id.sectionAudio)
        val btnPlay      = findViewById<Button>(R.id.btnReproducirAudio)
        seekBar = findViewById(R.id.seekBarAudio)
        tvTiempoActual = findViewById(R.id.tvTiempoActual)
        tvTiempoTotal = findViewById(R.id.tvTiempoTotal)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
                draggingSeekBar = true
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                draggingSeekBar = false
            }
        })

        if (entrada.rutaAudio != null) {
            sectionAudio.visibility = View.VISIBLE
            btnPlay.setOnClickListener {
                if (player?.isPlaying == true) {
                    player?.pause()
                    handler.removeCallbacks(updaterRunnable)
                    btnPlay.text = "▶ Reproducir audio"
                } else {
                    reproducirAudio(entrada.rutaAudio, btnPlay)
                }
            }
        } else {
            sectionAudio.visibility = View.GONE
        }

        // ── Video ─────────────────────────────────────────────────────────
        val sectionVideo = findViewById<View>(R.id.sectionVideo)
        val videoView    = findViewById<VideoView>(R.id.videoViewDetalle)
        if (entrada.rutaVideo != null) {
            sectionVideo.visibility = View.VISIBLE
            val mc = MediaController(this)
            mc.setAnchorView(videoView)
            videoView.setMediaController(mc)
            videoView.setVideoURI(Uri.parse(entrada.rutaVideo))
            videoView.setOnCompletionListener {
                Toast.makeText(this, "Video terminado", Toast.LENGTH_SHORT).show()
            }
        } else {
            sectionVideo.visibility = View.GONE
        }
    }

    private fun reproducirAudio(ruta: String, btnPlay: Button) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(ruta)
            prepare()
            seekBar?.max = duration
            tvTiempoTotal?.text = formatTime(duration)
            tvTiempoActual?.text = formatTime(0)
            start()
        }
        btnPlay.text = "⏸ Pausar audio"
        handler.removeCallbacks(updaterRunnable)
        handler.post(updaterRunnable)
        player?.setOnCompletionListener {
            handler.removeCallbacks(updaterRunnable)
            seekBar?.progress = 0
            tvTiempoActual?.text = formatTime(0)
            it.release()
            player = null
            btnPlay.text = "▶ Reproducir audio"
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${sec.toString().padStart(2, '0')}"
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        findViewById<VideoView>(R.id.videoViewDetalle).pause()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updaterRunnable)
        player?.apply {
            stop()
            release()
        }
        player = null
        seekBar?.progress = 0
        tvTiempoActual?.text = "0:00"
        tvTiempoTotal?.text = "0:00"
        findViewById<VideoView>(R.id.videoViewDetalle).stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updaterRunnable)
        player?.apply {
            stop()
            release()
        }
        player = null
        findViewById<VideoView>(R.id.videoViewDetalle).stopPlayback()
    }
}
