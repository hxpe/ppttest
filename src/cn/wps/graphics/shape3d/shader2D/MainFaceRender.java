package cn.wps.graphics.shape3d.shader2D;

import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

/**
 * 主面附加投影矩阵的渲染
 */
public class MainFaceRender extends Shader2DBase {
	private Camera mCamera = new Camera();
    private Matrix mMatrix = new Matrix();
	protected Paint mPaint = new Paint();
	
	public MainFaceRender(ModelBase model) {
		super(model);
	}
	
	@Override
	public void init() {
		mInited = true;
	}
	
	@Override
	public void dispose() {
		mInited = false;
	}
	
	@Override
	public void render(Canvas canvas) {
		long start = System.currentTimeMillis();
		canvas.save();
		canvas.concat(mMatrix);
		doBaseRender(canvas);
		canvas.restore();
		Log.d("MainFaceRender", "draw with camera " + (System.currentTimeMillis() - start));
	}
	
	private void doBaseRender(Canvas canvas) {
		Bitmap textureBimatp = mModel.getFrontTexture();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		mPaint.setShader(s);
		canvas.drawPath(mModel.getShapePath(), mPaint);
		mPaint.setShader(null);
	}
	
	@Override
	public void update() {
		super.update();
		if (!mIsVisible) {
			return;
		}
		
		updateCameraMatrix();
	}
	
	@Override
	protected void updateVisible() {
		RectF viewPort = mModel.getMatrixState().getViewPort();
		Vector3f topLeft = Vector3f.obtain().set2(viewPort.left, viewPort.top, 0);
		Vector3f topBottom = Vector3f.obtain().set2(viewPort.left, viewPort.bottom, 0);
		Vector3f rightTop = Vector3f.obtain().set2(viewPort.right, viewPort.top, 0);
		
		mModel.getMatrixState().projectionMap(topLeft, topLeft);
		mModel.getMatrixState().projectionMap(topBottom, topBottom);
		mModel.getMatrixState().projectionMap(rightTop, rightTop);
		mIsVisible = isTriangleFront(topBottom, topLeft, rightTop);
		
		topLeft.recycle();
		topBottom.recycle();
		rightTop.recycle();
	}
	
	private void updateCameraMatrix() {
		mMatrix.reset();
		mCamera.save();
		mCamera.setLocation(0, 0, -mModel.getMatrixState().getEyez() / 72);
		mCamera.rotateX(45);
		mCamera.getMatrix(mMatrix);
		RectF viewPort = mModel.getMatrixState().getViewPort();
		mMatrix.preTranslate(-viewPort.width() / 2, -viewPort.height() / 2);
		mMatrix.postTranslate(viewPort.height() / 2, viewPort.height() / 2);
		mCamera.restore();
	}
}
