package cn.wps.graphics.shape3d;

import android.R.color;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class Simulate3D {
	protected int mCacheArrayCount; // 缓存总float数
	protected float[] mVerts;
	protected float[] mTexs;
	protected int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
	protected short[] mIndices;
	protected int mIndicesRealCount; // 缓存可能还有冗余空间，这个标记真实数目
	
	protected Paint mPaint = new Paint();
	
	private ModelBase mModelBase;
	public Simulate3D(ModelBase model) {
		mModelBase = model;
	}
	
	public void init() {
		// 缓存区按最大算，可能实际有冗余
		mCacheArrayCount = mModelBase.mListVerts.size() * 2 * 2; 
    	
    	mVerts = new float[mCacheArrayCount];
    	mColors = new int[mCacheArrayCount]; 
    	mIndices = new short[(mModelBase.mListVerts.size() - 1) * 2 * 3]; // VertexMode.TRIANGLES
    	
    	update2DMesh();
	}
	
	private void update2DMesh() {
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
        	
        	mModelBase.mMatrixState.mapVert(front);
        	mVerts[vertCount++] = front.x;
        	mVerts[vertCount++] = front.y;
        	
        	int color = calcLight(mModelBase.mListNormals.get(i));
        	mColors[colorCount++] = color;
        	
        	mModelBase.mMatrixState.mapVert(bottom);
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
    	mModelBase.mMatrixState.mapNormal(normal, normal);
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
//		Bitmap textureBimatp = mModelBase.getTextureBitmap();
//		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
//                Shader.TileMode.CLAMP);
//		mPaint.setShader(s);
		
		canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mCacheArrayCount, mVerts, 0,
        		mTexs, 0, mColors, 0, mIndices, 23, 3, mPaint);
        mPaint.setShader(null);
        canvas.restore();
        
        drawPoints(canvas);
	}
	
	private Paint testPaint = new Paint();
	private void drawPoints(Canvas canvas) {
		testPaint.setStrokeWidth(5);
		for (int i = 0, size = mVerts.length / 4; i < size; i++) {
			if (i == size - 1) {
				testPaint.setColor(0xffff0000);
			} else {
				testPaint.setColor(0xff00ff00);
			}
			canvas.drawLine(mVerts[i * 4], mVerts[i * 4 + 1], 
					mVerts[i * 4 + 2], mVerts[i * 4 + 3], testPaint);
		}
	}
}
