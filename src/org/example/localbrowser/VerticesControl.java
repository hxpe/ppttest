package org.example.localbrowser;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.Matrix;

public class VerticesControl {
	private final static float FOVY = 45f; // 透视投影视场角
	
	public int mVerTexNormalCount;
	public int mVerTextInterCount; // 附加的内插值顶点数，钝角三角形拆分成两个直角三角形，每个三角形保留一个
	public int mCacheArrayCount; // 缓存总float数
    public float[] mVerts;
    public float[] mTexs;
    public int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
    public short[] mIndices;
    public int mIndicesRealCount; // 缓存可能还有冗余空间，这个标记真实数目
    public int mAddInterVertCount; // 拆分三角形多出的内插顶点数
    
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
    	mVerTextInterCount = meshCul * 2 * meshRow;
    	mCacheArrayCount = (mVerTexNormalCount + mVerTextInterCount) * 2;
    	
    	initPerspectiveMatrix();
    	
    	mVerts = new float[mCacheArrayCount];
    	mTexs = new float[mCacheArrayCount];
    	mColors = new int[mCacheArrayCount]; 
    	mIndices = new short[meshCul * 4 * 3 * meshRow];
    }
    
    private void updateMvp() {
    	mViewMat.reset();
    	mMVP.setMatrix(mPerspective);
    	mMVP.preConcat(mViewMat);
    }
    
    public void update(RectF texRect) {
    	updateMvp();
    	updateCubic(mAspectRatioRect);
    	updateTexs(texRect);
    	updateVerts(new RectF(0, 0, 1, 1));
    	updateIndices();
    }
    
    private void updateIndices() {
    	mIndicesRealCount = 0;
    	mAddInterVertCount = 0;
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
    	PointF p1 = mAnyObjPool.getPoinF(mVerts[v1*2], mVerts[v1*2 + 1]);
    	PointF p2 = mAnyObjPool.getPoinF(mVerts[v2*2], mVerts[v2*2 + 1]);
    	PointF p3 = mAnyObjPool.getPoinF(mVerts[v3*2], mVerts[v3*2 + 1]);
    	PointF t1 = mAnyObjPool.getPoinF(mTexs[v1*2], mTexs[v1*2 + 1]);
    	PointF t2 = mAnyObjPool.getPoinF(mTexs[v2*2], mTexs[v2*2 + 1]);
    	PointF t3 = mAnyObjPool.getPoinF(mTexs[v3*2], mTexs[v3*2 + 1]);
    	float l12 = leng(p1, p2);
    	float l13 = leng(p1, p3);
    	float l23 = leng(p2, p3);
    	PointF p4 = null;
    	PointF t4 = null;
    	if (l23 > l13 && l23 > l13 && l23 * l23 > l12 * l12 + l13 * l13) {
    		// 内插点在p2-p3之间
    		p4 = getFootPoint(p1, p2, p3);
    		t4 = getFootPoint(p1, p2, p3);
    		int color = getInnerColor(p2, p3, p4, mColors[v2], mColors[v3]);
    		int innerStart = mVerTexNormalCount + mAddInterVertCount;
    		mAddInterVertCount++;
    		mColors[innerStart] = color;
    		mVerts[innerStart * 2] = p4.x;
    		mVerts[innerStart * 2 + 1] = p4.y;
    		mTexs[innerStart * 2] = t4.x;
    		mTexs[innerStart * 2 + 1] = t4.y;
    		
    		mIndices[mIndicesRealCount++] = (short)v1;
        	mIndices[mIndicesRealCount++] = (short)v2;
        	mIndices[mIndicesRealCount++] = (short)innerStart; 
        	mIndices[mIndicesRealCount++] = (short)v1;
        	mIndices[mIndicesRealCount++] = (short)innerStart;
        	mIndices[mIndicesRealCount++] = (short)v3; 
    		
    	} else if (l13 > l12 && l13 > l23 && l13 * l13 > l12 * l12 + l23 * l23) {
    		// 内插点在p1-p3之间
    		p4 = getFootPoint(p2, p1, p3);
    		t4 = getFootPoint(t2, t1, t3);
    		int color = getInnerColor(p1, p3, p4, mColors[v1], mColors[v3]);
    		int innerStart = mVerTexNormalCount + mAddInterVertCount;
    		mAddInterVertCount++;
    		mColors[innerStart] = color;
    		mVerts[innerStart * 2] = p4.x;
    		mVerts[innerStart * 2 + 1] = p4.y;
    		mTexs[innerStart * 2] = t4.x;
    		mTexs[innerStart * 2 + 1] = t4.y;
    		
    		mIndices[mIndicesRealCount++] = (short)v1;
        	mIndices[mIndicesRealCount++] = (short)v2;
        	mIndices[mIndicesRealCount++] = (short)innerStart; 
        	mIndices[mIndicesRealCount++] = (short)innerStart;
        	mIndices[mIndicesRealCount++] = (short)v2;
        	mIndices[mIndicesRealCount++] = (short)v3;
    	} else if (l12 > l13 && l12 > l23 && l12 * l12 > l13 * l13 + l23 * l23) {
    		// 内插点在p1-p2之间
    		p4 = getFootPoint(p3, p1, p2);
    		t4 = getFootPoint(t3, t1, t2);
    		int color = getInnerColor(p1, p2, p4, mColors[v1], mColors[v2]);
    		int innerStart = mVerTexNormalCount + mAddInterVertCount;
    		mAddInterVertCount++;
    		mColors[innerStart] = color;
    		mVerts[innerStart * 2] = p4.x;
    		mVerts[innerStart * 2 + 1] = p4.y;
    		mTexs[innerStart * 2] = t4.x;
    		mTexs[innerStart * 2 + 1] = t4.y;
    		
    		mIndices[mIndicesRealCount++] = (short)v1;
        	mIndices[mIndicesRealCount++] = (short)innerStart;
        	mIndices[mIndicesRealCount++] = (short)v3; 
        	mIndices[mIndicesRealCount++] = (short)innerStart;
        	mIndices[mIndicesRealCount++] = (short)v2;
        	mIndices[mIndicesRealCount++] = (short)v3;
    	} else {
    		// 锐角或直角，不需要拆分
    		mIndices[mIndicesRealCount++] = (short)v1;
        	mIndices[mIndicesRealCount++] = (short)v2;
        	mIndices[mIndicesRealCount++] = (short)v3;
    	}
    	
    	mAnyObjPool.tryReuse(p1);
    	mAnyObjPool.tryReuse(p2);
    	mAnyObjPool.tryReuse(p3);
    	mAnyObjPool.tryReuse(p4);
    	mAnyObjPool.tryReuse(t1);
    	mAnyObjPool.tryReuse(t2);
    	mAnyObjPool.tryReuse(t3);
    	mAnyObjPool.tryReuse(t4);
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
            	mColors[lightIndex++] = toColor(lightValue);
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
        
    	for (int row = 0; row < 4; row++) {
            for (int cur = 0; cur < 4; cur++) {
            	float x = rect.left + cur * wspan;
                float y = rect.bottom + row * hspan;
                float z = 0;
                if (row == 0 && cur == 0) {
                	z = -2f;
                }
                mCubicBezierPoints.get(row, cur).set(x, y, z);
            }
        }
    	mCubicBezierPoints.makeDirty();
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
