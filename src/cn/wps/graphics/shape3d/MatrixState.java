package cn.wps.graphics.shape3d;

import android.graphics.RectF;
import android.opengl.Matrix;

public class MatrixState {
	private final static float FOVY = 45f; // 透视投影视场角
	
	protected RectF mViewPort = new RectF();
    protected GlMatrix mTempMat = new GlMatrix();
    protected float mEyez = 0;
    public RectF mAspectRatioRect;
    
    protected GlMatrix mMatPerspective = new GlMatrix();
    protected GlMatrix mMatView = new GlMatrix();
    protected GlMatrix mMatMVP = new GlMatrix();
    
    public void init(RectF viewPort) {
    	mViewPort.set(viewPort);
    	
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
        } else {
            ratioY = 1.0f * mViewPort.height() / mViewPort.width();
        }

        return new RectF(-ratioX, ratioY, ratioX, -ratioY);
    }
    
    private float[] mPointCache = new float[] {0, 0, 0, 1};
    public float[] mapPoint(GlMatrix mat, float x, float y, float z){
    	mPointCache[0] = x;
    	mPointCache[1] = y;
    	mPointCache[2] = z;
    	mPointCache[3] = 1;
        Matrix.multiplyMV(mPointCache, 0, mat.getValues(), 0, mPointCache, 0);
        return mPointCache;
    }
    
    private void updateMvp() {
    	mMatView.reset();
    	mMatMVP.setMatrix(mMatPerspective);
    	mMatMVP.preConcat(mMatView);
    }
}
