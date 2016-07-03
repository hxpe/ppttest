package cn.wps.graphics.shape3d.shader2D;

import cn.wps.graphics.shape3d.Matrix3D;
import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;
import cn.wps.graphics.shape3d.Camera.Camera3D;
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
    private Paint mTextPaint = new Paint();
	
	private Camera3D mCamera3d = new Camera3D();
	private Matrix mMatrix = new Matrix();
	
	private boolean mIsBackFace = false;
	
	public MainFaceRender(ModelBase model, boolean isBackFace) {
		super(model);
		mIsBackFace = isBackFace;
	}
	
	@Override
	public void init() {
		if (!mIsBackFace) {
			mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
			mTextPaint.setTextSize(100);
			mTextPaint.setColor(0xffff0000);
		}
		
		mInited = true;
	}
	
	@Override
	public void dispose() {
		mInited = false;
	}
	
	@Override
	public void render(Canvas canvas) {
		if (!mIsVisible) {
			return;
		}
		long start = System.currentTimeMillis();
		canvas.save();
		canvas.concat(mMatrix);
		doBaseRender(canvas);
		canvas.restore();
		Log.d("MainFaceRender", "draw with camera " + (System.currentTimeMillis() - start));
	}
	
	private void doBaseRender(Canvas canvas) {
		// 画正反面纹理
		Bitmap textureBimatp = mIsBackFace ? mModel.getBackTexture() : mModel.getFrontTexture();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		mPaint.setShader(s);
		canvas.drawPath(mModel.getShapePath(), mPaint);
		mPaint.setShader(null);
		
		// 正画些文本
		if (!mIsBackFace) {
			RectF viewPort = mModel.getMatrixState().getViewPort();
			canvas.drawText("正面文本3D投影", 
					viewPort.left, viewPort.centerY() - 55, mTextPaint);
			canvas.drawText("测试abcdefgh12345678", 
					viewPort.left, viewPort.centerY() + 55, mTextPaint);
		}
	}
	
	@Override
	public void update() {
		super.update();
		if (!mIsVisible) {
			return;
		}
		
		updateCameraMatrix2();
	}
	
	@Override
	protected void updateVisible() {
		RectF viewPort = mModel.getMatrixState().getViewPort();
		Vector3f topLeft = Vector3f.obtain().set2(viewPort.left, viewPort.top, 0);
		Vector3f topBottom = Vector3f.obtain().set2(viewPort.left, viewPort.bottom, 0);
		Vector3f rightTop = Vector3f.obtain().set2(viewPort.right, viewPort.top, 0);
		
		if (mIsBackFace) {
			float offset = getFaceOffset();
			topLeft.subZ(offset);
			topBottom.subZ(offset);
			rightTop.subZ(offset);
		}
		
		mModel.getMatrixState().projectionMap(topLeft, topLeft);
		mModel.getMatrixState().projectionMap(topBottom, topBottom);
		mModel.getMatrixState().projectionMap(rightTop, rightTop);
		mIsVisible = isTriangleFront(topBottom, topLeft, rightTop);
		if (mIsBackFace) {
			mIsVisible = !mIsVisible;
		}
	}
	
	protected float getFaceOffset() {
		return mIsBackFace ? mModel.getObject3d().height : 0;
	}
	
	private void updateCameraMatrix2() {
		mMatrix.reset();
		mCamera3d.save();
		mCamera3d.setLocation(0, 0, -mModel.getMatrixState().getEyez() / 72);
		Matrix3D t = mCamera3d.getTransfromMatrix();
		t.setMatrix(mModel.getMatrixState().cameraTransfrom());
		if (mIsBackFace) {
			t.translate3d(0, 0, mModel.getObject3d().height);
		}
		mCamera3d.getMatrix(mMatrix);
		RectF viewPort = mModel.getMatrixState().getViewPort();
		mMatrix.preTranslate(-viewPort.width() / 2, -viewPort.height() / 2);
		mMatrix.postTranslate(viewPort.height() / 2, viewPort.height() / 2);
		mCamera3d.restore();
	}
}
