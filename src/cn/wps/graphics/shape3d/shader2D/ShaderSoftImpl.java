package cn.wps.graphics.shape3d.shader2D;

import android.graphics.Canvas;
import cn.wps.graphics.shape3d.IShader;
import cn.wps.graphics.shape3d.ModelBase;

/**
 * 2D绘图函数模拟实现3D效果
 */
public class ShaderSoftImpl implements IShader {
	private ModelBase mModel;
	private Mesh2D mSlideFaceMesh;
	private Mesh2D mFrontFaceMesh;
	private Mesh2D mBackFaceMesh;
	
	public ShaderSoftImpl(ModelBase model) {
		mModel = model;
		mSlideFaceMesh = new SlideFaceMesh(model);
		mFrontFaceMesh = new FrontFaceMesh(model);
		mBackFaceMesh = new BackFaceMesh(model);
	}
	
	public void init() {
		mSlideFaceMesh.init();
		mFrontFaceMesh.init();
		mBackFaceMesh.init();
	}
	
	public void update() {
		mSlideFaceMesh.update();
		mFrontFaceMesh.update();
		mBackFaceMesh.update();
	}
	
	public void render(Canvas canvas) {
		mSlideFaceMesh.draw(canvas);
		mFrontFaceMesh.draw(canvas);
		mBackFaceMesh.draw(canvas);
	}
	
	public void dispose() {
		
	}
	
	public Mesh2D getSlideFace() {
		return mSlideFaceMesh;
	}
	
	public Mesh2D getFrontFace() {
		return mFrontFaceMesh;
	}
	
	public Mesh2D getBackFace() {
		return mBackFaceMesh;
	}
}
