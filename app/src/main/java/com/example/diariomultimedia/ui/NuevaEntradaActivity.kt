package com.example.diariomultimedia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.diariomultimedia.R
import com.example.diariomultimedia.data.Entrada
import com.example.diariomultimedia.viemodels.DiarioViewModel
import java.io.File

class NuevaEntradaActivity : AppCompatActivity() {

    private val vm: DiarioViewModel by viewModels()

    // Estado multimedia
    private var recorder:   MediaRecorder? = null
    private var grabando    = false
    private var rutaAudio   = ""
    private var fotoUri: Uri? = null
    private var videoUri: Uri? = null
    private var rutaFoto: String? = null
    private var rutaVideo: String? = null

    // Vistas
    private lateinit var etTitulo:       EditText
    private lateinit var btnAudio:       Button
    private lateinit var btnFoto:        Button
    private lateinit var btnImportarFoto: Button
    private lateinit var btnVideo:       Button
    private lateinit var btnGuardar:     Button
    private lateinit var imgPreviewFoto: ImageView
    private lateinit var videoPreview:   VideoView

    // ── Launchers ─────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarGrabacion()
        else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
    }

    private val fotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            imgPreviewFoto.visibility = View.VISIBLE
            Glide.with(this).load(fotoUri).centerCrop().into(imgPreviewFoto)
        }
    }

    private val videoLauncher = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { exito ->
        if (exito) {
            videoPreview.visibility = View.VISIBLE
            videoPreview.setVideoURI(videoUri)
            videoPreview.start()
        }
    }

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val copia = copiarFotoAlmacenamientoPrivado(uri)
        if (copia != null) {
            rutaFoto = copia.absolutePath
            fotoUri = Uri.fromFile(copia)
            imgPreviewFoto.visibility = View.VISIBLE
            Glide.with(this).load(fotoUri).centerCrop().into(imgPreviewFoto)
        } else {
            Toast.makeText(this, "No se pudo importar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_entrada)
        supportActionBar?.title = "Nueva entrada"

        etTitulo       = findViewById(R.id.etTitulo)
        btnAudio       = findViewById(R.id.btnGrabarAudio)
        btnFoto        = findViewById(R.id.btnTomarFoto)
        btnImportarFoto = findViewById(R.id.btnImportarFoto)
        btnVideo       = findViewById(R.id.btnGrabarVideo)
        btnGuardar     = findViewById(R.id.btnGuardar)
        imgPreviewFoto = findViewById(R.id.imgPreviewFoto)
        videoPreview   = findViewById(R.id.videoPreview)

        btnAudio.setOnClickListener    { onAudioClick() }
        btnFoto.setOnClickListener     { onFotoClick() }
        btnImportarFoto.setOnClickListener { onImportarFotoClick() }
        btnVideo.setOnClickListener    { onVideoClick() }
        btnGuardar.setOnClickListener  { onGuardarClick() }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::videoPreview.isInitialized) videoPreview.stopPlayback()
        detenerGrabacionSiActiva()
    }

    override fun onStop() {
        super.onStop()
        detenerGrabacionSiActiva()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::videoPreview.isInitialized) videoPreview.stopPlayback()
        recorder?.apply { stop(); release() }
        recorder = null
    }

    // ── Audio ─────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onAudioClick() {
        if (!grabando) {
            val permiso = Manifest.permission.RECORD_AUDIO
            if (checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED) {
                iniciarGrabacion()
            } else {
                permLauncher.launch(permiso)
            }
        } else {
            detenerGrabacionSiActiva()
            Toast.makeText(this, "Audio guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarGrabacion() {
        rutaAudio = "${getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder  = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(rutaAudio)
                prepare()
                start()
            }
        }
        grabando       = true
        btnAudio.text  = "⏹ Detener grabación"
    }

    private fun detenerGrabacionSiActiva() {
        if (grabando) {
            recorder?.apply { stop(); release() }
            recorder      = null
            grabando       = false
            btnAudio.text  = "🎙 Grabar audio"
        }
    }

    // ── Foto ──────────────────────────────────────────────────────────────

    private fun onFotoClick() {
        val archivo = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "foto_${System.currentTimeMillis()}.jpg"
        )
        rutaFoto = archivo.absolutePath
        fotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivo)
        fotoLauncher.launch(fotoUri!!)
    }

    private fun onImportarFotoClick() {
        galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun copiarFotoAlmacenamientoPrivado(origen: Uri): File? {
        return try {
            val dir = File(filesDir, "fotos_importadas")
            if (!dir.exists()) dir.mkdirs()
            val destino = File(dir, "foto_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(origen)?.use { input ->
                destino.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destino
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── Video ─────────────────────────────────────────────────────────────

    private fun onVideoClick() {
        val archivo = File(
            getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "video_${System.currentTimeMillis()}.mp4"
        )
        rutaVideo = archivo.absolutePath
        videoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", archivo)
        videoLauncher.launch(videoUri!!)
    }

    // ── Guardar ───────────────────────────────────────────────────────────

    private fun onGuardarClick() {
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            etTitulo.error = "El título es obligatorio"
            return
        }
        if (rutaAudio.isEmpty() && fotoUri == null && videoUri == null) {
            Toast.makeText(this, "Agrega al menos audio, foto o video", Toast.LENGTH_SHORT).show()
            return
        }
        vm.guardar(
            Entrada(
                titulo = titulo,
                rutaAudio = rutaAudio.ifEmpty { null },
                rutaFoto = rutaFoto,
                rutaVideo = rutaVideo
            )
        )
        setResult(RESULT_OK)
        finish()
    }

}
