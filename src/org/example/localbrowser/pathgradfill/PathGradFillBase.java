package org.example.localbrowser.pathgradfill;

import android.R.integer;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

/**
 * gradFill指定path(rect,circle,shape)的渐变填充的实现基类
 */
public abstract class PathGradFillBase {
	protected Path path;
	protected Canvas canvas;
	protected Paint fillPaint;
	protected RectF dstRect;
	protected RectF fillToRect;
	protected RectF tileRect;
	protected int[] colors;
	protected float[] positions;
	
	private PointF[] points;
	private Matrix matrix;
	private PointF tempCenterF;
	
	public PathGradFillBase(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		this.path = path;
		this.canvas = canvas;
		this.fillPaint = fillPaint;
		this.dstRect = dstRect;
		this.colors = colors;
		this.positions = positions;
		
		if (fillToRect == null)
			fillToRect = new RectF(1.0f, 1.0F, 0, 0); // 默认右下角
		if (tileRect == null)
			tileRect = new RectF(); // 默认占满dstRect
		this.fillToRect = transPercentageRect(fillToRect, dstRect);
		this.tileRect = transPercentageRect(tileRect, dstRect);
	}
	
	public void setPoints(PointF[] points) {
		this.points = points;
	}
	
	public PointF[] getPoints() {
		return this.points;
	}
	
	abstract void gradFill();
	abstract float getFocusPercent();
	
	/**
	 * Circle渐变的中心点
	 */
	protected PointF getCenter() {
		if (tempCenterF == null)
			tempCenterF = new PointF(fillToRect.centerX(), fillToRect.centerY());
		return tempCenterF;
	}
	
	/**
	 * Circle渐变填充
	 */
	protected void fillForCircle(PointF centerF, float radius) {
		RadialGradient shader = new RadialGradient(centerF.x, centerF.y,
				radius, colors, positions, TileMode.MIRROR);
		canvas.save();
		fillPaint.setShader(shader);
		canvas.drawPath(path, fillPaint);
		canvas.restore();
	}
	
	/**
	 * 根据焦点框所占比例获取调整后的渐变位置列表
	 * 只有焦点框不是一个点才需要
	 * @return
	 */
	protected void updateNewColorPositions() {
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
	
	/**
	 * 一个点到n条直线组成的闭合形状的渐变
	 */
	protected void gradFillForLinesPath(PointF gradCenter, boolean closePath) {
		if (points == null || points.length < 2)
			return;
		PointF starF = points[0];
		for (int i = 1; i < points.length; i++) {
			PointF endF = points[i];
			gradFillForTriangle(path, canvas, fillPaint, colors, positions, 
					gradCenter, starF, endF);
			starF = endF;
		}
		
		if (closePath) {
			gradFillForTriangle(path, canvas, fillPaint, colors, positions, 
					gradCenter, starF, points[0]);
		}
	}
	
	/**
	 * 附加翻转矩阵重新填充
	 */
	protected void gradFillForLinesPath(PointF gradCenter, boolean closePath, PointF applyScalePointF, float dx, float dy) {
		if (applyScalePointF == null)
			return;
		if (matrix == null)
			matrix = new Matrix();
		else 
			matrix.reset();
		matrix.preTranslate(applyScalePointF.x, applyScalePointF.y);
		matrix.preScale(dx, dy);
		matrix.preTranslate(-applyScalePointF.x, -applyScalePointF.y);
		
		canvas.save();
		canvas.concat(matrix);
		gradFillForLinesPath(gradCenter, closePath);
		canvas.restore();
	}
	
	/**
	 * 一个点到一条边的渐变，这里定义为点到垂足的渐变，triangle义指clipPath区域形状
	 */
	private void gradFillForTriangle(Path path, Canvas canvas, 
			Paint fillPaint, int[] colors, float[] positions,
			PointF gradCenter, PointF lineStart, PointF lineEnd) {
		PointF footPointF = getFootPoint(gradCenter, lineStart, lineEnd);
		if (gradCenter.x == footPointF.x &&
				gradCenter.y == footPointF.y) {
			// 点在直线上
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
				footPointF.x, footPointF.y, colors, positions, TileMode.MIRROR);
		fillPaint.setShader(lg);
		canvas.drawPath(path, fillPaint);
		canvas.restore();
	}
	
	/**
	 * 百分比到实际Rect的转换
	 * @param percentRect
	 * @param dstRect
	 */
	private RectF transPercentageRect(RectF percentRect, RectF dstRect) {
		float left = dstRect.left + dstRect.width() * percentRect.left;
		float top = dstRect.top + dstRect.height() * percentRect.top;
		float right = dstRect.right - dstRect.width() * percentRect.right;
		float bottom = dstRect.bottom - dstRect.height() * percentRect.bottom;
		RectF newRectF = new RectF(left, top, right, bottom);
		return newRectF;
	}
	
	/**
	 * 求一点在经过另两点直线的垂足（D）
	 * @param C
	 * @param A 
	 * @param B
	 * @return
	 */
	private PointF getFootPoint(PointF C, PointF A, PointF B) {
		PointF destPoint = new PointF();
		if (A.x == B.x) { 
			// 垂直
			destPoint.set(A.x, C.y);
		} else if (A.y == B.y) {
			// 水平
			destPoint.set(C.x, A.y);
		} else {
			/*
			 原直线AB方程式为：y-yA=k*（x-xA）,斜率公式:k=(yB-yA)/(xB-xA)
			   直线 外点C，设垂足D
			 	∵两条垂直直线的斜率乘积 = -1
				∴由AB线斜率为k可知CD线斜率为-1/k，可知直线CD方程式为y-yC=-1/k*（x-xC）
			这里已经排除斜率不存在的情况，联立二元方程组，接得：
				x = (k * xA+ xC / k + yC - yA) / (1 / k + k)
			再代入 BC方程得：
				y=k*(x-xA)+ yA
			 */
			float k = (B.y-A.y) / (B.x-A.x); 
			float x = (k * A.x+ C.x / k + C.y - A.y) / (1 / k + k);
			float y = k*(x-A.x)+ A.y;
			destPoint.set(x, y);
		}
		
		return destPoint;
	}
}
