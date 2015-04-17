package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * 线性(Linear)类型的渐变填充实现，规范是对应<lin>节点，不是<path>节点,但借用后者一些公用实现方法
 *
 */
public class LinGradFill extends PathGradFillBase {
	private boolean scaled = false;
	private float angle = 90;
	
	public LinGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
	
	public void setLinParam(boolean scaled, float angle) {
		this.scaled = scaled;
		this.angle = scaleAngle(scaled, angle);
	}
	
	@Override
	public void gradFill() {
		
		// 始终以左上角为渐变起点，并根据角度寻找渐变终点
		PointF start = new PointF(tileRect.left, tileRect.top);
		PointF end = null;
		
		int sx = 0; // 辅助点相对于tileWith的倍数
		int sy = 0; // 辅助点相对于tileHeight的倍数
		if (angle == 0) {
			sx = 1;
		} else if(angle < 90) {
			sx = 1;
			sy = 1;
		} else if (angle == 90) {
			sy = 1;
		} else if (angle < 180) {
			sx = -1;
			sy = 1;
		} else if (angle == 180) {
			sx = -1;
		} else if (angle < 270) {
			sx = -1;
			sy = -1;
		} else if (angle == 270) {
			sy = -1;
		} else {
			sx = 1;
			sy = -1;
		}
		
		end = new PointF(tileRect.left + tileRect.width() * sx, tileRect.top + tileRect.height() * sy);
		if (sx != 0 && sy != 0) {
			// 纠正为stat到过end的直线的垂足
			end = getFootPoint((float)Math.tan(Math.toRadians(angle)), start, end);
		}
		
		float dx = 0;
		float dy = 0;
		// end不在 tileRect内，要做平移
		if (sx < 0)
			dx = tileRect.width();
		if (sy < 0)
			dy = tileRect.height();
		lineGradFill(null, start, end, dx, dy);
	}
	
	private float scaleAngle(boolean scaled, float angle) {
		angle = (angle + 360) % 360;
		if (scaled) {
			if (angle < 45) {
				angle = 0;
			} else if (angle < 90) {
				angle = 90 - (float)Math.toDegrees(Math.atan(tileRect.height() / tileRect.width()));
			} else if (angle < 135) {
				angle = 90;
			} else if (angle < 180) {
				angle = 90 + (float)Math.toDegrees(Math.atan(tileRect.height() / tileRect.width()));
			} else if (angle < 225) {
				angle = 180;
			} else if (angle < 270) {
				angle = 270 - (float)Math.toDegrees(Math.atan(tileRect.height() / tileRect.width()));
			} else if (angle < 315) {
				angle = 270;
			} else if (angle < 360) {
				angle = 270 + (float)Math.toDegrees(Math.atan(tileRect.height() / tileRect.width()));
			}
		}
		return angle;
	}
	
	@Override
	protected float getFocusPercent() {
		return 0;
	}

}
