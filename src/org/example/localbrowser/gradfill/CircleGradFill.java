package org.example.localbrowser.gradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

/**
 * circle类型的渐变填充实现
 */
public class CircleGradFill extends PathGradFillBase
{
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
		
		RadialGradient shader = new RadialGradient(fillToRect.centerX(), fillToRect.centerY(),
				radius, colors, positions, TileMode.MIRROR);
		canvas.save();
		fillPaint.setShader(shader);
		canvas.drawPath(path, fillPaint);
		canvas.restore();
	}
}
