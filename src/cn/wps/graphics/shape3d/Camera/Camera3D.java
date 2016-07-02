package cn.wps.graphics.shape3d.Camera;

import android.graphics.Canvas;
import android.graphics.Matrix;
import cn.wps.graphics.shape3d.Matrix3D;
import cn.wps.graphics.shape3d.Vector3f;

public class Camera3D {
	private Vector3f mLocation = new Vector3f();
	private Vector3f mAxis = new Vector3f();
	private Vector3f mZenith = new Vector3f();
	private Vector3f mObserver = new Vector3f();
	private boolean mNeedToUpdate = true;
	private final float[] mOrientation = new float[9];
	
	private Matrix3D mTransfrom = new Matrix3D();
	
	private Patch3D mPatch = new Patch3D();
	
	public Camera3D() {
	    reset();
	}
	
	public void setLocation(float x, float y, float z) {
	    // the camera location is passed in inches, set in pt
	    float lz = z * 72.0f;
	    mLocation.set(x * 72.0f, y * 72.0f, lz);
	    mObserver.set(0, 0, lz);
	    update();

	}

	public float getLocationX() {
	    return mLocation.x / 72.0f;
	}

	public float getLocationY() {
	    return mLocation.y / 72.0f;
	}

	public float getLocationZ() {
	    return mLocation.z / 72.0f;
	}
	
	public void save() {
		
	}
	
	public void restore() {
		
	}
	
	// 3D变换的目标矩阵
	public Matrix3D getTransfromMatrix() {
		return mTransfrom;
	}
	
	public void getMatrix(Matrix matrix) {
	    if (matrix != null) {
	    	mPatch.reset();
	        mPatch.transform(mTransfrom);
	        patchToMatrix(mPatch, matrix);
	    }
	}

	private Matrix mCachematrix = new Matrix();
	public void applyToCanvas(Canvas canvas) {
	    getMatrix(mCachematrix);
	    canvas.concat(mCachematrix);
	}

	public void reset() {
	    mLocation.set(0, 0, -576f);   // 8 inches backward
	    mAxis.set(0, 0, 1);           // forward
	    mZenith.set(0, -1, 0);             // up
	    mObserver.set(0, 0, mLocation.z);
	    mNeedToUpdate = true;
	}
	
	public void update() {
	    mNeedToUpdate = true;
	}
	
	public void doUpdate() {
	    Vector3f axis = Vector3f.obtain().set2(mAxis).normalize2();
	    Vector3f zenith , cross;

        float dot = mZenith.dotProduct(axis);
        
        // 纠正向上向量使和axis垂直
        zenith = Vector3f.obtain().set2(mZenith);
        zenith.sub2(dot * axis.x, dot * axis.y, dot * axis.z).normalize();
        cross = Vector3f.obtain().set2(axis).crossProduct2(zenith);

        float x = mObserver.x;
        float y = mObserver.y;
        float z = mObserver.z;
        
        mOrientation[Matrix.MSCALE_X] = x * axis.x - z * cross.x;
        mOrientation[Matrix.MSKEW_X] = x * axis.y - z * cross.y;
        mOrientation[Matrix.MTRANS_X] = x * axis.z - z * cross.z;
        mOrientation[Matrix.MSKEW_Y] = y * axis.x - z * zenith.x;
        mOrientation[Matrix.MSCALE_Y] = y * axis.y - z * zenith.y;
        mOrientation[Matrix.MTRANS_Y] = y * axis.z - z * zenith.z;
        mOrientation[Matrix.MPERSP_0] = axis.x;
        mOrientation[Matrix.MPERSP_1] = axis.y;
        mOrientation[Matrix.MPERSP_2] = axis.z;
        
        axis.recycle();
        zenith.recycle();
        cross.recycle();
	}

	float[] matrixValue = new float[9];
	public void patchToMatrix(final Patch3D quilt, Matrix matrix) {
	    if (mNeedToUpdate) {
	        doUpdate();
	        mNeedToUpdate = false;
	    }

	    final float[] mapPtr = mOrientation;
	    Vector3f diff = Vector3f.obtain().set2(quilt.mOrigin).sub2(mLocation);
	    Vector3f persp = Vector3f.obtain().set2(mapPtr[Matrix.MPERSP_0], 
	    		mapPtr[Matrix.MPERSP_1], mapPtr[Matrix.MPERSP_2]);
	    float dot = diff.dotProduct(persp);
	    diff.recycle();
	    persp.recycle();
	    
	    matrix.getValues(matrixValue);
	    float[] patchPtr = getVectorCache(quilt.mU);
	    matrixValue[Matrix.MSCALE_X] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 0, 1, dot);
	    matrixValue[Matrix.MSKEW_Y] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 3, 1, dot);
	    matrixValue[Matrix.MPERSP_0] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 6, 1, dot);

	    patchPtr = getVectorCache(quilt.mV);
	    matrixValue[Matrix.MSKEW_X] =  Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 0, 1, dot);
	    matrixValue[Matrix.MSCALE_Y] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 3, 1, dot);
	    matrixValue[Matrix.MPERSP_1] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 6, 1, dot);

	    patchPtr = getVectorCache(quilt.mOrigin);
	    matrixValue[Matrix.MTRANS_X] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 0, 1, dot);
	    matrixValue[Matrix.MTRANS_Y] = Matrix3D.scalarDotDiv(3, patchPtr, 0, 1, mapPtr, 3, 1, dot);
	    matrixValue[Matrix.MPERSP_2] = 1f;
	    matrix.setValues(matrixValue);
	}
	 
	private float[] mVectorCache = new float[3];
	private float[] getVectorCache(Vector3f v) {
		mVectorCache[0] = v.x;
		mVectorCache[1] = v.y;
		mVectorCache[2] = v.z;
		return mVectorCache;
	}
}
