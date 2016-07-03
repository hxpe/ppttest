package cn.wps.graphics.shape3d.shader2D;

import android.graphics.Canvas;
import android.util.Log;
import cn.wps.graphics.shape3d.IShader;
import cn.wps.graphics.shape3d.ModelBase;

/**
 * 2D绘图函数模拟实现3D效果
 */
public class ShaderSoftImpl implements IShader {
	private ModelBase mModel;
	private Shader2DBase mSlideFaceMesh;
	private Shader2DBase mFrontFaceMesh;
	private Shader2DBase mBackFaceMesh;
	
	public ShaderSoftImpl(ModelBase model) {
		mModel = model;
		mSlideFaceMesh = new SlideFaceMesh(model);
		
		if (isCamera3DEnable(false)) {
			mFrontFaceMesh = new MainFaceRender(model, false);
		} else {
			mFrontFaceMesh = new FrontFaceMesh(model);
		}
		
		if (isCamera3DEnable(true)) {
			mBackFaceMesh = new MainFaceRender(model, true);
		} else {
			mBackFaceMesh = new BackFaceMesh(model);
		}
	}
	
	public void init() {
		long start = System.currentTimeMillis();
		mSlideFaceMesh.init();
		mFrontFaceMesh.init();
		mBackFaceMesh.init();
		Log.d("ModelBase", "shader2D init " + (System.currentTimeMillis() - start));
	}
	
	public void update() {
		mSlideFaceMesh.update();
		mFrontFaceMesh.update();
		mBackFaceMesh.update();
	}
	
	// 当前状态是否允许使用Camera3D构造变换矩阵
	protected boolean isCamera3DEnable(boolean backFace) {
		// TODO；如果存在棱台的情况下，不能简单的靠转换矩阵了
		return true;
	}
	
	public void render(Canvas canvas) {
		mSlideFaceMesh.render(canvas);
		mFrontFaceMesh.render(canvas);
		mBackFaceMesh.render(canvas);
	}
	
	public void dispose() {
		mSlideFaceMesh.dispose();
		mFrontFaceMesh.dispose();
		mBackFaceMesh.dispose();
	}
	
	public Shader2DBase getSlideFace() {
		return mSlideFaceMesh;
	}
	
	public Shader2DBase getFrontFace() {
		return mFrontFaceMesh;
	}
	
	public Shader2DBase getBackFace() {
		return mBackFaceMesh;
	}
}
