package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * rect类型的渐变填充实现
 */
public class RectGradFill extends PathGradFillBase {
	@Override
	public void gradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		if (fillToRect == null)
			fillToRect = new RectF(1.0f, 1.0F, 0, 0); // 默认右下角
		if (tileRect == null)
			tileRect = new RectF(); // 默认占满dstRect
		
		fillToRect = transPercentageRect(fillToRect, dstRect);
		tileRect = transPercentageRect(tileRect, dstRect);
		
		float radius = getRadius(fillToRect, tileRect);
		
		if (fillToRect.width() > 0){
			// 渐变位置从焦点框外算起
			positions = getNewColorPositions(positions, fillToRect.width() / 2 / radius);
		}
		
		PointF gradCenter = new PointF(fillToRect.centerX(), fillToRect.centerY());
		
		// left triangle
		PointF lineStart = new PointF(tileRect.left, tileRect.top);
		PointF lineEnd = new PointF(tileRect.left, tileRect.bottom);
		gradFillForTriangle(path, canvas, fillPaint, colors, positions, gradCenter, lineStart, lineEnd);
		
		// top triangle
		lineEnd.set(tileRect.right,tileRect.top);
		gradFillForTriangle(path, canvas, fillPaint, colors, positions, gradCenter, lineStart, lineEnd);
		
		// right triangle
		lineStart.set(tileRect.right, tileRect.bottom);
		gradFillForTriangle(path, canvas, fillPaint, colors, positions, gradCenter, lineStart, lineEnd);
		
		// bottom triangle
		lineEnd.set(tileRect.left, tileRect.bottom);
		gradFillForTriangle(path, canvas, fillPaint, colors, positions, gradCenter, lineStart, lineEnd);
	}
}
