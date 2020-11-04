import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:camera/camera.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:scanner/image_cropper.dart';
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
  bool isBusy = false;
  bool isAuto = true;
  bool isDetect = false;
  Uint8List imageBytes;
  MethodChannel channel = MethodChannel("opencv");
  double width;
  double height;
  Directory extDir;
  String dirPath;
  var tl = new Offset(20, 100);
  var tr = new Offset(400, 100);
  var bl = new Offset(20, 400);
  var br = new Offset(400, 400);

  @override
  void initState() {
    super.initState();
    initializeCamera();
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
                Container(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      AspectRatio(
                        aspectRatio: _controller.value.aspectRatio,
                        child: CameraPreview(_controller),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          CupertinoButton(
                            child: Text("Huỷ"),
                            onPressed: () {
                              Navigator.pop(context);
                            },
                          ),
                          CupertinoButton(
                            child: Text("Chụp"),
                            onPressed: () {
                              takePicture();
                            },
                          ),
                          CupertinoButton(
                            child: Text(
                              "Auto",
                              style: TextStyle(
                                color:
                                    isAuto ? Colors.grey[800] : Colors.grey[50],
                              ),
                            ),
                            onPressed: () {
                              autoDetect();
                            },
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                isDetect
                    ? CustomPaint(painter: RectPainter(tl, tr, bl, br))
                    : Container(),
              ],
            ),
          )
        : Container();
  }

  initializeCamera() async {
    _cameras = await availableCameras();
    _controller = CameraController(_cameras[0], ResolutionPreset.max);
    _controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        isReady = true;
        width = MediaQuery.of(context).size.width;
        height = width / _controller.value.aspectRatio;
      });
    });
    var _extDir = await getApplicationDocumentsDirectory();
    var _dirPath = '${_extDir.path}/Pictures/flutter_test';
    await Directory(_dirPath).create(recursive: true);
    setState(() {
      extDir = _extDir;
      dirPath = _dirPath;
    });
  }

  autoDetect() async {
    setState(() {
      isAuto = !isAuto;
    });
    if (isAuto) {
      _controller.stopImageStream();
      return;
    }
    try {
      _controller.startImageStream((image) async {
        var startTime = DateTime.now();
        if (isBusy) {
          return;
        }
        setState(() {
          isBusy = true;
        });
        Timer(Duration(milliseconds: 300), () async {
          try {
            var listPoint = await channel.invokeMethod(
              "streamDetect",
              {
                'y': image.planes[0].bytes,
                // 'u': image.planes[1].bytes.buffer.asUint8List(),
                // 'v': image.planes[2].bytes.buffer.asUint8List(),
                'height': image.height,
                'width': image.width,
              },
            );
            var size = _controller.value.previewSize;
            print("${size.height} : ${size.width}");
            print("$height: $width");
            print(listPoint);
            var dxRaito = size.height / width;
            var dyRaito = size.width / height;
            setState(() {
              // imageBytes = listPoint;
              if (listPoint != null) {
                tl = Offset(
                    listPoint[0]["x"] / dxRaito, listPoint[0]["y"] / dyRaito);
                tr = Offset(
                    listPoint[1]["x"] / dxRaito, listPoint[1]["y"] / dyRaito);
                br = Offset(
                    listPoint[2]["x"] / dxRaito, listPoint[2]["y"] / dyRaito);
                bl = Offset(
                    listPoint[3]["x"] / dxRaito, listPoint[3]["y"] / dyRaito);
                isDetect = true;
              } else {
                isDetect = false;
              }
              isBusy = false;
            });
            print(DateTime.now().difference(startTime).inMilliseconds);
          } catch (e) {
            print(e);
          }
        });
      });
    } catch (e) {
      print(e);
    }
  }

  takePicture() async {
    var startTime = DateTime.now();
    final String filePath =
        '$dirPath/${DateTime.now().millisecondsSinceEpoch.toString()}.jpg';
    await _controller.takePicture(filePath);
    try {
      MethodChannel channel = MethodChannel("opencv");
      var image = File(filePath);
      var listPoint = await channel.invokeMethod(
          "pictureDetect", {'byteArray': image.readAsBytesSync()});
      print(listPoint);
      print(DateTime.now().difference(startTime).inMilliseconds);
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => ImageCropper(
            fileUri: filePath,
            listPoint: listPoint,
          ),
        ),
      );
    } catch (e) {
      print(e);
    }
  }
}
