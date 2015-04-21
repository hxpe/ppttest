package org.example.localbrowser.pathgradfill;


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
	private PointF tempCenterF;

	public CircleGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);

		if (haveMoreTile()) {
			// 对于circle填充，tileRect进行修正
			float radius = (float)Math.sqrt((this.tileRect.width() / 2) *(this.tileRect.width() / 2) +
					(this.tileRect.height() / 2) * (this.tileRect.height() / 2));
			float centerX = this.tileRect.centerX();
			float centerY = this.tileRect.centerY();
			this.tileRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
		}
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
	
	private PointF getCenter() {
		if (tempCenterF == null)
			tempCenterF = new PointF(fillToRect.centerX(), fillToRect.centerY());
		return tempCenterF;
	}
	
	/**
	 * 根据焦点框所占比例获取调整后的渐变位置列表
	 * 只有焦点框不是一个点才需要
	 * @return
	 */
	private void updateNewColorPositions() {
		if (positions == null)
			return;
		float focusPercent = getFocusPercent();
		if (focusPercent == 0)
			return;
		float remain = 1 - focusPercent;
		float[] newPosition = new float[positions.length];
		for (int i = 0; i < positions.length; i++) {
			newPosition[i] = focusPercent + positions[i] * remain;
		}
		
		positions = newPosition;
	}
	
	private float getFocusPercent() {
		if (fillToRect.width() == 0 && fillToRect.height() == 0)
			return 0f;
		float focusRadius = Math.max(fillToRect.width(), fillToRect.height()) / 2; 
		return focusRadius / getTileRadius();
	}
	
	private float getTileRadius() {
        float focusRadius = 0;
        if (!haveMoreTile()) {
            // 取离焦点最远的那个角来计算渐变半径
            float destX = fillToRect.centerX();
            float destY = fillToRect.centerY();
            if (destX < tileRect.centerX()) {
                if (destY < tileRect.centerY()) {
                    destX = tileRect.right;
                    destY = tileRect.bottom;
                } else {
                    destX = tileRect.right;
                    destY = tileRect.top;
                }
            } else {
                if (destY < tileRect.centerY()) {
                    destX = tileRect.left;
                    destY = tileRect.bottom;
                } else {
                    destX = tileRect.left;
                    destY = tileRect.top;
                }
            }
            focusRadius = (float) Math.sqrt((fillToRect.centerX() - destX) * (fillToRect.centerX() - destX) +
                    (fillToRect.centerY() - destY) * (fillToRect.centerY() - destY));
        }
        if (focusRadius == 0)
            focusRadius = tileRect.width() / 2;
		return focusRadius;
	}
}
