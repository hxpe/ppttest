package org.example.localbrowser;

import android.graphics.RectF;
import android.opengl.Matrix;

public class VerticesControl {
	private final static float FOVY = 45f; // 透视投影视场角
	
	public int mVerTexCount;
    public float[] mVerts;
    public float[] mTexs;
    public int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
    public short[] mIndices;
    
    protected int MeshRow = 10;
    protected int MeshCul = MeshRow;
    
    private RectF mViewPort = new RectF();
    protected GlMatrix mTempMat = new GlMatrix();
    private float mEyez = 0;
    protected RectF mAspectRatioRect;
    private GlMatrix mPerspective = new GlMatrix();
    private GlMatrix mViewMat = new GlMatrix();
    private GlMatrix mMVP = new GlMatrix();
    
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
    	mVerTexCount = (meshRow + 1) * (meshCul + 1) * 2; 
    	
    	initPerspectiveMatrix();
    	
    	mVerts = new float[mVerTexCount];
    	mTexs = new float[mVerTexCount];
    	mColors = new int[mVerTexCount]; 
    	mIndices = new short[meshCul * 2 * 3 * meshRow];
    }
    
    private void updateMvp() {
    	mViewMat.reset();
    	mViewMat.setScale3d(0.5f, 0.5f, 1);
    	mMVP.setMatrix(mPerspective);
    	mMVP.preConcat(mViewMat);
    }
    
    public void update(RectF renderRect, RectF texRect) {
    	updateMvp();
    	updateCubic(renderRect);
    	updateTexs(texRect);
    	updateVerts(new RectF(0, 0, 1, 1));
    	updateIndices();
    }
    
    private void updateIndices() {
    	int count = 0;
    	int rowStrike = MeshCul + 1;
        for(int j = 0; j < MeshRow; j++) {
            for (int i = 0; i < MeshCul; i++) {
            	int one = rowStrike * j + i;
            	mIndices[count++] = (short)one;
            	mIndices[count++] = (short)(one + rowStrike);
            	mIndices[count++] = (short)(one + 1);
            	
            	mIndices[count++] = (short)(one + 1);
            	mIndices[count++] = (short)(one + rowStrike);
            	mIndices[count++] = (short)(one + 1 + rowStrike);
            }
        }
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

        int count = 0;
        for(int j = 0;j <= MeshRow; j++) {
            float sy = uvRect.top + j * rowSpan;
            for (int i = 0; i <= MeshCul; i++) {
            	float sx = uvRect.left + i * culSpan;
            	Vector3f src = mCubicBezierPoints.smoothPoint(sx, sy);
            	float[] dst = mapPoint(mMVP, src.x, src.y, src.z);
            	mVerts[count++] = dst[0] * dst[0] / mViewPort.width() / dst[3];
            	mVerts[count++] = dst[1] * dst[1] / mViewPort.height() / dst[3];
            }
        }
    }
    
    private void updateCubic(RectF rect) {
    	float hspan = Math.abs(rect.height()) / 3;
        float wspan = Math.abs(rect.width()) / 3;
        
    	for (int row = 0; row < 4; row++) {
            for (int cur = 0; cur < 4; cur++) {
            	float x = rect.left + cur * wspan;
                float y = rect.top + row * hspan;
                float z = 0;
                if (row == 0 && cur == 0) {
                	z = 10;
                }
                
                mCubicBezierPoints.get(row, cur).set(x, y, z);
            }
        }
    	mCubicBezierPoints.makeDirty();
    }
}
