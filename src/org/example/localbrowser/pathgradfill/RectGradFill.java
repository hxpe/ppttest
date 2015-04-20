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
		
        this.setPoints(createPointArray(
	        	this.tileRect.left,
	        	this.tileRect.top,
	        	this.tileRect.right,
	        	this.tileRect.top,
	        	this.tileRect.right,
	        	this.tileRect.bottom,
	        	this.tileRect.left,
	        	this.tileRect.bottom));
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
        if (fillToRect.width() > 0 && fillToRect.height() > 0) {
            // 纯色填充焦点框
            canvas.save();
            fillPaint.setColor(colors[0]);
            canvas.drawRect(fillToRect.left, fillToRect.top, fillToRect.right, fillToRect.bottom, fillPaint);
            canvas.restore();
        }

        // 上
        gradFillFromFocusLineToTileLine(0, 1, 1, 0);
        // 右
        gradFillFromFocusLineToTileLine(1, 2, 2, 1);
        // 下
        gradFillFromFocusLineToTileLine(2, 3, 3, 2);
        // 左
        gradFillFromFocusLineToTileLine(0, 3, 3, 0);
    }
    
    private void gradFillFromFocusLineToTileLine(int focusLineStartIndex, int focusLineEndIndex,
    		int tileLineStartIndex, int tileLineEndIndex) {
    	float[] fillToPoints = getFillToRectPoints();
    	float[] tilePoints = this.getPoints();
    		
    	focusLineStartIndex *= 2;
    	focusLineEndIndex *= 2;
    	tileLineStartIndex *= 2;
    	tileLineEndIndex *= 2;
    	float[] line1Start = createPointArray(fillToPoints[focusLineStartIndex],
    			fillToPoints[focusLineStartIndex + 1]);
    	float[] line1End = createPointArray(fillToPoints[focusLineEndIndex],
    			fillToPoints[focusLineEndIndex + 1]);
    	float[] line2Start= createPointArray(tilePoints[tileLineStartIndex],
    			tilePoints[tileLineStartIndex + 1]);
    	float[] line2End = createPointArray(tilePoints[tileLineEndIndex],
    			tilePoints[tileLineEndIndex + 1]);
    	
    	if (!tileRect.contains(line1Start[0], line1Start[1]))
    		return;
    	
        if (line1Start[0] == line1End[0] && line1Start[1] == line1End[1]) {
            // 是三角形，转化成三角形渐变
            gradFillForTriangle(line1Start, line2Start, line2End);
        } else {
            float[] footPointF = getFootPoint(line1Start, line2Start, line2End);
            if (footPointF == null || (line1Start[0] == footPointF[0] &&
                    line1Start[1] == footPointF[1])) {
                // 点在直线上
                return;
            }

            // 剪切矩形区域
            Path trianglePath = new Path();
            trianglePath.moveTo(line1Start[0], line1Start[1]);
            trianglePath.lineTo(line1End[0], line1End[1]);
            trianglePath.lineTo(line2Start[0], line2Start[1]);
            trianglePath.lineTo(line2End[0], line2End[1]);
            trianglePath.close();

            lineGradFill(trianglePath, line1Start, footPointF, 0, 0);
        }

    }
}
