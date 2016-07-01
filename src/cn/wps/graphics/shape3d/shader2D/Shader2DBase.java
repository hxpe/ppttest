package cn.wps.graphics.shape3d.shader2D;

import android.graphics.Canvas;
import android.graphics.Paint;
import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;

public abstract class Shader2DBase {
	protected Paint mPaint = new Paint();
	protected ModelBase mModel;
	
	protected boolean mInited = false;
	protected boolean mIsVisible = false;
	
	public Shader2DBase(ModelBase model) {
		this.mModel = model;
	}
	
	// 需要对缓存空间的初始化，调用上允许延迟进行
	public abstract void init();
	public abstract void dispose();
	
	public abstract void render(Canvas canvas);
	
	// 实现对当前网格可见性的初始化
	protected abstract void updateVisible();
	
	// 派生类需要实现类似基类的延迟初始化
	public void update() {
		updateVisible();
		if (mIsVisible) {
			fourceInit();
		}
	}
	
	protected void fourceInit() {
		if (!mInited) {
			init();
		}
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
