package cn.wps.graphics.shape3d;

import javax.security.auth.PrivateCredentialPermission;

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
 * 2D绘图函数模拟实现3D效果
 */
public class Shader2DImpl {
	public abstract class Mesh2D {
		public int mCacheArrayCount = 0; // 缓存总float数
		public int mIndicesRealCount = 0; // 缓存可能还有冗余空间，这个标记真实数目
		public float[] mVerts;
		public float[] mTexs;
		public int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
		public short[] mIndices;
		
		public abstract void init();
		public abstract void updateMesh();
		
		public void draw(Canvas canvas) {
			canvas.save();
	        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mCacheArrayCount, mVerts, 0,
	        		mTexs, 0, mColors, 0, mIndices, 0, mIndicesRealCount, mPaint);
	        canvas.restore();
		}
	}
	
	private ModelBase mModelBase;
	private Mesh2D mProfileMesh = new Mesh2D() {
		@Override
		public void init() {
			// 缓存区按最大算，可能实际有冗余
			mCacheArrayCount = mModelBase.mListVerts.size() * 2 * 2; 
	    	
	    	mVerts = new float[mCacheArrayCount];
	    	mColors = new int[mCacheArrayCount]; 
	    	mIndices = new short[(mModelBase.mListVerts.size() - 1) * 2 * 3]; // VertexMode.TRIANGLES
	    	
	    	updateMesh();
		}
		
		@Override
		public void draw(Canvas canvas) {
			super.draw(canvas);
			mModelBase.mDebugger.drawEdge(canvas, this);
		}
		
		@Override
		public void updateMesh() {
			long start = System.currentTimeMillis();
			
			int vertCount = 0;
	        int colorCount = 0;
	        mIndicesRealCount = 0;
	        Vector3f lastFront = new Vector3f();
	        Vector3f lastBottom = new Vector3f();
	        Vector3f front = new Vector3f();
	        Vector3f bottom = new Vector3f();
	        for (int i = 0; i < mModelBase.mListVerts.size(); i++) {
	        	front.set2(mModelBase.mListVerts.get(i));
	        	bottom.set2(front).subZ(mModelBase.mObject3d.height);
	        	
	        	mModelBase.mMatrixState.projectionMap(front, front);
	        	mVerts[vertCount++] = front.x;
	        	mVerts[vertCount++] = front.y;
	        	
	        	int color = calcLight(mModelBase.mListNormals.get(i));
	        	mColors[colorCount++] = color;
	        	
	        	mModelBase.mMatrixState.projectionMap(bottom, bottom);
	        	mVerts[vertCount++] = bottom.x;
	        	mVerts[vertCount++] = bottom.y;
	        	mColors[colorCount++] = color; // 对于方向光，法向相同，则认为光照相同
	        	
	        	if (lastFront.notZero() && lastBottom.notZero()) {
	        		if (isTriangleFront(lastFront, lastBottom, front)) {
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 4);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
	        			
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 1);
	        		}
	        	}
	        	
	        	lastFront.set(front);
	        	lastBottom.set(bottom);
	        }
	        
	        Log.d("Simulate3D", "update Mesh of Profile " + (System.currentTimeMillis() - start));
		}
	};
	
	private Mesh2D mFrontFaceMesh = new Mesh2D() {
		private boolean mIsVisible = false;
		@Override
		public void init() {
			Vector3f temp = Vector3f.obtain().set2(0, 0, 1);
			mModelBase.mMatrixState.transformMap(temp, temp);
			mIsVisible = temp.z > 0;
			temp.recycle();
			
			if (mIsVisible) {
				// TRIANGLE_FAN方向初始化顶点
				mCacheArrayCount = (mModelBase.mListVerts.size() + 1) * 2; 
		    	
		    	mVerts = new float[mCacheArrayCount];
		    	mTexs = new float[mCacheArrayCount]; 
		    	
//		    	updateMesh();
			}
		}
		
		@Override
		public void draw(Canvas canvas) {
			if (mIsVisible) {
//				canvas.save();
//		        canvas.drawVertices(Canvas.VertexMode.TRIANGLE_FAN, mCacheArrayCount, mVerts, 0,
//		        		mTexs, 0, mColors, 0, mIndices, 0, mIndicesRealCount, mPaint);
//		        canvas.restore();
			}
		}
		
		@Override
		public void updateMesh() {
			long start = System.currentTimeMillis();
			
			int vertCount = 0;
	        int colorCount = 0;
	        mIndicesRealCount = 0;
	        Vector3f lastFront = new Vector3f();
	        Vector3f lastBottom = new Vector3f();
	        Vector3f front = new Vector3f();
	        Vector3f bottom = new Vector3f();
	        for (int i = 0; i < mModelBase.mListVerts.size(); i++) {
	        	front.set2(mModelBase.mListVerts.get(i));
	        	bottom.set2(front).subZ(mModelBase.mObject3d.height);
	        	
	        	mModelBase.mMatrixState.projectionMap(front, front);
	        	mVerts[vertCount++] = front.x;
	        	mVerts[vertCount++] = front.y;
	        	
	        	int color = calcLight(mModelBase.mListNormals.get(i));
	        	mColors[colorCount++] = color;
	        	
	        	mModelBase.mMatrixState.projectionMap(bottom, bottom);
	        	mVerts[vertCount++] = bottom.x;
	        	mVerts[vertCount++] = bottom.y;
	        	mColors[colorCount++] = color; // 对于方向光，法向相同，则认为光照相同
	        	
	        	if (lastFront.notZero() && lastBottom.notZero()) {
	        		if (isTriangleFront(lastFront, lastBottom, front)) {
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 4);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
	        			
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
	        			mIndices[mIndicesRealCount++] = (short)(colorCount - 1);
	        		}
	        	}
	        	
	        	lastFront.set(front);
	        	lastBottom.set(bottom);
	        }
	        
	        Log.d("Simulate3D", "update Mesh of Front face " + (System.currentTimeMillis() - start));
		}
	};
	
	private Paint mPaint = new Paint();
	
	public Shader2DImpl(ModelBase model) {
		mModelBase = model;
	}
	
	public void init() {
		mProfileMesh.init();
		mFrontFaceMesh.init();
	}
	
	// 根据三角形按照逆转计算朝向
	private Vector3f pvTemp = new Vector3f();
	private Vector3f pvTemp2 = new Vector3f();
	private boolean isTriangleFront(Vector3f pv1, Vector3f pv2, Vector3f pv3) {
		pvTemp.set2(pv2).sub(pv1);
		pvTemp2.set2(pv3).sub(pv2);
		pvTemp.crossProduct2(pvTemp2).normalize();
		return pvTemp.z > 0;
	}
	
	private Vector4f pvLight = new Vector4f();
	private int calcLight(Vector3f normal) {
    	mModelBase.mMatrixState.normalMap(normal, normal);
    	normal.normalize();
    	pvLight.set2(sLightDiffuse).scale(Math.abs(normal.dotProduct(sLightDirection)));;
    	pvLight.add(sLightAmbient);
    	int color = toColor(pvLight);
    	return color;
	}
    
    private int toColor(Vector4f v) {
    	if (v.w > 1.0f) {
    		v.w  = 1.0f;
    	}
    	int color = ((int)(v.w * 255) << 24) | ((int)(v.x * 255) << 16) 
    			| ((int)(v.y * 255) << 8) | (int)(v.z * 255);
    	return color;
    }
    
    private static Vector3f sLightDirection = new Vector3f(0, 0, 1.0f);
    private static Vector4f sLightDiffuse = new Vector4f(1.0f, 1.0f, 1.0f); // 散射光强度
    private static Vector4f sLightAmbient = new Vector4f(0f, 00f, 0f); // 环境光强度
	
	public void draw(Canvas canvas) {
		mProfileMesh.draw(canvas);
		mFrontFaceMesh.draw(canvas);
	}
}
