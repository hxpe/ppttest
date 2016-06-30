package cn.wps.graphics.shape3d.shader2D;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;

/**
 * 背面网格纹理渲染实现
 */
public class BackFaceMesh extends FrontFaceMesh {
	private static Vector3f sNormal = new Vector3f(0, 0, -1);
	
	public BackFaceMesh(ModelBase model) {
		super(model);
	}
	
	@Override
	protected Vector3f getFaceNormal() {
		return sNormal;
	}
	
	@Override
	protected float getFaceOffset() {
		return mModel.getObject3d().height;
	}
	
	@Override
	protected void updateVisible() {
		RectF viewPort = mModel.getMatrixState().getViewPort();
		float offset = getFaceOffset();
		Vector3f topLeft = Vector3f.obtain().set2(viewPort.left, viewPort.top, 0);
		topLeft.subZ(offset);
		Vector3f topBottom = Vector3f.obtain().set2(viewPort.left, viewPort.bottom, 0);
		topBottom.subZ(offset);
		Vector3f rightTop = Vector3f.obtain().set2(viewPort.right, viewPort.top, 0);
		rightTop.subZ(offset);
		
		mModel.getMatrixState().projectionMap(topLeft, topLeft);
		mModel.getMatrixState().projectionMap(topBottom, topBottom);
		mModel.getMatrixState().projectionMap(rightTop, rightTop);
		mIsVisible = !isTriangleFront(topBottom, topLeft, rightTop);
		
		topLeft.recycle();
		topBottom.recycle();
		rightTop.recycle();
	}
	
	@Override
	public void draw(Canvas canvas) {
		if (mIsVisible) {
			long start = System.currentTimeMillis();
			canvas.save();
			Bitmap textureBimatp = mModel.getBackTexture();
			Log.d("ModelBase", "getTextureBitmap " + (System.currentTimeMillis() - start));
			Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
	                Shader.TileMode.CLAMP);
			mPaint.setShader(s);
			Log.d("ModelBase", "BitmapShader " + (System.currentTimeMillis() - start));
	        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, mCacheArrayCount, mVerts, 0,
	        		mTexs, 0, null, 0, mIndices, 0, mIndicesRealCount, mPaint);
	        mPaint.setShader(null);
	        canvas.restore();
	        Log.d("ModelBase", "draw back " + (System.currentTimeMillis() - start));
		}
	}
}
