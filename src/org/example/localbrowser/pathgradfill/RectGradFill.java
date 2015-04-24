package org.example.localbrowser.pathgradfill;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.EventLogTags.Description;

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
	protected void doFill() {
        fillForRect();
		
		if (haveMoreTile()) {
			tileGradFill(new ITileFillAction() {
				public void tileAction() {
					fillForRect();
				}
			});
		}
	}

    private void fillForRect() {
        PointF[] tilePoints = new PointF[4];
        tilePoints[0] = new PointF(tileRect.left, tileRect.top);
        tilePoints[1] = new PointF(tileRect.right,tileRect.top);
        tilePoints[2] = new PointF(tileRect.right, tileRect.bottom);
        tilePoints[3] = new PointF(tileRect.left, tileRect.bottom);

        PointF[] fillPoints = new PointF[4];
        fillPoints[0] = new PointF(fillToRect.left, fillToRect.top);
        fillPoints[1] = new PointF(fillToRect.right,fillToRect.top);
        fillPoints[2] = new PointF(fillToRect.right, fillToRect.bottom);
        fillPoints[3] = new PointF(fillToRect.left, fillToRect.bottom);

        if (fillToRect.width() > 0 && fillToRect.height() > 0) {
            // 纯色填充焦点框
            int oriColor = fillPaint.getColor();
            fillPaint.setColor(colors[0]);
            canvas.drawRect(fillToRect.left, fillToRect.top, fillToRect.right, fillToRect.bottom, fillPaint);
            fillPaint.setColor(oriColor);
        }

        // 上
        gradFillForParallelTwoLine(fillPoints[0], fillPoints[1], tilePoints[1], tilePoints[0]);
        // 右
        gradFillForParallelTwoLine(fillPoints[1], fillPoints[2], tilePoints[2], tilePoints[1]);
        // 下
        gradFillForParallelTwoLine(fillPoints[2], fillPoints[3], tilePoints[3], tilePoints[2]);
        // 左
        gradFillForParallelTwoLine(fillPoints[0], fillPoints[3], tilePoints[3], tilePoints[0]);
    }

    /**
     * 矩形渐变(方法是剪切区域，从一边到另一条平行边的渐变)
     * @param line1Start
     * @param line1End
     * @param line2Start
     * @param line2End
     */
    private void gradFillForParallelTwoLine(PointF line1Start, PointF line1End, PointF line2Start, PointF line2End) {
        if (line1Start.x == line1End.x && line1Start.y == line1End.y ) {
            // 是三角形，转化成三角形渐变
            gradFillForTriangle(line1Start, line2Start, line2End);
        } else {
            if (!tileRect.contains(line1Start.x, line1Start.y) &&
                    !tileRect.contains(line1End.x, line1End.y))
                return;
            PointF footPointF = getFootPoint(line1Start, line2Start, line2End);
            if (footPointF == null || isSamePoint(line1Start.x, line1Start.y, footPointF.x, footPointF.y)) {
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
}
