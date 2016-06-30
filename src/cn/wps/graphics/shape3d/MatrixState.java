package cn.wps.graphics.shape3d;

import android.graphics.RectF;
import android.opengl.Matrix;

public class MatrixState {
	private final static float FOVY = 45f; // 透视投影视场角
	
	private RectF mViewPort = new RectF();
    protected GlMatrix mTempMat = new GlMatrix();
    protected float mEyez = 0;
    public RectF mRenderRect;
    
    protected GlMatrix mMatPerspective = new GlMatrix();
    protected GlMatrix mMatModel = new GlMatrix();
    protected GlMatrix mMatView = new GlMatrix();
    protected GlMatrix mMatTransform = new GlMatrix();
    protected GlMatrix mMatMVP = new GlMatrix();
    protected GlMatrix mMatNormal = new GlMatrix();
    
    protected float mHalfX;
    protected float mHalfY;
    protected float mCenterX;
    protected float mCenterY;
    
    public void init(RectF viewPort) {
    	mViewPort.set(viewPort);
    	mHalfX = mViewPort.width() / 2;
    	mHalfY = mViewPort.height() / 2;
    	mCenterX = mViewPort.centerX();
    	mCenterY = mViewPort.centerY();
    	mRenderRect = new RectF(-mHalfX, mHalfY, mHalfX, -mHalfY);
    	initPerspectiveMatrix();
    }
    
    public void dispose() {
    	
    }
    
    public RectF getViewPort() {
    	return this.mViewPort;
    }
    
    public GlMatrix modelMatrix() {
    	return mMatModel;
    }
    
    protected void initPerspectiveMatrix() {
        // 透视投影
    	mEyez = (float) (mRenderRect.top / Math.tan(Math.toRadians(FOVY / 2)));
    	mMatPerspective.setFrustum(mRenderRect.left, mRenderRect.right, 
    			mRenderRect.bottom, mRenderRect.top, mEyez, 10f);

        // 摄像机参数调整
        mTempMat.reset();
        mTempMat.setLookAt(0, 0, mEyez, 0, 0, 0, 0, 1f, 0);
        mMatPerspective.preConcat(mTempMat);
    }
    
    private float[] mPointCache = new float[] {0, 0, 0, 1};
    public void projectionMap(Vector3f src, Vector3f dest) {
    	synchronized (mPointCache) {
    		// 变换到opengl的坐标空间(对象中心为原点，Y向上正向)
    		mPointCache[0] = (src.x - mCenterX);
        	mPointCache[1] = (mCenterY - src.y);
        	mPointCache[2] = src.z;
        	mPointCache[3] = 1;
        	// MVP转换
        	Matrix.multiplyMV(mPointCache, 0, mMatMVP.getValues(), 0, mPointCache, 0);
        	// 透视除法
        	dest.x = mPointCache[0] / mPointCache[3];
        	dest.y = mPointCache[1] / mPointCache[3];
        	dest.z = mPointCache[2] / mPointCache[3];
		}
    	
    	// 变换回Canvas的视口坐标空间（对象左上角为原点，Y向下正向）
    	// Z仍保留在规范化坐标空间中[-1,1],后面可能用到的地方是深度排序
    	dest.x = mCenterX + mHalfX * dest.x;
    	dest.y = mCenterY - mHalfY * dest.y;
    }
    
    public void normalMap(Vector3f src, Vector3f dest) {
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
    
    public void transformMap(Vector3f src, Vector3f dest) {
    	synchronized (mPointCache) {
    		mPointCache[0] = src.x;
        	mPointCache[1] = src.y;
        	mPointCache[2] = src.z;
        	mPointCache[3] = 1;
        	Matrix.multiplyMV(mPointCache, 0, mMatTransform.getValues(), 0, mPointCache, 0);
        	dest.x = mPointCache[0];
        	dest.y = mPointCache[1];
        	dest.z = mPointCache[2];
		}
    }
    
    public void updateMatrix() {
    	mMatTransform.setMatrix(mMatView);
    	mMatTransform.preConcat(mMatModel);
    	
    	mMatMVP.setMatrix(mMatPerspective);
    	mMatMVP.preConcat(mMatTransform);
    	
    	mMatNormal.setMatrix(mMatTransform);
    	mMatNormal.invertAndTranspose();
    }
}
