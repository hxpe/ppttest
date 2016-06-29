package cn.wps.graphics.shape3d;

import java.util.ArrayList;

import cn.wps.graphics.shape3d.shader2D.Mesh2D;
import cn.wps.graphics.shape3d.shader2D.ShaderSoftImpl;
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
	
	public void drawTest(Canvas canvas) {
		if (!sDebug) {
			return;
		}
		
//		if (mModel.getShader() instanceof ShaderSoftImpl) {
//			ShaderSoftImpl shader2d = (ShaderSoftImpl)mModel.getShader();
//			drawSlideEdge(canvas, shader2d.getSlideFace(), false);
//			if (shader2d.getFrontFace().isVisible())
//				drawMainFaceMesh(canvas, shader2d.getFrontFace());
//		}
		
		drawFrame(canvas);
	}
	
	// 画出原始顶点或法线方向
	public void drawOriVerts(Canvas canvas, boolean drawNormal) {
		if (!sDebug) {
			return;
		}
		Vector3f temp3f = new Vector3f();
		testPaint.setStrokeWidth(5);
		ArrayList<Vector3f> verts = mModel.getPathDivision().getVerts();
		ArrayList<Vector3f> normals = mModel.getPathDivision().getNormals();
		for (int i = 0, size = verts.size(); i < size; i++) {
			if (i == size - 1) {
				testPaint.setColor(0xffff0000);
			} else {
				testPaint.setColor(0xff00ff00);
			}
			Vector3f v = verts.get(i);
			if (drawNormal) {
				temp3f.set2(normals.get(i)).scale(100);
				canvas.drawLine(v.x, v.y, v.x + temp3f.x, v.y + temp3f.y, testPaint);	
			} else {
				canvas.drawPoint(v.x, v.y, testPaint);
			}
		}
	}
	
	// 画对象控制框轮廓
	public void drawFrame(Canvas canvas) {
		if (!sDebug) {
			return;
		}
		testPaint.setStyle(Style.STROKE);
		testPaint.setStrokeWidth(2);
		testPaint.setColor(0xFF0000ff);
		RectF viewPort = mModel.getMatrixState().getViewPort();
		canvas.drawRect(viewPort, testPaint);
		canvas.drawLine(viewPort.centerX(), viewPort.top, 
				viewPort.centerX(), viewPort.bottom, testPaint);
		canvas.drawLine(viewPort.left, viewPort.centerY(), 
				viewPort.right, viewPort.centerY(), testPaint);
	}
	
	// 画侧面网格
	public void drawSlideMesh(Canvas canvas, Mesh2D mesh) {
		if (!sDebug) {
			return;
		}
		float[] verts = mesh.getVerts();
		if (verts == null) {
			return;
		}
		testPaint.setStrokeWidth(1);
		testPaint.setColor(0xff00ff00);
		for (int i = 0, size = verts.length / 4; i < size; i++) {
			canvas.drawLine(verts[i * 4], verts[i * 4 + 1], 
					verts[i * 4 + 2], verts[i * 4 + 3], testPaint);
			if (i > 0) {
				canvas.drawLine(verts[i * 4 - 2], verts[i * 4 - 1], 
						verts[i * 4], verts[i * 4 + 1], testPaint);
			}
		}
	}
	
	// 侧面轮廓
	public void drawSlideEdge(Canvas canvas, Mesh2D mesh, boolean drawBackEdge) {
		if (!sDebug) {
			return;
		}
		float[] verts = mesh.getVerts();
		if (verts == null) {
			return;
		}
		testPaint.setStrokeWidth(1);
		testPaint.setColor(0xff0000ff);
		for (int i = 0, size = verts.length / 4 - 1; i < size; i++) {
			canvas.drawLine(verts[i * 4], verts[i * 4 + 1], 
					verts[(i + 1) * 4], verts[(i + 1) * 4 + 1], testPaint);
		}
		if (drawBackEdge) {
			testPaint.setColor(0xff00ff00);
			for (int i = 0, size = verts.length / 4 - 1; i < size; i++) {
				canvas.drawLine(verts[i * 4 + 2], verts[i * 4 + 3], 
						verts[(i + 1) * 4 + 2], verts[(i + 1) * 4 + 3], testPaint);
			}
		}
	}
	
	// 画主面网格
	public void drawMainFaceMesh(Canvas canvas, Mesh2D mesh) {
		if (!sDebug) {
			return;
		}
		float[] verts = mesh.getVerts();
		if (verts == null || verts.length <= 0) {
			return;
		}
		testPaint.setStrokeWidth(1);
		testPaint.setColor(0xffff0000);
		
		float centerX = verts[0];
		float centerY = verts[1];
		for (int i = 1, size = verts.length / 2; i < size; i++) {
			canvas.drawLine(centerX, centerY, 
					verts[i * 2], verts[i * 2 + 1], testPaint);
		}
	}
	
	public void drawFrontFace(Canvas canvas) {
		canvas.save();
		Bitmap textureBimatp = mModel.getFrontTexture();
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
		RectF viewPort = mModel.getMatrixState().getViewPort();
		mMatrix.preTranslate(-viewPort.width() / 2, -viewPort.height() / 2);
		mMatrix.postTranslate(viewPort.height() / 2, viewPort.height() / 2);
		mCamera.restore();
		return mMatrix;
	}
}
