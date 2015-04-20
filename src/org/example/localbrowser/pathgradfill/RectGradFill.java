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

		this.canvas.save();
		this.canvas.clipPath(this.path);
        fillForRect();
		
		if (haveMoreTile()) {
			tileGradFill(new ITileFillAction() {
				public void tileAction() {
                fillForRect();
					
				}
			});
		}
		this.canvas.restore();
	}

    private void fillForRect() {
        PointF[] tilePoints = new PointF[4];
        tilePoints[0] = new PointF(tileRect.left, tileRect.top);
        tilePoints[1] = new PointF(tileRect.right,tileRect.top);
        tilePoints[2] = new PointF(tileRect.right, tileRect.bottom);
        tilePoints[3] = new PointF(tileRect.left, tileRect.bottom);

        PointF[] filePoints = new PointF[4];
        filePoints[0] = new PointF(fillToRect.left, fillToRect.top);
        filePoints[1] = new PointF(fillToRect.right,fillToRect.top);
        filePoints[2] = new PointF(fillToRect.right, fillToRect.bottom);
        filePoints[3] = new PointF(fillToRect.left, fillToRect.bottom);

        if (fillToRect.width() > 0 && fillToRect.height() > 0) {
            // 纯色填充焦点框
            canvas.save();
            fillPaint.setColor(colors[0]);
            canvas.drawRect(fillToRect.left, fillToRect.top, fillToRect.right, fillToRect.bottom, fillPaint);
            canvas.restore();
        }

        // 上
        gradFillForParallelTwoLine(filePoints[0], filePoints[1], tilePoints[1], tilePoints[0]);
        // 右
        gradFillForParallelTwoLine(filePoints[1], filePoints[2], tilePoints[2], tilePoints[1]);
        // 下
        gradFillForParallelTwoLine(filePoints[2], filePoints[3], tilePoints[3], tilePoints[2]);
        // 左
        gradFillForParallelTwoLine(filePoints[0], filePoints[3], tilePoints[3], tilePoints[0]);
    }

    private void gradFillForParallelTwoLine(PointF line1Start, PointF line1End, PointF line2Start, PointF line2End) {
        if (line1Start.x == line1End.x && line1Start.y == line1End.y ) {
            // 是三角形，转化成三角形渐变
            gradFillForTriangle(line1Start, line2Start, line2End);
        } else {
            PointF footPointF = getFootPoint(line1Start, line2Start, line2End);
            if (footPointF == null && line1Start.x == footPointF.x &&
                    line1Start.y == footPointF.y) {
                // 点在直线上
                return;
            }

            // 剪切矩形区域
            Path trianglePath = new Path();
            trianglePath.moveTo(line1Start.x, line1Start.y);
            trianglePath.lineTo(line1End.x, line1End.y);
            trianglePath.lineTo(line2Start.x, line2Start.y);
            trianglePath.lineTo(line2End.x, line2End.y);
            trianglePath.close();


            lineGradFill(trianglePath, line1Start, footPointF, 0, 0);
        }

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
