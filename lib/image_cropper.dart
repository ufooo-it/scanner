import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scanner/crop_painter.dart';
import 'package:scanner/image_preview.dart';

class ImageCropper extends StatefulWidget {
  final String fileUri;
  final List<dynamic> listPoint;
  ImageCropper({
    Key key,
    this.fileUri,
    this.listPoint,
  }) : super(key: key);

  @override
  _ImageCropperState createState() => _ImageCropperState();
}

class _ImageCropperState extends State<ImageCropper> {
  bool isReady = false;
  Uint8List byte;
  Offset tl, tr, br, bl;
  double dxRatio;
  double dyRatio;
  double height;
  double width;

  @override
  void initState() {
    super.initState();
    getImageInfo();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Cắt ảnh"),
        actions: [
          IconButton(
              icon: Icon(Icons.arrow_forward),
              onPressed: () {
                cropImage();
              })
        ],
      ),
      body: SafeArea(
        child: GestureDetector(
          onPanDown: (details) {
            double x1 = details.localPosition.dx;
            double y1 = details.localPosition.dy;
            double x2 = tl.dx;
            double y2 = tl.dy;
            double x3 = tr.dx;
            double y3 = tr.dy;
            double x4 = bl.dx;
            double y4 = bl.dy;
            double x5 = br.dx;
            double y5 = br.dy;
            if (sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) < 30 &&
                x1 >= 0 &&
                y1 >= 0 &&
                x1 < width / 2 &&
                y1 < height / 2) {
              print(details.localPosition);
              setState(() {
                tl = details.localPosition;
              });
            } else if (sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1)) <
                    30 &&
                x1 >= width / 2 &&
                y1 >= 0 &&
                x1 < width &&
                y1 < height / 2) {
              setState(() {
                tr = details.localPosition;
              });
            } else if (sqrt((x4 - x1) * (x4 - x1) + (y4 - y1) * (y4 - y1)) <
                    30 &&
                x1 >= 0 &&
                y1 >= height / 2 &&
                x1 < width / 2 &&
                y1 < height) {
              setState(() {
                bl = details.localPosition;
              });
            } else if (sqrt((x5 - x1) * (x5 - x1) + (y5 - y1) * (y5 - y1)) <
                    30 &&
                x1 >= width / 2 &&
                y1 >= height / 2 &&
                x1 < width &&
                y1 < height) {
              setState(() {
                br = details.localPosition;
              });
            }
          },
          onPanUpdate: (details) {
            double x1 = details.localPosition.dx;
            double y1 = details.localPosition.dy;
            double x2 = tl.dx;
            double y2 = tl.dy;
            double x3 = tr.dx;
            double y3 = tr.dy;
            double x4 = bl.dx;
            double y4 = bl.dy;
            double x5 = br.dx;
            double y5 = br.dy;
            if (sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) < 30 &&
                x1 >= 0 &&
                y1 >= 0 &&
                x1 < width / 2 &&
                y1 < height / 2) {
              print(details.localPosition);
              setState(() {
                tl = details.localPosition;
              });
            } else if (sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1)) <
                    30 &&
                x1 >= width / 2 &&
                y1 >= 0 &&
                x1 < width &&
                y1 < height / 2) {
              setState(() {
                tr = details.localPosition;
              });
            } else if (sqrt((x4 - x1) * (x4 - x1) + (y4 - y1) * (y4 - y1)) <
                    30 &&
                x1 >= 0 &&
                y1 >= height / 2 &&
                x1 < width / 2 &&
                y1 < height) {
              setState(() {
                bl = details.localPosition;
              });
            } else if (sqrt((x5 - x1) * (x5 - x1) + (y5 - y1) * (y5 - y1)) <
                    30 &&
                x1 >= width / 2 &&
                y1 >= height / 2 &&
                x1 < width &&
                y1 < height) {
              setState(() {
                br = details.localPosition;
              });
            }
          },
          child: Stack(
            children: [
              Container(
                child: Image.file(
                  File(widget.fileUri),
                ),
              ),
              isReady
                  ? CustomPaint(
                      painter: CropPainter(tl, tr, bl, br),
                    )
                  : Container(),
            ],
          ),
        ),
      ),
    );
  }

  getImageInfo() async {
    var image = File(widget.fileUri);
    var decodedImage = await decodeImageFromList(image.readAsBytesSync());
    var points = widget.listPoint;

    setState(() {
      width = MediaQuery.of(context).size.width;
      height = width / 0.5675;
      dxRatio = decodedImage.width / width;
      dyRatio = decodedImage.height / height;
    });

    if (points != null) {
      setState(() {
        tl = Offset(points[0]["x"] / dyRatio, points[0]["y"] / dxRatio);
        tr = Offset(points[1]["x"] / dyRatio, points[1]["y"] / dxRatio);
        br = Offset(points[2]["x"] / dyRatio, points[2]["y"] / dxRatio);
        bl = Offset(points[3]["x"] / dyRatio, points[3]["y"] / dxRatio);
        isReady = true;
      });
    } else {
      setState(() {
        tl = Offset(20, 20);
        tr = Offset(width - 20, 20);
        br = Offset(width - 20, height - 20);
        bl = Offset(20, height - 20);
        isReady = true;
      });
    }
  }

  cropImage() async {
    try {
      MethodChannel channel = new MethodChannel("opencv");
      var image = File(widget.fileUri);
      var bytes = await channel.invokeMethod("cropImage", {
        "byteArray": image.readAsBytesSync(),
        "listPoint": [
          {"x": tr.dy * dxRatio, "y": tr.dx * dyRatio},
          {"x": br.dy * dxRatio, "y": br.dx * dyRatio},
          {"x": bl.dy * dxRatio, "y": bl.dx * dyRatio},
          {"x": tl.dy * dxRatio, "y": tl.dx * dyRatio},
        ]
      });
      print(
          "x: ${tr.dy * dxRatio}, y: ${tr.dx * dyRatio}, x: ${br.dy * dxRatio}, y: ${br.dx * dyRatio}, x: ${bl.dy * dxRatio}, y: ${bl.dx * dyRatio}, x: ${tl.dy * dxRatio}, y: ${tl.dx * dyRatio}");
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => ImagePreview(
            byte: bytes,
          ),
        ),
      );
    } catch (e) {
      print(e);
    }
  }
}
