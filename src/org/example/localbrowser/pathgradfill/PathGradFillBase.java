package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Path.Direction;
import android.graphics.Shader.TileMode;

/**
 * gradFill指定path(rect,circle,shape)的渐变填充的实现基类
 */
public abstract class PathGradFillBase {
	protected Path path;
	protected Path adjustPath;
	protected Canvas canvas;
	protected Paint fillPaint;
	protected RectF dstRect;
	protected RectF fillToRect;
	protected RectF tileRect;
	protected int[] colors;
	protected float[] positions;
	
	private RectF oriDstRect;
	private RectF oriFillToRect;
	private RectF oriTileRect;
	
	private PointF[] points;
	protected Matrix matrix;
	private PointF tempCenterF;
	
	protected float rotation = 0f;
	protected boolean rotWithShape = true; 
	
	static public interface ITileFillAction {
		void tileAction();
	}
	
	public PathGradFillBase(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		this.path = path;
		this.canvas = canvas;
		this.fillPaint = fillPaint;
		this.colors = colors;
		this.positions = positions;
		
		this.oriDstRect = dstRect;
		this.oriFillToRect = fillToRect;
		this.oriTileRect = tileRect;
	}
	
	public void gradFill() {
		beginFill();
		try {
			doFill();
		} finally {
			afterFill();
		}
	}
	
	/**
	 * 主要填充实现方法
	 */
	protected abstract void doFill();
	
	protected void beginFill() {
		adjustParams(true);
		
		this.canvas.save();
		this.canvas.clipPath(this.path);
		if (!rotWithShape && rotation != 0) {
			this.canvas.concat(getRotationMatric(dstRect.centerX(), dstRect.centerY(), -rotation));
		}
	}
	
	protected void afterFill() {
		this.canvas.restore();
	}
	
	public void setPoints(PointF[] points) {
		this.points = points;
	}
	
	public PointF[] getPoints() {
		return this.points;
	}
	
	public void setRotParam(boolean rotWithShape, float rotation) {
		this.rotWithShape = rotWithShape;
		this.rotation = rotation;
	}
	
	protected void adjustParams(boolean adustDstRectForRotation) {
		if (oriFillToRect == null)
			oriFillToRect = new RectF(0, 0, 1.0f, 1.0f); // 默认左上角
		if (oriTileRect == null)
			oriTileRect = new RectF(); // 默认占满dstRect
		
		// 对象本身已经附加了一个旋转矩阵,即默认已经跟随旋转
		// 这里只处理rotWithShape等于false的情况，反向旋转回去
		if (adustDstRectForRotation && !rotWithShape && rotation != 0) {
			dstRect = getRotationRect(oriDstRect, rotation);
			adjustPath = new Path();
			adjustPath.addRect(dstRect, Direction.CW);
		} else {
			this.dstRect = oriDstRect;
			adjustPath = path;
		}

		fillToRect = transPercentageRect(oriFillToRect, dstRect);
		tileRect = transPercentageRect(oriTileRect, dstRect);
	}
	
	/**
	 * 渐变的中心点
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
	 * 一个点到n条直线组成的闭合形状的渐变
	 * @param gradCenter 渐变起点
	 * @param closePath 路径是否闭合
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
	 * @param action 填充动作
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
	private void rotateToTile(ITileFillAction action, float px, float py, float sx, float sy) {
		if (action == null)
			return;
		
		canvas.save();
		canvas.concat(getScaleMatric(px, py, sx, sy));
		action.tileAction();
		canvas.restore();
	}
	
	/**
	 * 获取缩放矩阵
	 * @param px
	 * @param py
	 * @param sx
	 * @param sy
	 * @return
	 */
	protected Matrix getScaleMatric(float px, float py, float sx, float sy) {
		if (matrix == null)
			matrix = new Matrix();
		else 
			matrix.reset();
		matrix.preTranslate(px, py);
		matrix.preScale(sx, sy);
		matrix.preTranslate(-px, -py);
		return matrix;
	}
	
	/**
	 * 获取旋转矩阵
	 * @param px
	 * @param py
	 * @param degrees
	 * @return
	 */
	protected Matrix getRotationMatric(float px, float py, float degrees) {
		if (matrix == null)
			matrix = new Matrix();
		else 
			matrix.reset();
		matrix.preTranslate(px, py);
		matrix.preRotate(degrees);
		matrix.preTranslate(-px, -py);
		return matrix;
	}
	
	/**
	 * 一个点到一条边的渐变，这里定义为点到垂足的渐变，triangle名称义指clipPath区域形状
	 * @param gradCenter 渐变起点
	 * @param lineStart 直线上一点
	 * @param lineEnd 直线上另一点
	 */
	protected void gradFillForTriangle(PointF gradCenter, PointF lineStart, PointF lineEnd) {
		PointF footPointF = getFootPoint(gradCenter, lineStart, lineEnd);
		if (footPointF == null  || isSamePoint(gradCenter.x, gradCenter.y, footPointF.x, footPointF.y)) {
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
		
		LinearGradient lg = new LinearGradient(start.x, start.y, 
				end.x, end.y, colors, positions, TileMode.MIRROR);
		if (dx != 0 || dy != 0) {
			Matrix matrix = new Matrix();
			matrix.preTranslate(dx, dy);
			lg.setLocalMatrix(matrix);
		}
		fillPaint.setShader(lg);
		if (clippath != null) {
			canvas.clipPath(clippath);
		} 
		canvas.drawPath(adjustPath, fillPaint);
		fillPaint.setShader(null);
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

    /**
     * 在误差范围内，判断是否同一点
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    static public boolean isSamePoint(float x1, float y1, float x2, float y2) {
        float precision = 0.0001f;
        return ((Math.abs(x1 - x2) < precision) && (Math.abs(y1 - y2) < precision));
    }
    
    /**
     * 计算旋转后的Rect
     * @param sourceRect
     * @param rotation
     * @return
     */
    static public RectF getRotationRect(RectF sourceRect, float rotation)
	 {
		 float centerX = sourceRect.centerX();
		 float centerY = sourceRect.centerY();
		 
		 float cosValue = Math.abs((float)Math.cos(rotation * Math.PI / 180));
		 float sinValue = Math.abs((float)Math.sin(rotation * Math.PI / 180));
		 float height = sourceRect.height() * cosValue + sourceRect.width() * sinValue;
		 float width = sourceRect.width() * cosValue  + sourceRect.height() * sinValue;
		 
		 return new RectF(centerX - width / 2,
		                  centerY - height / 2,
		                  centerX + width / 2,
		                  centerY + height /2);
	 }
}
