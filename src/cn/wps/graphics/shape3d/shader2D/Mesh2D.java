package cn.wps.graphics.shape3d.shader2D;

import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;
import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class Mesh2D {
	protected int mCacheArrayCount = 0; // 缓存总float数
	protected int mIndicesRealCount = 0; // 缓存可能还有冗余空间，这个标记真实数目
	protected float[] mVerts;
	protected float[] mTexs;
	protected int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，系统bug
	protected short[] mIndices;
	
	protected boolean mInited = false;
	protected boolean mIsVisible = false;
	
	protected Paint mPaint = new Paint();
	protected ModelBase mModel;
	public Mesh2D(ModelBase model) {
		this.mModel = model;
	}
	
	// 需要对缓存空间的初始化，调用上允许延迟进行
	public abstract void init();
	
	// 实现对当前网格可见性的初始化
	protected abstract void updateVisible();
	
	// 派生类需要实现类似基类的延迟初始化
	public void update() {
		updateVisible();
		if (mIsVisible) {
			fourceInit();
		}
	}
	
	public void draw(Canvas canvas) {
		if (!mInited || !mIsVisible) {
			return;
		}
		canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mCacheArrayCount, mVerts, 0,
        		mTexs, 0, mColors, 0, mIndices, 0, mIndicesRealCount, mPaint);
        canvas.restore();
	}
	
	protected void fourceInit() {
		if (!mInited) {
			init();
		}
	}
	
	public float[] getVerts() {
		return mVerts;
	}
	
	public boolean isVisible() {
		return mIsVisible;
	}
	
	// 根据三角形按照逆转计算朝向
	private Vector3f pvTemp = new Vector3f();
	private Vector3f pvTemp2 = new Vector3f();
	protected boolean isTriangleFront(Vector3f pv1, Vector3f pv2, Vector3f pv3) {
		pvTemp.set2(pv2).sub2(pv1);
		pvTemp2.set2(pv3).sub2(pv2);
		pvTemp.crossProduct2(pvTemp2).normalize();
		return pvTemp.z > 0;
	}
}
