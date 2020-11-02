package com.example.scanner

import android.graphics.*
import android.graphics.Rect
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.CvType.CV_8UC3
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_YUV2RGB_NV21
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class MainActivity : FlutterActivity() {
    private val CHANNEL = "opencv"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (OpenCVLoader.initDebug()) {
                if (call.method == "streamDetect") {
                    val y = call.argument<ByteArray>("y")
//                    val u = call.argument<ByteArray>("u")
//                    val v = call.argument<ByteArray>("v")
                    val width = call.argument<Int>("width")
                    val height = call.argument<Int>("height")

//                    val yBuffer: ByteBuffer = ByteBuffer.wrap(y)
//                    val uBuffer: ByteBuffer = ByteBuffer.wrap(u)
//                    val vBuffer: ByteBuffer = ByteBuffer.wrap(v)
//                    val ySize = yBuffer.remaining()
//                    val uSize = uBuffer.remaining()
//                    val vSize = vBuffer.remaining()
//                    val nv21 = ByteArray(ySize)

//                    yBuffer.get(nv21, 0, ySize)
//                    vBuffer.get(nv21, ySize, vSize)
//                    uBuffer.get(nv21, ySize + vSize, uSize)

                    val yuv = YuvImage(y, ImageFormat.NV21, width ?: 1080, height ?: 1920, null)
                    val out = ByteArrayOutputStream()
                    yuv.compressToJpeg(Rect(0, 0, width ?: 1920, height ?: 1080), 100, out)
                    val bytes = out.toByteArray()
                    out.close()

                    val conners = processPicture(bytes)
                    val listPoint = conners?.corners?.let { toDartPoint(it) }
                    result.success(listPoint)
                }

                if (call.method == "pictureDetect") {
                    val byteArray = call.argument<ByteArray>("byteArray")
                    if (byteArray != null) {
                        val conners = processPicture(byteArray)
                        if (conners?.corners?.count() == 4) {
                            val listPoint = toDartPoint(conners.corners)
                            result.success(listPoint)
                        } else {
                            result.success(null)
                        }
                    } else {
                        result.success(null)
                    }
                }
            }
        }
    }

    fun processPicture(byteArray: ByteArray): Corners? {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val mat = Mat()
        println("flutter: ${bitmap.height}: ${bitmap.width}")
        Utils.bitmapToMat(bitmap, mat)
        bitmap.recycle()
        Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE)
        val contours = findContours(mat)
        if (contours.count() > 0) {
            val conners = getCorners(contours, mat.size())

            return conners
        }
        return null
    }

    fun enhancePicture(src: Bitmap?): Bitmap {
        val src_mat = Mat()
        Utils.bitmapToMat(src, src_mat)
        Imgproc.cvtColor(src_mat, src_mat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.adaptiveThreshold(src_mat, src_mat, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15.0)
        val result = Bitmap.createBitmap(src?.width ?: 1080, src?.height
                ?: 1920, Bitmap.Config.RGB_565)
        Utils.matToBitmap(src_mat, result, true)
        src_mat.release()

        return result
    }

    private fun findContours(src: Mat): ArrayList<MatOfPoint> {

        val grayImage: Mat
        val cannedImage: Mat
        val kernel: Mat = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(9.0, 9.0))
        val dilate: Mat
        val size = Size(src.size().width, src.size().height)
        grayImage = Mat(size, CvType.CV_8UC4)
        cannedImage = Mat(size, CvType.CV_8UC1)
        dilate = Mat(size, CvType.CV_8UC1)

        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(grayImage, grayImage, Size(5.0, 5.0), 0.0)
        Imgproc.threshold(grayImage, grayImage, 20.0, 255.0, Imgproc.THRESH_TRIANGLE)
        Imgproc.Canny(grayImage, cannedImage, 75.0, 200.0)
        Imgproc.dilate(cannedImage, dilate, kernel)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(dilate, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        contours.sortByDescending { p: MatOfPoint -> Imgproc.contourArea(p) }
        hierarchy.release()
        grayImage.release()
        cannedImage.release()
        kernel.release()
        dilate.release()

        return contours
    }

    private fun getCorners(contours: ArrayList<MatOfPoint>, size: Size): Corners? {
        val indexTo: Int
        when (contours.size) {
            in 0..5 -> indexTo = contours.size - 1
            else -> indexTo = 4
        }
        for (index in 0..contours.size) {
            if (index in 0..indexTo) {
                val c2f = MatOfPoint2f(*contours[index].toArray())
                val peri = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
                val points = approx.toArray().asList()
                // select biggest 4 angles polygon
                if (points.size == 4) {
                    val foundPoints = sortPoints(points)
                    return Corners(foundPoints, size)
                }
            } else {
                return null
            }
        }
        return null
    }

    private fun sortPoints(points: List<Point>): List<Point> {
        val p0 = points.minBy { point -> point.x + point.y } ?: Point()
        val p1 = points.maxBy { point -> point.x - point.y } ?: Point()
        val p2 = points.maxBy { point -> point.x + point.y } ?: Point()
        val p3 = points.minBy { point -> point.x - point.y } ?: Point()

        return listOf(p0, p1, p2, p3)
    }

    fun toDartPoint(conners: List<Point?>): List<Map<String, Double>> {
        val listPoint = mutableListOf<Map<String, Double>>()
        for (conner in conners) {
            if (conner != null) {
                listPoint.add(mapOf("x" to conner.x, "y" to conner.y))
            }
        }
        return listPoint
    }
}

data class Corners(val corners: List<Point?>, val size: Size)
