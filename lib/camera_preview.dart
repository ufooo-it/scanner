import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:scanner/rect_painter.dart';

class Scanner extends StatefulWidget {
  Scanner({Key key}) : super(key: key);

  @override
  _ScannerState createState() => _ScannerState();
}

class _ScannerState extends State<Scanner> {
  List<CameraDescription> _cameras;
  CameraController _controller;
  bool isReady = false;
  var tl = new Offset(20, 20);
  var tr = new Offset(400, 20);
  var bl = new Offset(20, 400);
  var br = new Offset(400, 400);

  @override
  void initState() {
    super.initState();
    getCamera();
  }

  getCamera() async {
    _cameras = await availableCameras();
    _controller = CameraController(_cameras[0], ResolutionPreset.max);
    _controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        isReady = true;
      });
      // _controller.startImageStream((image) => print(image));
    });
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return isReady
        ? SafeArea(
            child: Stack(
              children: [
                Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      AspectRatio(
                        aspectRatio: _controller.value.aspectRatio,
                        child: CameraPreview(_controller),
                      ),
                    ],
                  ),
                ),
                CustomPaint(
                  painter: RectPainter(tl, tr, bl, br),
                ),
              ],
            ),
          )
        : Container();
  }
}
