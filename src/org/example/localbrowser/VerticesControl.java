package org.example.localbrowser;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.Matrix;

public class VerticesControl {
	private final static float FOVY = 45f; // 透视投影视场角
	
	public int mVerTexNormalCount;
	public int mCacheArrayCount; // 缓存总float数
    public float[] mVerts;
    public float[] mTexs;
    public int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
    public short[] mIndices;
    public int mIndicesRealCount; // 缓存可能还有冗余空间，这个标记真实数目
    
    protected int MeshRow = 10;
    protected int MeshCul = MeshRow;
    
    private RectF mViewPort = new RectF();
    protected GlMatrix mTempMat = new GlMatrix();
    private float mEyez = 0;
    protected RectF mAspectRatioRect;
    private GlMatrix mPerspective = new GlMatrix();
    private GlMatrix mViewMat = new GlMatrix();
    private GlMatrix mMVP = new GlMatrix();
    
    private AnyObjPool mAnyObjPool = AnyObjPool.getPool();
    
    private CubicBezier mCubicBezierPoints = new CubicBezier();
    
    private long mStartTime = 0;
    private long mDuration = 2000; // ms
    private float mFraction = 0;
    
    protected void initPerspectiveMatrix() {
    	mAspectRatioRect = getAspectRatioRect();
        // 透视投影
    	mPerspective.setPerspective(FOVY, Math.abs(mAspectRatioRect.width()) / Math.abs(mAspectRatioRect.height()), 0.001f, 10f);

        // 摄像机参数调整
        mTempMat.reset();
        mEyez = (float) (mAspectRatioRect.top / Math.tan(Math.toRadians(FOVY / 2)));
        mTempMat.setLookAt(0, 0, mEyez, 0, 0, 0, 0, 1f, 0);

        mPerspective.preConcat(mTempMat);
    }
    
    protected RectF getAspectRatioRect() {
        float ratioX = 1.0f;
        float ratioY = 1.0f;
        if (mViewPort.width() > mViewPort.height()) {
            ratioX = 1.0f * mViewPort.width() / mViewPort.height();
        } else {
            ratioY = 1.0f * mViewPort.height() / mViewPort.width();
        }

        return new RectF(-ratioX, ratioY, ratioX, -ratioY);
    }
    
    float[] mPointCache = new float[] {0, 0, 0, 1};
    public float[] mapPoint(GlMatrix mat, float x, float y, float z){
    	mPointCache[0] = x;
    	mPointCache[1] = y;
    	mPointCache[2] = z;
    	mPointCache[3] = 1;
        Matrix.multiplyMV(mPointCache, 0, mat.getValues(), 0, mPointCache, 0);
        return mPointCache;
    }
    
    public void init(int meshRow, int meshCul, RectF viewPort) {
    	mViewPort.set(viewPort);
    	this.MeshRow = meshRow;
    	this.MeshCul = meshCul;
    	mVerTexNormalCount = (meshRow + 1) * (meshCul + 1);
    	mCacheArrayCount = mVerTexNormalCount * 2;
    	
    	initPerspectiveMatrix();
    	
    	mVerts = new float[mCacheArrayCount];
    	mTexs = new float[mCacheArrayCount];
    	mColors = new int[mCacheArrayCount]; 
    	mIndices = new short[meshCul * 2 * 3 * meshRow];
    	
    	mStartTime = System.currentTimeMillis();
    	mFraction = 0;
    }
    
    private void updateMvp() {
    	mViewMat.reset();
    	mMVP.setMatrix(mPerspective);
    	mMVP.preConcat(mViewMat);
    }
    
    public void update(RectF texRect) {
    	long current = System.currentTimeMillis();
    	this.mFraction = ((current - mStartTime) % mDuration) * 1.0f / mDuration;
    	this.mFraction = Math.min(this.mFraction, 1.0f);
    	updateMvp();
    	updateCubic(mAspectRatioRect);
    	updateTexs(texRect);
    	updateVerts(new RectF(0, 0, 1, 1));
    	updateIndices();
    }
    
    private void updateIndices() {
    	mIndicesRealCount = 0;
    	int rowStrike = MeshCul + 1;
        for(int j = 0; j < MeshRow; j++) {
            for (int i = 0; i < MeshCul; i++) {
            	int one = rowStrike * j + i;
            	addIndicesTriangle(one, one + rowStrike, one + 1);
            	addIndicesTriangle(one + 1, one + rowStrike, one + 1 + rowStrike);
            }
        }
    }
    
    private void addIndicesTriangle(int v1, int v2, int v3) {
		mIndices[mIndicesRealCount++] = (short)v1;
    	mIndices[mIndicesRealCount++] = (short)v2;
    	mIndices[mIndicesRealCount++] = (short)v3;
    }
    
    private int getInnerColor(PointF p1, PointF p2, PointF inner, int c1, int c2) {
		float l1 = leng(p1, inner);
		float l2 = leng(inner, p2);
		float f = l1 / (l1 + l2);
		float fr = 1 - f;
		int a = (int)(((c1 >> 24) & 0xFF) * f + ((c2 >> 24) & 0xFF ) * fr);
		if (a > 255)
			a = 255;
		int r = (int)(((c1 >> 16) & 0xFF) * f + ((c2 >> 16) & 0xFF) * fr);
		if (r > 255)
			r = 255;
		int g = (int)(((c1 >> 8) & 0xFF ) * f + ((c2  >> 8) & 0xFF) * fr);
		if (g > 255)
			g = 255;
		int b = (int)((c1 & 0xff) * f + (c2 & 0xff) * fr);
		if (b > 255)
			b = 255;
		return a << 24 | r << 16 | g << 8 | b;
    }
    
