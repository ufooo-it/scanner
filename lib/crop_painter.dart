import 'package:flutter/material.dart';

class CropPainter extends CustomPainter {
  Offset tl, tr, bl, br;

  CropPainter(this.tl, this.tr, this.bl, this.br);

  Paint painter = Paint()
    ..color = Colors.blue
    ..strokeWidth = 2
    ..strokeCap = StrokeCap.round
    ..style = PaintingStyle.stroke;
  Paint painter1 = Paint()
    ..color = Colors.blue
    ..strokeWidth = 3
    ..strokeCap = StrokeCap.round;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawCircle(tl, 10, painter);
    canvas.drawCircle(tr, 10, painter);
    canvas.drawCircle(bl, 10, painter);
    canvas.drawCircle(br, 10, painter);
    // canvas.drawRRect(
    //     RRect.fromLTRBR(
    //         (tr.dx / 2) - 10, 10, (tr.dx / 2) + 10, 30, Radius.circular(10)),
    //     painter);
    // canvas.drawRRect(
    //     RRect.fromLTRBR(370, 420, 390, 380, Radius.circular(10)), painter);
    // canvas.drawRRect(
    //     RRect.fromLTRBR(180, 790, 220, 810, Radius.circular(10)), painter);
    // canvas.drawRRect(
    //     RRect.fromLTRBR(10, 420, 30, 380, Radius.circular(10)), painter);
    canvas.drawLine(tl.translate(11, 0), tr.translate(-11, 0), painter1);
    canvas.drawLine(tr.translate(0, 11), br.translate(0, -11), painter1);
    canvas.drawLine(br.translate(-11, 0), bl.translate(11, 0), painter1);
    canvas.drawLine(bl.translate(0, -11), tl.translate(0, 11), painter1);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => true;
}
