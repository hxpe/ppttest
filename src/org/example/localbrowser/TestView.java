package org.example.localbrowser;

import java.io.File;

import org.example.localbrowser.pathgradfill.CircleGradFill;
import org.example.localbrowser.pathgradfill.LinGradFill;
import org.example.localbrowser.pathgradfill.PathGradFillBase;
import org.example.localbrowser.pathgradfill.RectGradFill;
import org.example.localbrowser.pathgradfill.ShapeGradFill;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Path.Direction;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TestView extends View {
	static public interface ILog {
		public void log(String msg);
	}

	private ILog mLog;
	public void setLog(ILog log) {
		this.mLog = log;
	}
	
	private void log(String msg) {
		if (mLog != null) {
			mLog.log(msg);
		}
	}
	
	public TestView(Context context) {
		super(context);
	}

	public TestView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	private void testRecolor(Canvas canvas) {
		File file = new File("/storage/sdcard1/hxpe/test1/test.png");
		if (file.exists()) {
			Bitmap temp = BitmapFactory.decodeFile("/storage/sdcard1/hxpe/test1/test.png");
			if (temp != null) {
				Paint p = new Paint();
				p.setColor(Color.RED);
				canvas.drawBitmap(temp, 0, 0, p);
			}
		}
	}

	public void onDraw(Canvas canvas) {
		long startCount = System.currentTimeMillis();
		testCircleGradFill(canvas);
		long endCount = System.currentTimeMillis();
		long des = endCount - startCount;
		Log.d("onDraw", "abcdefg" + des);
	}
	
	private void testRectRotation(Canvas canvas) {
		final float rot = 130;
		RectF testRctF = new RectF(100, 100, 400, 300);
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		canvas.drawRect(testRctF, p);
		p.setColor(Color.GREEN);
		p.setAlpha(50);
		canvas.save();
		canvas.rotate(rot, testRctF.centerX(), testRctF.centerY());
		canvas.drawRect(testRctF, p);
		canvas.restore();
		p.setColor(Color.RED);
		p.setAlpha(50);
		canvas.drawRect(PathGradFillBase.getRotationRect(testRctF, rot), p);
	}
	
	private void testLinGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(100, 50, this.getHeight() + 100, this.getHeight()-100);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = null;
		RectF tileRect = new RectF(1f/3, 1f/3, 1f/3, 1f/3);
		
		LinGradFill gradFill = new LinGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setLinParam(false, 45);
		gradFill.setRotParam(false, 30);
		
		canvas.save();
		Matrix matrix = new Matrix();
		matrix.setRotate(30, dstRect.centerX(), dstRect.centerY());
		canvas.concat(matrix);
		gradFill.gradFill();
		canvas.restore();
	}
	
	private void testShapeGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(0, 0, this.getHeight(), this.getHeight());
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		ShapeGradFill gradFill = new ShapeGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		
		PointF[] points = getStar5Poins(dstRect);
		gradFill.setPoints(points);
		gradFill.gradFill();
	}
	
	private PointF createCenter(RectF dstRect, float xOffset, float yOffset){
		PointF centerF = new PointF(dstRect.centerX(), dstRect.centerY());
		centerF.offset(xOffset, yOffset);
		return centerF;
	}
	
	private PointF[] getStar5Poins(RectF dstRect) {
		float r54 = (float)Math.PI * 54/180;
		float r18 = (float)Math.PI * 18/180;
		PointF[] vertexes = new PointF[10];
		float radiusOut = dstRect.width() / 2;
		float radiusIn = 0.3819660112501f * radiusOut;
		vertexes[0] = createCenter(dstRect, 0, -radiusOut);
		vertexes[1] = createCenter(dstRect, (float)(Math.cos(r54) * radiusIn), (float)(-Math.sin(r54) * radiusIn));
		vertexes[2] = createCenter(dstRect, (float)(Math.cos(r18) * radiusOut), (float)(-Math.sin(r18) * radiusOut));
		vertexes[3] = createCenter(dstRect, (float)(Math.cos(r18) * radiusIn), (float)(Math.sin(r18) * radiusIn));
		vertexes[4] = createCenter(dstRect, (float)(Math.cos(r54) * radiusOut), (float)(Math.sin(r54) * radiusOut));
		vertexes[5] = createCenter(dstRect, 0, radiusIn);
		vertexes[6] = createCenter(dstRect, (float)(-Math.cos(r54) * radiusOut), (float)(Math.sin(r54) * radiusOut));
		vertexes[7] = createCenter(dstRect, (float)(-Math.cos(r18) * radiusIn), (float)(Math.sin(r18) * radiusIn));
		vertexes[8] = createCenter(dstRect, (float)(-Math.cos(r18) * radiusOut), (float)(-Math.sin(r18) * radiusOut));
		vertexes[9] = createCenter(dstRect, (float)(-Math.cos(r54) * radiusIn), (float)(-Math.sin(r54) * radiusIn));
		
		return vertexes;
	}
	
	private void testRectGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(100, 100, this.getHeight() + 100, this.getHeight() - 100);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0f, 0f, 1f, 1f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		Matrix matrix = new Matrix();
		matrix.preTranslate(dstRect.centerX(), dstRect.centerY());
		matrix.preRotate(30);
		matrix.preTranslate(-dstRect.centerX(), -dstRect.centerY());
		canvas.concat(matrix);
		
		RectGradFill gradFill = new RectGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setRotParam(false, 30);
		gradFill.gradFill();
	}
	
	private void testCircleGradFill(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		RectF dstRect = new RectF(100, 100, this.getHeight() + 100, this.getHeight() - 100);
		Path path = new Path();
		path.addRect(dstRect, Direction.CW);
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(1f/3, 1f/3, 1f/3, 1f/3);
		
		Matrix matrix = new Matrix();
		matrix.preTranslate(dstRect.centerX(), dstRect.centerY());
		matrix.preRotate(30);
		matrix.preTranslate(-dstRect.centerX(), -dstRect.centerY());
		canvas.concat(matrix);
		
		CircleGradFill gradFill = new CircleGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.setRotParam(false, 30);
		gradFill.gradFill();
	}
	
	private void testDrawLineLinearGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		
		Path path = new Path();
		RectF dstRect = new RectF(0, 0, this.getHeight(), this.getHeight());
		PointF[] points = getStar5Poins(dstRect);
		path.moveTo(points[0].x, points[0].y);
		for (int i = 1; i < points.length; i++)
			path.lineTo(points[i].x, points[i].y);
		path.close();
		
		RectF fillToRect = new RectF(0.5f, 0.5f, 0.5f, 0.5f);
		RectF tileRect = new RectF(0, 0, 0, 0);
		
		ShapeGradFill gradFill = new ShapeGradFill(path, canvas, p, dstRect, fillToRect, tileRect, colors, positions);
		gradFill.gradFill();
	}
	
	private void testLinearGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		LinearGradient shader = new LinearGradient(0, midY, len, midY, colors,
				positions, TileMode.MIRROR);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);

		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
	
	private void testRadialGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		float radius = this.getWidth() - len / 2;
		RadialGradient shader = new RadialGradient(len / 2, midY,
				radius, colors, positions, TileMode.MIRROR);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);

		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
	
	private void testSweepGradient(Canvas canvas) {
		Paint p = new Paint();

		int[] colors = new int[3];
		float[] positions = new float[3];

		colors[0] = Color.BLUE;
		colors[1] = Color.YELLOW;
		colors[2] = Color.GREEN;
		positions[0] = 0f;
		positions[1] = 0.5f;
		positions[2] = 1f;
		float midY = this.getHeight() / 2;
		float len = this.getWidth() / 2;
		SweepGradient shader = new SweepGradient(len / 2, midY, colors,
				positions);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, len / 2, midY);
		shader.setLocalMatrix(matrix);
		p.setShader(shader);
		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
	}
}
