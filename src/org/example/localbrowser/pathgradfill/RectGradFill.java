package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * rect类型的渐变填充实现
 */
public class RectGradFill extends PathGradFillBase {
	public RectGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
	
	@Override
	public void gradFill() {
		updateNewColorPositions();
		
		PointF[] points = new PointF[4];
		points[0] = new PointF(tileRect.left, tileRect.top);
		points[1] = new PointF(tileRect.right,tileRect.top);
		points[2] = new PointF(tileRect.right, tileRect.bottom);
		points[3] = new PointF(tileRect.left, tileRect.bottom);
		setPoints(points);
		
		gradFillForLinesPath(getCenter(), true);
		
		// 翻转铺满剩下区域，假设周围各平铺一次，应当覆盖大部分的应用了吧
		PointF scalePoint = new PointF(tileRect.left, tileRect.top);
		if (tileRect.left > dstRect.left) 
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, 1);
		if (tileRect.top > dstRect.top)
			gradFillForLinesPath(getCenter(), true, scalePoint, 1, -1);
		if (tileRect.left > dstRect.left && tileRect.top > dstRect.top)
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, -1);
		
		scalePoint.set(tileRect.right, tileRect.bottom);
		if (tileRect.bottom < dstRect.bottom)
			gradFillForLinesPath(getCenter(), true, scalePoint, 1, -1);
		if (tileRect.right < dstRect.right)
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, 1);
		if (tileRect.bottom < dstRect.bottom && tileRect.right < dstRect.right)
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, -1);
		
		if (tileRect.top > dstRect.top && tileRect.right < dstRect.right) {
			scalePoint.set(tileRect.right, tileRect.top);
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, -1);
		}
		
		if (tileRect.bottom < dstRect.bottom && tileRect.left > dstRect.left) {
			scalePoint.set(tileRect.left, tileRect.bottom);
			gradFillForLinesPath(getCenter(), true, scalePoint, -1, -1);
		}
	}
	
	@Override
	protected float getFocusPercent() {
		if (fillToRect.width() == 0)
			return 0f;
		return fillToRect.width() / 2 / getTileRadius();
	}
	
	protected float getTileRadius() {
		float xOffset = fillToRect.centerX() - tileRect.left;
		float yOffset = fillToRect.centerY() - tileRect.top;
		float radius = Math.max(Math.abs(xOffset), Math.abs(yOffset));
		return radius;
	}
}
