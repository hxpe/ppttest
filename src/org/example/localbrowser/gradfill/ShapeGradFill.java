package org.example.localbrowser.gradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * shpae类型的渐变填充实现
 */
public class ShapeGradFill extends RectGradFill {
	@Override
	public void gradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		// 暂时没法提供基于任意形状路径的PathGradient，转作rect实现处理
		super.gradFill(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
}
