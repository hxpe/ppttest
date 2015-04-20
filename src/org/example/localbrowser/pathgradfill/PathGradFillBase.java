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
	
	private float[] points;
	private Matrix matrix;
	
	// 缓存加速
	float[] fileToRectPoints;
	
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
	
	public void setPoints(float[] points) {
		this.points = points;
	}
	
	public float[] getPoints() {
		return this.points;
	}
	
	public abstract void gradFill();
	
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
	 * 平铺区域顶点数组
	 * @return
	 */
	protected float[] getFillToRectPoints() {
	    	if (fileToRectPoints == null) {
	    		fileToRectPoints = createPointArray(
	    	        	fillToRect.left, fillToRect.top,
	    	        	fillToRect.right,fillToRect.top,
	    	        	fillToRect.right, fillToRect.bottom,
	    	        	fillToRect.left, fillToRect.bottom);
	    	}
	    	
	    	return fileToRectPoints;
	    }
	
	/**
	 * 一个点到n条直线组成的闭合形状的渐变
	 */
	protected void gradFillForLinesPath(float[] gradCenter, boolean closePath) {
		if (points == null || points.length < 2 || points.length % 2 == 1)
			return;
		if (gradCenter != null && gradCenter.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		float[] start = new float[2];
		float[] end = new float[2];
		start[0] = points[0];
		start[1] = points[1];
		for (int i = 2; i < points.length; i = i + 2) {
			end[0] = points[i];
			end[1] = points[i + 1];
			gradFillForTriangle(gradCenter, start, end);
			start[0] = end[0];
			start[1] = end[1];
		}
		
		if (closePath) {
			end[0] = points[0];
			end[1] = points[1];
			gradFillForTriangle(gradCenter, start, end);
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
	protected void gradFillForTriangle(float[] gradCenter, float[] lineStart, float[] lineEnd) {
		if (gradCenter != null && gradCenter.length < 2 || lineStart != null && lineStart.length < 2 ||
				lineEnd != null && lineEnd.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		float[] footPointF = getFootPoint(gradCenter, lineStart, lineEnd);
		if (footPointF == null || (gradCenter[0] == footPointF[0] &&
				gradCenter[1] == footPointF[1])) {
			// 点在直线上
			return;
		}
		// 剪切三角形区域
		Path trianglePath = new Path();
		trianglePath.moveTo(gradCenter[0], gradCenter[1]);
		trianglePath.lineTo(lineStart[0], lineStart[1]);
		trianglePath.lineTo(lineEnd[0], lineEnd[1]);
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
	protected void lineGradFill(Path clippath, float[] start, float[] end,
			float dx, float dy) {
		if (start != null && start.length < 2 || end != null && end.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		canvas.save();
		if (clippath != null)
			canvas.clipPath(clippath);
		LinearGradient lg = new LinearGradient(start[0], start[1], 
				end[0], end[1], colors, positions, TileMode.MIRROR);
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
	static public float[] getFootPoint(float[] C, float[] A, float[] B) {
		if (A != null && A.length < 2 || C != null && B.length < 2 ||
				C != null && C.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		float[] destPoint = null;
		if (A[0] == B[0]) { 
			// 垂直
			destPoint = new float[2];
			destPoint[0] = A[0];
			destPoint[1] = C[1];
		} else if (A[1] == B[1]) {
			// 水平
			destPoint = new float[2];
			destPoint[0] = C[0];
			destPoint[1] = A[1];
		} else {
			float k = (B[1]-A[1]) / (B[0]-A[0]);
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
	static public float[] getFootPoint(float k, float[] A, float[] C) {
		if (A != null && A.length < 2 || C != null && C.length < 2) {
			throw new ArrayIndexOutOfBoundsException();
		}
		 
		float k1 = (C[1]-A[1]) / (C[0]-A[0]); 
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
		float x = (k * A[0]+ C[0] / k + C[1] - A[1]) / (1 / k + k);
		float y = k*(x-A[0])+ A[1];
		float[] foot = new float[2];
		foot[0] = x;
		foot[1] = y;
		return foot;
	}
	
	static public float[] createPointArray(float... array) {
		float[] points = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			points[i] = array[i];
		}
		
		return points;
	}
}
