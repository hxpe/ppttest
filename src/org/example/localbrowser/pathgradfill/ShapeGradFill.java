package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * shpae类型的渐变填充实现
 */
public class ShapeGradFill extends RectGradFill {
	public ShapeGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
	@Override
	public void gradFill() {
		// 暂时没法提供基于任意形状路径的PathGradient，转作rect实现处理
		if (getPoints() == null) {
			super.gradFill();
		} else {
			updateNewColorPositions();
			gradFillForLinesPath(getCenter(), true);
		}
	}
}
