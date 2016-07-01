package cn.wps.graphics.shape3d.shader2D;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;
import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;

/**
 * 正面网格纹理渲染实现
 */
public class FrontFaceMesh extends Mesh2D {
	public FrontFaceMesh(ModelBase model) {
		super(model);
	}
	
	@Override
	public void init() {
		// TRIANGLE_FAN方向初始化顶点
		mCacheArrayCount = (mModel.getPathDivision().getVerts().size() + 1) * 2; 
    	
    	mVerts = new float[mCacheArrayCount];
    	mTexs = new float[mCacheArrayCount]; 
    	mColors = new int[mCacheArrayCount];
    	
    	mInited = true;
	}
	
	@Override
	public void update() {
		super.update();
		if (!mIsVisible) {
			return;
		}
		long start = System.currentTimeMillis();
		
		int vertCount = 0;
		int texsCount = 0;
        int colorCount = 0;
        mIndicesRealCount = 0;
        float faceOffset = getFaceOffset();
        
        RectF viewPort = mModel.getMatrixState().getViewPort();
		Vector3f texCenter = Vector3f.obtain().set2(viewPort.centerX(), viewPort.centerY(), 0);
		
        Vector3f faceCenter = Vector3f.obtain().set2(texCenter);
        faceCenter.subZ(faceOffset);
        mModel.getMatrixState().projectionMap(faceCenter, faceCenter);
        mVerts[vertCount++] = faceCenter.x;
    	mVerts[vertCount++] = faceCenter.y;
    	mTexs[texsCount++] = texCenter.x;
    	mTexs[texsCount++] = texCenter.y;
    	
    	int faceColor = mModel.getLight().calcLight(getFaceNormal());
    	mColors[colorCount++] = faceColor;
    	Vector3f oriV = new Vector3f();		// 纹理坐标
        Vector3f transV = new Vector3f();	// 顶点坐标
        
        ArrayList<Vector3f> verts = mModel.getPathDivision().getVerts();
        for (int i = 0; i < verts.size(); i++) {
        	oriV.set2(verts.get(i));
        	transV.set2(oriV).subZ(faceOffset);
        	mModel.getMatrixState().projectionMap(transV, transV);
        	mVerts[vertCount++] = transV.x;
        	mVerts[vertCount++] = transV.y;
        	mTexs[texsCount++] = oriV.x;
        	mTexs[texsCount++] = oriV.y;
        	mColors[colorCount++] = faceColor;
        }
        
        Log.d("Simulate3D", "update Mesh of Front face " + (System.currentTimeMillis() - start));
	}
	
	// 获取面在Z轴的偏移
	protected float getFaceOffset() {
		return 0;
	}
	
	@Override
	public void render(Canvas canvas) {
		if (mIsVisible) {
			long start = System.currentTimeMillis();
			canvas.save();
			Bitmap textureBimatp = mModel.getFrontTexture();
			Log.d("ModelBase", "getTextureBitmap " + (System.currentTimeMillis() - start));
			Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
	                Shader.TileMode.CLAMP);
			mPaint.setShader(s);
	        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, mCacheArrayCount, mVerts, 0,
	        		mTexs, 0, null, 0, mIndices, 0, mIndicesRealCount, mPaint);
	        mPaint.setShader(null);
	        canvas.restore();
	        Log.d("ModelBase", "draw front " + (System.currentTimeMillis() - start));
		}
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
	
	private static Vector3f sNormal = new Vector3f(0, 0, 1);
	protected Vector3f getFaceNormal() {
		return sNormal;
	}
}
