package org.example.localbrowser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
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

	public void onDraw(Canvas canvas) {
		//testLinearGradient(canvas);
		testRadialGradient(canvas);
		//testSweepGradient(canvas);
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
		float radio = this.getWidth() - len / 2;
		RadialGradient shader = new RadialGradient(len / 2, midY,
				radio, colors, positions, TileMode.MIRROR);
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
