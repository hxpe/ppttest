package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

/**
 * gradFill指定path(rect,circle,shape)的渐变填充的实现基类
 */
public abstract class PathGradFillBase {
	abstract void gradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions);
	
	/**
	 * 根据焦点框所占比例获取调整后的渐变位置列表
	 * @param positions
	 * @param focusPercent
	 * @return
	 */
	protected float[] getNewColorPositions(float[] positions, float focusPercent) {
		float remain = 1 - focusPercent;
		float[] newPosition = new float[positions.length];
		for (int i = 0; i < positions.length; i++) {
			newPosition[i] = focusPercent + positions[i] * remain;
		}
		
		return newPosition;
	}
	
	/**
	 * 获取焦点Rect到tileRect的水平或垂直距离，取最大
	 * @param fillToRect
	 * @param tileRect
	 * @return
	 */
	protected float getRadius(RectF fillToRect, RectF tileRect) {
		float xOffset = fillToRect.centerX() - tileRect.left;
		float yOffset = fillToRect.centerY() - tileRect.top;
		return Math.max(Math.abs(xOffset), Math.abs(yOffset));
	}
	
	/**
	 * 百分比到实际Rect的转换
	 * @param percentRect
	 * @param dstRect
	 */
	protected RectF transPercentageRect(RectF percentRect, RectF dstRect) {
		float left = dstRect.left + dstRect.width() * percentRect.left;
		float top = dstRect.top + dstRect.height() * percentRect.top;
		float right = dstRect.right - dstRect.width() * percentRect.right;
		float bottom = dstRect.bottom - dstRect.height() * percentRect.bottom;
		RectF newRectF = new RectF(left, top, right, bottom);
		return newRectF;
	}
	
	/**
	 * 一个点到一条边的渐变，这里定义为点到垂足的渐变，triangle义指clipPath区域形状
	 */
	protected void gradFillForTriangle(Path path, Canvas canvas, 
			Paint fillPaint, int[] colors, float[] positions,
			PointF gradCenter, PointF lineStart, PointF lineEnd) {
		// 简化垂足计算，这里认为直线是水平或垂直的
		float lineCenterX = (lineStart.x + lineEnd.x) / 2;
		float lineCenterY = (lineStart.y + lineEnd.y) / 2;
		if (lineStart.x == lineEnd.x) { 
			// 垂直
			lineCenterY = gradCenter.y;
		} else if (lineStart.y == lineEnd.y) {
			// 水平
			lineCenterX = gradCenter.x;
		} else {
			return;
		}
				
		// 剪切三角形区域
		Path trianglePath = new Path();
		trianglePath.moveTo(gradCenter.x, gradCenter.y);
		trianglePath.lineTo(lineStart.x, lineStart.y);
		trianglePath.lineTo(lineEnd.x, lineEnd.y);
		trianglePath.close();
		
		canvas.save();
		canvas.clipPath(trianglePath);
		LinearGradient lg = new LinearGradient(gradCenter.x, gradCenter.y, 
				lineCenterX, lineCenterY, colors, positions, TileMode.MIRROR);
		fillPaint.setShader(lg);
		canvas.drawPath(path, fillPaint);
		canvas.restore();
	}
}
