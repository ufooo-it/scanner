import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';

class ImagePreview extends StatelessWidget {
  final Uint8List byte;
  ImagePreview({Key key, this.byte}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Image Preview"),
      ),
      body: Container(
        child: Transform(
          alignment: Alignment.center,
          transform: Matrix4.rotationY(pi),
          child: RotatedBox(
            quarterTurns: 1,
            child: Image.memory(
              byte,
            ),
          ),
        ),
      ),
    );
  }
}
