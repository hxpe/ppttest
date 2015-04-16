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
		
		this.canvas.save();
		this.canvas.clipPath(this.path);
		gradFillForLinesPath(getCenter(), true);
		
		if (haveMoreTile()) {
			tileGradFill(new ITileFillAction() {
				public void tileAction() {
					gradFillForLinesPath(getCenter(), true);
					
				}
			});
		}
		this.canvas.restore();
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
