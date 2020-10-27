import 'package:flutter/material.dart';

class RectPainter extends CustomPainter {
  Offset tl, tr, bl, br;

  RectPainter(this.tl, this.tr, this.bl, this.br);

  Paint painter = Paint()
    ..color = Colors.blue
    ..strokeWidth = 2
    ..strokeCap = StrokeCap.round
    ..style = PaintingStyle.stroke;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawLine(tl, tr, painter);
    canvas.drawLine(tr, br, painter);
    canvas.drawLine(br, bl, painter);
    canvas.drawLine(bl, tl, painter);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}
