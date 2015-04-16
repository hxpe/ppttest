package org.example.localbrowser.pathgradfill;

import org.example.localbrowser.pathgradfill.PathGradFillBase.ITileFillAction;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Path.Direction;
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
		
		// 对于circle填充，tileRect进行修正
		float radius = (float)Math.sqrt((this.tileRect.width() / 2) *(this.tileRect.width() / 2) +
				(this.tileRect.height() / 2) * (this.tileRect.height() / 2));
		float centerX = this.tileRect.centerX();
		float centerY = this.tileRect.centerY();
		this.tileRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
	}
	
	@Override
	public void gradFill() {
		updateNewColorPositions();
		this.canvas.save();
		this.canvas.clipPath(this.path);
		fillForCircle(getCenter(), getTileRadius());
		
		if (haveMoreTile()) {
			tileGradFill(new ITileFillAction() {
				public void tileAction() {
					fillForCircle(getCenter(), getTileRadius());
				}
			});
		}
		this.canvas.restore();
	}
	
	/**
	 * Circle渐变填充
	 */
	private void fillForCircle(PointF centerF, float radius) {
		RadialGradient shader = new RadialGradient(centerF.x, centerF.y,
				radius, colors, positions, TileMode.CLAMP);
		canvas.save();
		fillPaint.setShader(shader);
		if (haveMoreTile()) {
			Path clipPath = new Path();
			clipPath.addCircle(tileRect.centerX(), tileRect.centerY(), 
					tileRect.width() / 2, Direction.CW);
			canvas.clipPath(clipPath);
		}
		canvas.drawPath(path, fillPaint);
		canvas.restore();
	}
	
	@Override
	public float getFocusPercent() {
		if (fillToRect.width() == 0 && fillToRect.height() == 0)
			return 0f;
		float focusRadius = Math.max(fillToRect.width(), fillToRect.height()) / 2; 
		return focusRadius / getTileRadius();
	}
	
	private float getTileRadius() {
		return tileRect.width() / 2;
	}
}
