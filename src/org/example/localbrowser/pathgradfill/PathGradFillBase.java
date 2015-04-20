package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
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
	
	static public interface ITileFillAction {
		void tileAction();
	}
	
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
			fillToRect = new RectF(0, 0, 1.0f, 1.0f); // 默认左上角
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
	
	public abstract void gradFill();
	protected abstract float getFocusPercent();
	
	/**
	 * Circle渐变的中心点
	 */
	protected PointF getCenter() {
		if (tempCenterF == null)
			tempCenterF = new PointF(fillToRect.centerX(), fillToRect.centerY());
		return tempCenterF;
	}
	
	/**
	 * 是否需要更多平铺填充，tileRect不等于或内含于dstRect，则返回true
	 * @return
	 */
	protected boolean haveMoreTile() {
		return tileRect.left > dstRect.left ||
				tileRect.top > dstRect.top ||
				tileRect.right < dstRect.right ||
				tileRect.bottom < dstRect.bottom;
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
			gradFillForTriangle(gradCenter, starF, endF);
			starF = endF;
		}
		
		if (closePath) {
			gradFillForTriangle(gradCenter, starF, points[0]);
		}
	}
	
	/**
	 * tileRect周围的区域重复应用平铺动作
	 */
	protected void tileGradFill(ITileFillAction action) {
		if (action == null)
			return;
		// 翻转铺满剩下区域，假设周围各平铺一次，应当覆盖大部分的应用了吧
		if (tileRect.left > dstRect.left) 
			rotateToTile(action, tileRect.left, tileRect.top, -1, 1);
		if (tileRect.top > dstRect.top)
			rotateToTile(action, tileRect.left, tileRect.top, 1, -1);
		if (tileRect.left > dstRect.left && tileRect.top > dstRect.top)
			rotateToTile(action, tileRect.left, tileRect.top, -1, -1);
		
		if (tileRect.bottom < dstRect.bottom)
			rotateToTile(action, tileRect.right, tileRect.bottom, 1, -1);
		if (tileRect.right < dstRect.right)
			rotateToTile(action, tileRect.right, tileRect.bottom, -1, 1);
		if (tileRect.bottom < dstRect.bottom && tileRect.right < dstRect.right)
			rotateToTile(action, tileRect.right, tileRect.bottom, -1, -1);
		
		if (tileRect.top > dstRect.top && tileRect.right < dstRect.right) 
			rotateToTile(action, tileRect.right, tileRect.top, -1, -1);
		
		if (tileRect.bottom < dstRect.bottom && tileRect.left > dstRect.left) 
			rotateToTile(action, tileRect.left, tileRect.bottom, -1, -1);
	}
	
	/**
	 * 附加翻转矩阵重新填充
	 */
	private void rotateToTile(ITileFillAction action, float rotateX, float rotateY, float dx, float dy) {
		if (action == null)
			return;
		if (matrix == null)
			matrix = new Matrix();
		else 
			matrix.reset();
		matrix.preTranslate(rotateX, rotateY);
		matrix.preScale(dx, dy);
		matrix.preTranslate(-rotateX, -rotateY);
		
		canvas.save();
		canvas.concat(matrix);
		action.tileAction();
		canvas.restore();
	}
	
	/**
	 * 一个点到一条边的渐变，这里定义为点到垂足的渐变，triangle名称义指clipPath区域形状
	 */
	protected void gradFillForTriangle(PointF gradCenter, PointF lineStart, PointF lineEnd) {
		PointF footPointF = getFootPoint(gradCenter, lineStart, lineEnd);
		if (footPointF == null && gradCenter.x == footPointF.x &&
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
		
		lineGradFill(trianglePath, gradCenter, footPointF, 0, 0);
	}

    /**
     * 线性渐变
     * @param clippath
     * @param start
     * @param end
     * @param dx
     * @param dy
     */
	protected void lineGradFill(Path clippath, PointF start, PointF end,
			float dx, float dy) {
		canvas.save();
		if (clippath != null)
			canvas.clipPath(clippath);
		LinearGradient lg = new LinearGradient(start.x, start.y, 
				end.x, end.y, colors, positions, TileMode.MIRROR);
		if (dx != 0 || dy != 0) {
			Matrix matrix = new Matrix();
			matrix.preTranslate(dx, dy);
			lg.setLocalMatrix(matrix);
		}
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
	 * 求一点到直线(另两点决定)的垂足（D）
	 * @param C 直线外一点
	 * @param A 直线上一点
	 * @param B 直线上另一点
	 * @return 垂足坐标
	 */
	static public PointF getFootPoint(PointF C, PointF A, PointF B) {
		PointF destPoint = null;
		if (A.x == B.x) { 
			// 垂直
			destPoint = new PointF(A.x, C.y);
		} else if (A.y == B.y) {
			// 水平
			destPoint = new PointF(C.x, A.y);
		} else {
			float k = (B.y-A.y) / (B.x-A.x);
			destPoint = getFootPoint(k, A, C);
		}
		
		return destPoint;
	}
	
	/**
	 * 求一点到直线(由A和k决定)的垂足（D）
	 * @param k 过A的直线的斜率，必须存在
	 * @param A 斜率为k的直线上的一点 
	 * @param C 直线外一点 
	 * @return 垂足坐标
	 */
	static public PointF getFootPoint(float k, PointF A, PointF C) {
		float k1 = (C.y-A.y) / (C.x-A.x); 
		if (k1 == k) {
			return C;
		}
        /*
         设直线AB方程式为：y-yA=k*（x-xA）,斜率公式:k=(yB-yA)/(xB-xA)
           直线外点C，设垂足D
            ∵两条垂直直线的斜率乘积 = -1
            ∴由AB线斜率为k可知CD直线线斜率为-1/k，可知直线CD方程式为y-yC=-1/k*（x-xC）
        联立二元方程组，解得：
            x = (k * xA+ xC / k + yC - yA) / (1 / k + k)
        再代入BC方程得：
            y=k*(x-xA)+ yA
         */
		float x = (k * A.x+ C.x / k + C.y - A.y) / (1 / k + k);
		float y = k*(x-A.x)+ A.y;
		return new PointF(x, y);
	}
}
