package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;

/**
 * shpae类型的渐变填充实现
 */
public class ShapeGradFill extends RectGradFill {
	public ShapeGradFill(Path path, Canvas canvas, Paint fillPaint, 
			RectF dstRect, RectF fillToRect, RectF tileRect,
			int[] colors, float[] positions) {
		super(path, canvas, fillPaint, dstRect, fillToRect, tileRect, colors, positions);
	}
	
	@Override
	protected void beginFill() {
		adjustParams(false);
		
		this.canvas.save();
		this.canvas.clipPath(this.path);
	}
	
	@Override
	protected void doFill() {
		if (supportFillForPath()) {
			// 基于任意形状路径的填充方法现在还有问题,限制使用范围
			fillForPath();
		} else if (getPoints() == null || isRectShape(getPoints())) {
            // 其它暂时处理成Rect填充了，而如果是Rect转化Rect渐变为了更好处理焦点框情况
            super.gradFill();

		} else {
            // 如果纯粹是n条直线拼成的闭合path，可以分解成点到n条直线的渐变
            gradFillForLinesPath(getCenter(), true);
		}
	}

    private boolean isRectShape(PointF[] points) {
        if (points.length != 4)
            return false;
        for (int i = 0; i < points.length; i++) {
            PointF p = points[i];
            if (!isSamePoint(p.x, p.y, dstRect.left, dstRect.top) &&
                    !isSamePoint(p.x, p.y, dstRect.right, dstRect.top) &&
                    !isSamePoint(p.x, p.y, dstRect.right, dstRect.bottom) &&
                    !isSamePoint(p.x, p.y, dstRect.left, dstRect.bottom)) {
                return false;
            }
        }

        return  true;
    }
	
	/**
	 * 求走过Path的所有点，并分别作渐变焦点到这所有点的渐变
	 * 理论可行，但存在两个实际问题：一、效率，除非Path很短；二、误差，区域难免有些点还没覆盖到
	 */
	private void fillForPath() {
		PathMeasure measure = new PathMeasure(path, true);
		float leng = measure.getLength();
		
		float pos[] = new float[2];
		for (int i = 0; i <= Math.ceil(leng); i++) {
			if (measure.getPosTan(i, pos, null)) {
				LinearGradient shader = new LinearGradient(fillToRect.centerX(), fillToRect.centerY(), 
						pos[0], pos[1], colors,	positions, TileMode.MIRROR);
				canvas.save();
				fillPaint.setShader(shader);
				canvas.drawLine(fillToRect.centerX(), fillToRect.centerY(), pos[0], pos[1], fillPaint);
				canvas.restore();
			}
		}
	}
	
	private boolean supportFillForPath() {
        return false;
	}
}
