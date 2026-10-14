package pl.fotopuzzle

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var puzzle: PuzzleView
    private lateinit var movesTv: TextView
    private lateinit var timerTv: TextView
    private lateinit var hint: TextView
    private lateinit var group: MaterialButtonToggleGroup
    private var square: Bitmap? = null
    private var startTime = 0L
    private var elapsedFinal = -1L
    private var cameraUri: Uri? = null
    private val ticker = object : Runnable {
        override fun run() { updateTimer(); timerTv.postDelayed(this, 500) }
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { load(it) } }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let { load(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        puzzle = findViewById(R.id.puzzle); movesTv = findViewById(R.id.moves); timerTv = findViewById(R.id.timer)
        hint = findViewById(R.id.hint); group = findViewById(R.id.difficulty)
        savedInstanceState?.getString("cam")?.let { cameraUri = Uri.parse(it) }

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            val dir = File(cacheDir, "photos").apply { mkdirs() }
            val f = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            cameraUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            try { takePhoto.launch(cameraUri!!) } catch (e: Exception) { toast("Brak aplikacji aparatu") }
        }
        findViewById<Button>(R.id.btnPreview).setOnClickListener { showPreview() }
        findViewById<Button>(R.id.btnShuffle).setOnClickListener { if (square != null) newGame() else toast("Najpierw wybierz zdjęcie") }
        group.addOnButtonCheckedListener { _, _, checked -> if (checked && square != null && puzzle.moves == 0) newGame() }

        puzzle.onMove = { movesTv.text = "Ruchy: $it" }
        puzzle.onSolved = { win() }

        // restore state after rotation
        savedInstanceState?.let { s ->
            val path = s.getString("img") ?: return@let
            val bmp = BitmapFactory.decodeFile(path) ?: return@let
            square = bmp
            puzzle.restore(bmp, s.getInt("n"), s.getIntArray("order")!!, s.getInt("moves"))
            startTime = SystemClock.elapsedRealtime() - s.getLong("elapsed")
            elapsedFinal = s.getLong("final", -1)
            movesTv.text = "Ruchy: ${puzzle.moves}"; hint.visibility = TextView.GONE
        }
    }

    override fun onResume() { super.onResume(); timerTv.post(ticker) }
    override fun onPause() { super.onPause(); timerTv.removeCallbacks(ticker) }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        cameraUri?.let { out.putString("cam", it.toString()) }
        val b = square ?: return
        val f = File(cacheDir, "current.png")
        try { f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } } catch (_: Exception) { return }
        out.putString("img", f.absolutePath); out.putInt("n", gridSize()); out.putIntArray("order", puzzle.order)
        out.putInt("moves", puzzle.moves); out.putLong("elapsed", SystemClock.elapsedRealtime() - startTime); out.putLong("final", elapsedFinal)
    }

    private fun gridSize() = when (group.checkedButtonId) { R.id.d4 -> 4; R.id.d5 -> 5; else -> 3 }

    private fun load(uri: Uri) {
        try {
            square = decodeSquare(uri, 1200) ?: run { toast("Nie udało się wczytać zdjęcia"); return }
            newGame()
        } catch (e: Exception) { toast("Błąd wczytywania: ${e.message}") }
    }

    private fun newGame() {
        val n = gridSize()
        // make bitmap width divisible by n
        val b = square!!; val s = b.width - b.width % n
        val bmp = if (s == b.width) b else Bitmap.createBitmap(b, 0, 0, s, s)
        hint.visibility = TextView.GONE
        puzzle.start(bmp, n)
        startTime = SystemClock.elapsedRealtime(); elapsedFinal = -1
        updateTimer()
    }

    private fun updateTimer() {
        if (!puzzle.hasGame()) return
        val ms = if (elapsedFinal >= 0) elapsedFinal else SystemClock.elapsedRealtime() - startTime
        timerTv.text = "Czas: " + fmt(ms)
    }

    private fun fmt(ms: Long): String { val s = ms / 1000; return "%02d:%02d".format(s / 60, s % 60) }

    private fun win() {
        elapsedFinal = SystemClock.elapsedRealtime() - startTime; updateTimer()
        MaterialAlertDialogBuilder(this)
            .setTitle("Gratulacje! 🎉")
            .setMessage("Ułożono puzzle ${gridSize()}×${gridSize()}\nRuchy: ${puzzle.moves}\nCzas: ${fmt(elapsedFinal)}")
            .setPositiveButton("Zagraj ponownie") { _, _ -> newGame() }
            .setNegativeButton("Zamknij", null)
            .show()
    }

    private fun showPreview() {
        val b = square ?: run { toast("Najpierw wybierz zdjęcie"); return }
        val iv = ImageView(this).apply { setImageBitmap(b); adjustViewBounds = true; setPadding(24, 24, 24, 24) }
        MaterialAlertDialogBuilder(this).setTitle("Podgląd").setView(iv).setPositiveButton("OK", null).show()
    }

    private fun decodeSquare(uri: Uri, target: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth <= 0) return null
        var sample = 1
        while (min(opts.outWidth, opts.outHeight) / (sample * 2) >= target) sample *= 2
        val raw = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rot = contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
            }
        } ?: 0f
        val side = min(raw.width, raw.height)
        val m = Matrix().apply { val sc = min(1f, target.toFloat() / side); postScale(sc, sc); postRotate(rot) }
        return Bitmap.createBitmap(raw, (raw.width - side) / 2, (raw.height - side) / 2, side, side, m, true)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
