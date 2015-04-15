package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

/**
 * circle类型的渐变填充实现
 */
public class CircleGradFill extends PathGradFillBase
{
	public CircleGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
	
	@Override
	public void gradFill() {
		updateNewColorPositions();
		fillForCircle(getCenter(), getTileRadius());
	}
	
	@Override
	public float getFocusPercent() {
		if (fillToRect.width() == 0 && fillToRect.height() == 0)
			return 0f;
		float focusRadius = Math.max(fillToRect.width(), fillToRect.height()) / 2; 
		return focusRadius / getTileRadius();
	}
	
	private float getTileRadius() {
		// 取离焦点最远的那个角来计算渐变半径
		float destX = fillToRect.centerX();
		float destY = fillToRect.centerY();
		if (destX < tileRect.width() / 2) {
			if (destY < tileRect.height() / 2) {
				destX = tileRect.right;
				destY = tileRect.bottom;
			} else {
				destX = tileRect.right;
				destY = tileRect.top;
			}
		} else{
			if (destY < tileRect.height() / 2) {
				destX = 0;
				destY = tileRect.bottom;
			} else {
				destX = 0;
				destY = 0;
			}
		}
		destX += tileRect.left;
		destY += tileRect.top;
		float radius = (float)Math.sqrt((fillToRect.centerX() - destX) *(fillToRect.centerX() - destX) +
				(fillToRect.centerY() - destY) * (fillToRect.centerY() - destY));
		return radius;
	}
}
