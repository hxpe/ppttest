package cn.wps.graphics.shape3d;

import android.graphics.RectF;
import android.opengl.Matrix;

public class MatrixState {
	private final static float FOVY = 45f; // 透视投影视场角
	
	public RectF mViewPort = new RectF();
    protected GlMatrix mTempMat = new GlMatrix();
    protected float mEyez = 0;
    public RectF mAspectRatioRect;
    
    protected GlMatrix mMatPerspective = new GlMatrix();
    protected GlMatrix mMatModel = new GlMatrix();
    protected GlMatrix mMatView = new GlMatrix();
    protected GlMatrix mMatMVP = new GlMatrix();
    protected GlMatrix mMatNormal = new GlMatrix();
    
    protected float mOneLength;
    protected float mCenterX;
    protected float mCenterY;
    
    public void init(RectF viewPort) {
    	mViewPort.set(viewPort);
    	mCenterX = mViewPort.centerX();
    	mCenterY = mViewPort.centerY();
    	initPerspectiveMatrix();
    }
    
    protected void initPerspectiveMatrix() {
    	mAspectRatioRect = getAspectRatioRect();
        // 透视投影
    	mMatPerspective.setPerspective(FOVY, Math.abs(mAspectRatioRect.width()) / Math.abs(mAspectRatioRect.height()), 0.001f, 10f);

        // 摄像机参数调整
        mTempMat.reset();
        mEyez = (float) (mAspectRatioRect.top / Math.tan(Math.toRadians(FOVY / 2)));
        mTempMat.setLookAt(0, 0, mEyez, 0, 0, 0, 0, 1f, 0);

        mMatPerspective.preConcat(mTempMat);
    }
    
    protected RectF getAspectRatioRect() {
        float ratioX = 1.0f;
        float ratioY = 1.0f;
        if (mViewPort.width() > mViewPort.height()) {
            ratioX = 1.0f * mViewPort.width() / mViewPort.height();
            mOneLength = mViewPort.height() / 2;
        } else {
            ratioY = 1.0f * mViewPort.height() / mViewPort.width();
            mOneLength = mViewPort.width() / 2;
        }

        return new RectF(-ratioX, ratioY, ratioX, -ratioY);
    }
    
    private float[] mPointCache = new float[] {0, 0, 0, 1};
    public void mapVert(Vector3f src, Vector3f dest) {
    	synchronized (mPointCache) {
    		mPointCache[0] = (src.x - mCenterX) / mOneLength;
        	mPointCache[1] = (src.y - mCenterY) / mOneLength;
        	mPointCache[2] = src.z / mOneLength;
        	mPointCache[3] = 1;
        	// MVP转换
        	Matrix.multiplyMV(mPointCache, 0, mMatMVP.getValues(), 0, mPointCache, 0);
        	// 透视除法
        	dest.x = mPointCache[0] / mPointCache[3];
        	dest.y = mPointCache[1] / mPointCache[3];
        	dest.z = mPointCache[2] / mPointCache[3];
		}
    	
    	// 到视口的变换
    	dest.x = mCenterX + mOneLength * dest.x;
    	dest.y = mCenterY + mOneLength * dest.y;
    	dest.z = mOneLength * dest.z;
    }
    
    public void mapVert(Vector3f src) {
    	mapVert(src, src);
    }
    
    public void mapNormal(Vector3f src, Vector3f dest) {
    	synchronized (mPointCache) {
    		mPointCache[0] = src.x;
        	mPointCache[1] = src.y;
        	mPointCache[2] = src.z;
        	mPointCache[3] = 1;
        	Matrix.multiplyMV(mPointCache, 0, mMatNormal.getValues(), 0, mPointCache, 0);
        	dest.x = mPointCache[0];
        	dest.y = mPointCache[1];
        	dest.z = mPointCache[2];
		}
    }
    
    public void mapNormal(Vector3f src) {
    	mapNormal(src, src);
    }
    
    public void updateMatrix() {
    	// test
    	mMatModel.reset();
    	mMatModel.rotate3d(-45, 0, 1, 0);
    	
    	mMatMVP.setMatrix(mMatPerspective);
    	mMatMVP.preConcat(mMatView);
    	mMatMVP.preConcat(mMatModel);
    	
    	mMatNormal.setMatrix(mMatView);
    	mMatNormal.preConcat(mMatModel);
    	mMatNormal.invertAndTranspose();
    }
}
