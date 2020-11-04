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
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

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
                    val listPoint = conners?.corners?.let { pointToMap(it) }
                    result.success(listPoint)
                }

                if (call.method == "pictureDetect") {
                    val byteArray = call.argument<ByteArray>("byteArray")
                    if (byteArray != null) {
                        val conners = processPicture(byteArray)
                        if (conners?.corners?.count() == 4) {
                            val listPoint = pointToMap(conners.corners)
                            result.success(listPoint)
                        } else {
                            result.success(null)
                        }
                    } else {
                        result.success(null)
                    }
                }

                if (call.method == "cropImage") {

                    val byteArray = call.argument<ByteArray>("byteArray")
                    val listMap = call.argument<List<Map<String, Double>>>("listPoint")

                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray?.size!!)
                    val pts = mapToPoint(listMap!!)

                    val out = ByteArrayOutputStream()
                    val mat = Mat()

                    Utils.bitmapToMat(bitmap, mat)
                    bitmap.recycle()

                    val croppedBitmap = cropPicture(mat, pts)
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    croppedBitmap.recycle()

                    val bytes = out.toByteArray()
                    out.close()

                    result.success(bytes)
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

    fun pointToMap(listPoint: List<Point?>): List<Map<String, Double>> {
        val listMap = mutableListOf<Map<String, Double>>()
        for (point in listPoint) {
            if (point != null) {
                listMap.add(mapOf("x" to point.x, "y" to point.y))
            }
        }
        return listMap
    }

    fun mapToPoint(listMap: List<Map<String, Double>>): List<Point> {
        val listPoint = listMap.map { Point(it.get("x")!!, it.get("y")!!) }

        return listPoint
    }

    fun cropPicture(picture: Mat, pts: List<Point>): Bitmap {

        val tl = pts[0]
        val tr = pts[1]
        val br = pts[2]
        val bl = pts[3]

        val widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0))
        val widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0))

        val dw = Math.max(widthA, widthB)
        val maxWidth = java.lang.Double.valueOf(dw).toInt()

        val heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0))
        val heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0))

        val dh = Math.max(heightA, heightB)
        val maxHeight = java.lang.Double.valueOf(dh).toInt()

        val croppedPic = Mat(maxHeight, maxWidth, CvType.CV_8UC4)

        val src_mat = Mat(4, 1, CvType.CV_32FC2)
        val dst_mat = Mat(4, 1, CvType.CV_32FC2)

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh)

        val m = Imgproc.getPerspectiveTransform(src_mat, dst_mat)

        Imgproc.warpPerspective(picture, croppedPic, m, croppedPic.size())
        m.release()
        src_mat.release()
        dst_mat.release()

        val bitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
//        Core.rotate(croppedPic, croppedPic, Core.ROTATE_90_CLOCKWISE)
        Utils.matToBitmap(croppedPic, bitmap)
        return bitmap
    }

//    println("original started")
//    OpenCVLoader.initDebug()
//    val mat = Mat()
//    Utils.bitmapToMat(bitmap, mat)
//    val src_mat = Mat(4, 1, CvType.CV_32FC2)
//    val dst_mat = Mat(4, 1, CvType.CV_32FC2)
//    src_mat.put(0, 0, tl_x, tl_y, tr_x, tr_y, bl_x, bl_y, br_x, br_y)
//    dst_mat.put(0, 0, 0.0, 0.0, width.toDouble(), 0.0, 0.0, height.toDouble(), width.toDouble(), height.toDouble())
//    val perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat)
//    Imgproc.warpPerspective(mat, mat, perspectiveTransform, Size(width.toDouble(), height.toDouble()))
//    Utils.matToBitmap(mat, bitmap)
//    bitmap = Bitmap.createScaledBitmap(bitmap, 2480, 3508, true)
//    val stream = ByteArrayOutputStream()
//    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//    val byteArray = stream.toByteArray()
//    originalArray = byteArray
}

data class Corners(val corners: List<Point?>, val size: Size)
