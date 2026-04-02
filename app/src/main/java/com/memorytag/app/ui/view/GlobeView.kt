package com.memorytag.app.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.math.PI

/**
 * Vue custom qui dessine un globe simplifié avec :
 * - Un cercle représentant la Terre
 * - Des lignes de latitude/longitude (graticule)
 * - Des continents stylisés (formes simplifiées)
 * - Un point rouge animé pour la localisation
 *
 * Usage XML :
 *   <com.memorytag.app.ui.view.GlobeView
 *       android:id="@+id/globeView"
 *       android:layout_width="200dp"
 *       android:layout_height="200dp" />
 *
 * Usage code :
 *   globeView.setLocation(48.8566, 2.3522) // Paris
 */
class GlobeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ─── Coordonnées de la localisation ─────────────────────────────────────
    private var latitude = 0.0
    private var longitude = 0.0

    // ─── Paints ──────────────────────────────────────────────────────────────

    // Fond du globe — dégradé bleu océan
    private val oceanPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Lignes du graticule (latitude/longitude)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1AFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    // Continents
    private val landPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF82") // Vert naturel
        style = Paint.Style.FILL
    }

    // Contour du globe
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Point rouge de localisation
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.FILL
    }

    // Halo autour du point rouge
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FF3B30")
        style = Paint.Style.FILL
    }

    // Surbrillance (reflet en haut à gauche)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ─── API publique ────────────────────────────────────────────────────────

    /**
     * Définit la localisation à afficher sur le globe.
     * @param lat Latitude en degrés (-90 à 90)
     * @param lon Longitude en degrés (-180 à 180)
     */
    fun setLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
        invalidate()
    }

    // ─── Dessin ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) * 0.88f

        // 1. Fond océan avec dégradé
        oceanPaint.shader = RadialGradient(
            cx - radius * 0.2f, cy - radius * 0.2f, radius * 1.2f,
            intArrayOf(
                Color.parseColor("#1A6B9E"),
                Color.parseColor("#0D4A73"),
                Color.parseColor("#071F30")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, oceanPaint)

        // 2. Clip pour que tout reste dans le cercle
        canvas.save()
        canvas.clipPath(Path().also { it.addCircle(cx, cy, radius, Path.Direction.CW) })

        // 3. Graticule (lignes de grille projetées en perspective)
        drawGraticule(canvas, cx, cy, radius)

        // 4. Continents stylisés
        drawContinents(canvas, cx, cy, radius)

        canvas.restore()

        // 5. Contour du globe
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // 6. Reflet (surbrillance)
        highlightPaint.shader = RadialGradient(
            cx - radius * 0.3f, cy - radius * 0.35f, radius * 0.55f,
            intArrayOf(Color.parseColor("#40FFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, highlightPaint)

        // 7. Point de localisation
        drawLocationDot(canvas, cx, cy, radius)
    }

    /**
     * Dessine le graticule : lignes de latitude et longitude projetées
     * en perspective simple (projection orthographique).
     * Centre du globe = (0°lat, longitude cible)
     */
    private fun drawGraticule(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        // Lignes de latitude : -60, -30, 0, 30, 60
        for (latDeg in listOf(-60, -30, 0, 30, 60)) {
            val latRad = Math.toRadians(latDeg.toDouble())
            val projY = r * sin(latRad).toFloat()
            val projR = r * cos(latRad).toFloat()
            // Ellipse projetée
            val rect = RectF(cx - projR, cy - projR * 0.3f - projY, cx + projR, cy + projR * 0.3f - projY)
            canvas.drawOval(rect, gridPaint)
        }

        // Lignes de longitude : 6 méridiens
        for (i in 0 until 6) {
            val angle = (i * 30f) * PI.toFloat() / 180f
            canvas.drawLine(
                cx + r * cos(angle + PI.toFloat() / 2), cy - r * sin(angle + PI.toFloat() / 2),
                cx - r * cos(angle + PI.toFloat() / 2), cy + r * sin(angle + PI.toFloat() / 2),
                gridPaint
            )
        }
    }

    /**
     * Dessine des formes simplifiées représentant les continents.
     * Coordonnées en "espace globe" [-1..1] puis converties en pixels.
     */
    private fun drawContinents(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        // Projection : lat/lon → x,y sur la sphère
        fun project(lat: Double, lon: Double): PointF {
            val latR = Math.toRadians(lat)
            val lonR = Math.toRadians(lon - longitude) // Centre sur notre longitude
            val x = (r * cos(latR) * sin(lonR)).toFloat()
            val y = -(r * sin(latR)).toFloat()
            // On affiche seulement la face visible (cos(lon) > 0)
            return PointF(cx + x, cy + y)
        }

        fun isVisible(lat: Double, lon: Double): Boolean {
            val lonR = Math.toRadians(lon - longitude)
            return cos(lonR) > -0.2 // Demi-sphère visible + léger débordement
        }

        // ── Europe / Afrique ────────────────────────────────────────────────
        val euroAfrica = listOf(
            Pair(35.0, -5.0), Pair(36.0, 10.0), Pair(30.0, 32.0),
            Pair(10.0, 40.0), Pair(-35.0, 27.0), Pair(-34.0, 18.0),
            Pair(-17.0, 12.0), Pair(5.0, -5.0), Pair(35.0, -5.0)
        )
        drawContinent(canvas, euroAfrica, ::project, ::isVisible)

        // ── Amérique du Nord ─────────────────────────────────────────────────
        val northAmerica = listOf(
            Pair(70.0, -140.0), Pair(70.0, -70.0), Pair(45.0, -55.0),
            Pair(25.0, -80.0), Pair(15.0, -85.0), Pair(20.0, -105.0),
            Pair(30.0, -110.0), Pair(60.0, -140.0), Pair(70.0, -140.0)
        )
        drawContinent(canvas, northAmerica, ::project, ::isVisible)

        // ── Asie ─────────────────────────────────────────────────────────────
        val asia = listOf(
            Pair(70.0, 30.0), Pair(75.0, 130.0), Pair(55.0, 150.0),
            Pair(20.0, 120.0), Pair(5.0, 100.0), Pair(10.0, 75.0),
            Pair(25.0, 55.0), Pair(40.0, 40.0), Pair(70.0, 30.0)
        )
        drawContinent(canvas, asia, ::project, ::isVisible)
    }

    private fun drawContinent(
        canvas: Canvas,
        points: List<Pair<Double, Double>>,
        project: (Double, Double) -> PointF,
        isVisible: (Double, Double) -> Boolean
    ) {
        val path = Path()
        var started = false
        for ((lat, lon) in points) {
            if (!isVisible(lat, lon)) {
                started = false
                continue
            }
            val p = project(lat, lon)
            if (!started) {
                path.moveTo(p.x, p.y)
                started = true
            } else {
                path.lineTo(p.x, p.y)
            }
        }
        path.close()
        canvas.drawPath(path, landPaint)
    }

    /**
     * Projette la lat/lon du souvenir sur le globe et dessine un point rouge.
     * Halo pulsant pour attirer l'œil.
     */
    private fun drawLocationDot(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val latR = Math.toRadians(latitude)
        val lonR = Math.toRadians(longitude - longitude) // Toujours centré → 0

        val dotX = cx + (r * cos(latR) * sin(lonR)).toFloat()
        val dotY = cy - (r * sin(latR)).toFloat()

        // Halo externe
        canvas.drawCircle(dotX, dotY, 18f, haloPaint)
        // Point rouge principal
        canvas.drawCircle(dotX, dotY, 9f, dotPaint)
        // Point blanc central
        canvas.drawCircle(dotX, dotY, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        })
    }
}