    private float leng(PointF p1, PointF p2) {
    	return (float)Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }
    
    private void updateTexs(RectF texRect) {
    	float rowSpan = texRect.height() / MeshRow;
        float culSpan = texRect.width() / MeshCul;

        int count = 0;
        for(int j = 0; j <= MeshRow; j++) {
            float sy = texRect.top + j * rowSpan;
            for (int i = 0; i <= MeshCul; i++) {
                float sx = texRect.left + i * culSpan;
                mTexs[count++] = sx;
                mTexs[count++] = sy;
            }
        }
    }
    
    private void updateVerts(RectF uvRect) {
    	float rowSpan = uvRect.height() / MeshRow;
        float culSpan = uvRect.width() / MeshCul;
        
        float halfX = mViewPort.width() * 0.5f;
        float halfY = mViewPort.height() * 0.5f;

        Vector4f lightValue = new Vector4f();
        int vertIndex = 0;
        int lightIndex = 0;
        for(int j = 0;j <= MeshRow; j++) {
            float sy = uvRect.top + j * rowSpan;
            for (int i = 0; i <= MeshCul; i++) {
            	float sx = uvRect.left + i * culSpan;
            	Vector3f src = mCubicBezierPoints.smoothPoint(sx, sy);
            	// MVP转换
            	float[] dst = mapPoint(mMVP, src.x, src.y, src.z);
            	// 透视除法
            	dst[0] /= dst[3];
            	dst[1] /= dst[3];
            	// 到视口的变换
            	mVerts[vertIndex++] = halfX * (1 + dst[0]);
            	mVerts[vertIndex++] = halfY * (1 + dst[1]);
            	
            	// 光照计算
            	Vector3f normal = mCubicBezierPoints.calcNormal(sx, sy);
            	lightValue.set2(sLightDiffuse).scale(Math.abs(normal.dotProduct(sLightDirection)));;
            	lightValue.add(sLightAmbient);
            	int color = toColor(lightValue);
            	mColors[lightIndex++] = color;
            }
        }
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
    
    private void updateCubic(RectF rect) {
    	float hspan = Math.abs(rect.height()) / 3;
        float wspan = Math.abs(rect.width()) / 3;
        
        float step1 = smoothStep(0, 0.25f, this.mFraction);
        float step2 = smoothStep(0.25f, 0.5f, this.mFraction);
        float step3 = smoothStep(0.5f, 0.75f, this.mFraction);
        float step4 = smoothStep(0.25f, 1.0f, this.mFraction);
    	for (int row = 0; row < 4; row++) {
            for (int cur = 0; cur < 4; cur++) {
            	float x = rect.left + cur * wspan;
                float y = rect.bottom + row * hspan;
                float factor = 0;
                if (row == 0 && cur == 0 || row == 3 && cur == 3) {
                	factor = -1.0f;
                }
//                else if (row == 0 && cur == 3 || row == 3 && cur == 0) {
//                	factor = -1.0f;
//                }
               float z = factor;// * (-step1 + step2 + step3 -step4);
                mCubicBezierPoints.get(row, cur).set(x, y, z);
            }
        }
    	mCubicBezierPoints.makeDirty();
    }
    
 // 执行0~1之间的平滑Hermite插值
    protected float smoothStep(float edge0, float edge1, float x) {
        float t = (x - edge0) / (edge1 - edge0);
        t = Math.min(Math.max(0, t), 1);
        return t * t * (3 - 2 * t);
    }
    
    /**
	 * 求一点到直线(另两点决定)的垂足（D）
	 * @param C 直线外一点
	 * @param A 直线上一点
	 * @param B 直线上另一点
	 * @return 垂足坐标
	 */
	static public PointF getFootPoint(PointF C, PointF A, PointF B) {
		PointF destPoint = null;
		if (A.x == B.x) { 
			// 垂直
			destPoint = AnyObjPool.getPool().getPoinF(A.x, C.y);
		} else if (A.y == B.y) {
			// 水平
			destPoint = AnyObjPool.getPool().getPoinF(C.x, A.y);
		} else {
			float k = (B.y-A.y) / (B.x-A.x);
			destPoint = getFootPoint(k, A, C);
		}
		
		return destPoint;
	}
	
	/**
	 * 求一点到直线(由A和k决定)的垂足（D）
	 * @param k 过A的直线的斜率，必须存在
	 * @param A 斜率为k的直线上的一点 
	 * @param C 直线外一点 
	 * @return 垂足坐标
	 */
	static public PointF getFootPoint(float k, PointF A, PointF C) {
		float k1 = (C.y-A.y) / (C.x-A.x); 
		if (k1 == k) {
			return AnyObjPool.getPool().getPoinF(C.x, C.y);
		}
        /*
		         设直线AB方程式为：y-yA=k*（x-xA）,斜率公式:k=(yB-yA)/(xB-xA)
		           直线外点C，设垂足D
		            ∵两条垂直直线的斜率乘积 = -1
		            ∴由AB线斜率为k可知CD直线线斜率为-1/k，可知直线CD方程式为y-yC=-1/k*（x-xC）
		        联立二元方程组，解得：
		            x = (k * xA+ xC / k + yC - yA) / (1 / k + k)
		        再代入BC方程得：
            y=k*(x-xA)+ yA
         */
		float x = (k * A.x+ C.x / k + C.y - A.y) / (1 / k + k);
		float y = k*(x-A.x)+ A.y;
		return AnyObjPool.getPool().getPoinF(x, y);
	}
}
