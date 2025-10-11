package com.secretspaces32.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

@Composable
fun SecretSpacesLogo(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE85D75)
) {
    Canvas(modifier = modifier.size(120.dp)) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            // Top part of the "S" - upper curve and rectangle
            moveTo(width * 0.7f, height * 0.15f)
            lineTo(width * 0.85f, height * 0.15f)
            lineTo(width * 0.85f, height * 0.35f)
            cubicTo(
                width * 0.85f, height * 0.42f,
                width * 0.78f, height * 0.48f,
                width * 0.5f, height * 0.48f
            )
            lineTo(width * 0.3f, height * 0.48f)
            lineTo(width * 0.3f, height * 0.35f)
            lineTo(width * 0.5f, height * 0.35f)
            cubicTo(
                width * 0.65f, height * 0.35f,
                width * 0.7f, height * 0.3f,
                width * 0.7f, height * 0.25f
            )
            lineTo(width * 0.7f, height * 0.15f)

            // Add the top-left corner piece
            moveTo(width * 0.15f, height * 0.15f)
            lineTo(width * 0.4f, height * 0.15f)
            lineTo(width * 0.4f, height * 0.4f)
            lineTo(width * 0.15f, height * 0.4f)
            close()

            // Bottom part of the "S" - lower curve and rectangle
            moveTo(width * 0.3f, height * 0.85f)
            lineTo(width * 0.15f, height * 0.85f)
            lineTo(width * 0.15f, height * 0.65f)
            cubicTo(
                width * 0.15f, height * 0.58f,
                width * 0.22f, height * 0.52f,
                width * 0.5f, height * 0.52f
            )
            lineTo(width * 0.7f, height * 0.52f)
            lineTo(width * 0.7f, height * 0.65f)
            lineTo(width * 0.5f, height * 0.65f)
            cubicTo(
                width * 0.35f, height * 0.65f,
                width * 0.3f, height * 0.7f,
                width * 0.3f, height * 0.75f
            )
            lineTo(width * 0.3f, height * 0.85f)

            // Add the bottom-right corner piece
            moveTo(width * 0.85f, height * 0.85f)
            lineTo(width * 0.6f, height * 0.85f)
            lineTo(width * 0.6f, height * 0.6f)
            lineTo(width * 0.85f, height * 0.6f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}
