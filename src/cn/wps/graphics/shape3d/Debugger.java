package cn.wps.graphics.shape3d;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Style;

public class Debugger {
	public static boolean sDebug = true;
	
	private ModelBase mModel;
	
	public Debugger(ModelBase model) {
		this.mModel = model;
	}
	
	private Paint testPaint = new Paint();
	public void drawPoints(Canvas canvas) {
		if (!sDebug) {
			return;
		}
		Vector3f temp3f = new Vector3f();
		testPaint.setStrokeWidth(5);
		for (int i = 0, size = mModel.mListVerts.size(); i < size; i++) {
			if (i == size - 1) {
				testPaint.setColor(0xffff0000);
			} else {
				testPaint.setColor(0xff00ff00);
			}
			Vector3f v = mModel.mListVerts.get(i);
//			temp3f.set2(mListNormals.get(i)).scale(100);
//			canvas.drawLine(v.x, v.y, v.x + temp3f.x, v.y + temp3f.y, testPaint);
			canvas.drawPoint(v.x, v.y, testPaint);
		}
	}
	
	public void drawFrame(Canvas canvas) {
		if (!sDebug) {
			return;
		}
		testPaint.setStyle(Style.STROKE);
		testPaint.setStrokeWidth(2);
		testPaint.setColor(0xFF0000ff);
		RectF viewPort = mModel.mMatrixState.getViewPort();
		canvas.drawRect(viewPort, testPaint);
		canvas.drawLine(viewPort.centerX(), viewPort.top, 
				viewPort.centerX(), viewPort.bottom, testPaint);
		canvas.drawLine(viewPort.left, viewPort.centerY(), 
				viewPort.right, viewPort.centerY(), testPaint);
	}
	
	public void drawMesh(Canvas canvas, Shader2DImpl.Mesh2D mesh) {
		if (!sDebug) {
			return;
		}
		testPaint.setStrokeWidth(1);
		testPaint.setColor(0xff00ff00);
		float[] verts = mesh.mVerts;
		for (int i = 0, size = verts.length / 4; i < size; i++) {
			canvas.drawLine(verts[i * 4], verts[i * 4 + 1], 
					verts[i * 4 + 2], verts[i * 4 + 3], testPaint);
			if (i > 0) {
				canvas.drawLine(verts[i * 4 - 2], verts[i * 4 - 1], 
						verts[i * 4], verts[i * 4 + 1], testPaint);
			}
		}
	}
	
	public void drawEdge(Canvas canvas, Shader2DImpl.Mesh2D mesh) {
		if (!sDebug) {
			return;
		}
		testPaint.setStrokeWidth(1);
		testPaint.setColor(0xff0000ff);
		float[] verts = mesh.mVerts;
		for (int i = 0, size = verts.length / 4 - 1; i < size; i++) {
			canvas.drawLine(verts[(i) * 4], verts[i * 4 + 1], 
					verts[(i + 1) * 4], verts[(i + 1) * 4 + 1], testPaint);
		}
		testPaint.setColor(0xff00ff00);
		for (int i = 0, size = verts.length / 4 - 1; i < size; i++) {
			canvas.drawLine(verts[(i) * 4 + 2], verts[i * 4 + 3], 
					verts[(i + 1) * 4 + 2], verts[(i + 1) * 4 + 3], testPaint);
		}
	}
	
	public void drawFrontFace(Canvas canvas) {
		canvas.save();
		Bitmap textureBimatp = mModel.getTextureBitmap();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		testPaint.setShader(s);
		canvas.drawPath(mModel.getShapePath(), testPaint);
		testPaint.setShader(null);
		canvas.restore();
	}
	
	private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();
	private Matrix getCameraMatrix() {
		mMatrix.reset();
		mCamera.save();
		mCamera.rotateY(-45);
		mCamera.getMatrix(mMatrix);
		RectF viewPort = mModel.mMatrixState.getViewPort();
		mMatrix.preTranslate(-viewPort.width() / 2, -viewPort.height() / 2);
		mMatrix.postTranslate(viewPort.height() / 2, viewPort.height() / 2);
		mCamera.restore();
		return mMatrix;
	}
}
