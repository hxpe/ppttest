package cn.wps.graphics.shape3d;

import cn.wps.graphics.shape3d.shader2D.ShaderSoftImpl;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

public abstract class ModelBase {
	protected MatrixState mMatrixState;
	protected PathDivision mPathDivision;
	protected Object3D mObject3d;
	protected Light mLight;
	protected IShader mShader;
	protected Debugger mDebugger;
    
	public ModelBase(Object3D obj3D) {
		mObject3d = obj3D;
		mMatrixState = new MatrixState();
		mPathDivision = new PathDivision(this, true);
		mLight = new Light(this);
		mShader = new ShaderSoftImpl(this);
		mDebugger = new Debugger(this);
	}
	
	public abstract Path getShapePath();
	public abstract Bitmap getFrontTexture();
	public abstract Bitmap getBackTexture();
	
	public MatrixState getMatrixState(){
		return mMatrixState;
	}
	
	public PathDivision getPathDivision() {
		return mPathDivision;
	}
	
	public Object3D getObject3d() {
		return mObject3d;
	}
	
	public Light getLight() {
		return mLight;
	}
	
	public IShader getShader() {
		return mShader;
	}
	
	public Debugger getDebugger() {
		return mDebugger;
	}
	
	public void init(RectF viewPort) {
		mMatrixState.init(viewPort);
		mPathDivision.makeVertexs();
		mShader.init();
		update();
	}
	
	public void update() {
		if (mAnimTest.isRunning()) {
			mAnimTest.update();
		} else {
			mMatrixState.modelMatrix().reset();
			mMatrixState.modelMatrix().rotate3d(-45, 1, 0, 0);
			mMatrixState.updateMatrix();
			mShader.update();
		}
	}
	
	public void draw(Canvas canvas) {
		mShader.render(canvas);
        mDebugger.drawTest(canvas);
	}
	
	public void dispose() {
		mObject3d = null;
		
		mPathDivision.dispose();
		mPathDivision = null;
		
		mMatrixState.dispose();
		mMatrixState = null;
		
		mShader.dispose();
		mShader = null;
		
		mLight = null;
		mDebugger = null;
	}
	
	private static boolean sTestAnim = true;
	public AnimTest mAnimTest = new AnimTest();
	public class AnimTest {
		private long mStartTime = 0;
	    private long mDuration = 20000; // ms
	    private float mFraction = 0;
	    
	    private boolean mRunning = false;
	    
	    public void start() {
	    	mStartTime = System.currentTimeMillis();
	    	mFraction = 0;
	    	mRunning = true;
	    }
	    
	    public void stop() {
	    	mRunning = false;
	    }
	    
	    public boolean isRunning() {
	    	return mRunning;
	    }
	    
	    public void update() {
	    	long current = System.currentTimeMillis();
	    	this.mFraction = ((current - mStartTime) % mDuration) * 1.0f / mDuration;
	    	this.mFraction = Math.min(this.mFraction, 1.0f);
	    	
	    	float stepx = lineStep(0, 0.5f, mFraction);
	    	float stepy = lineStep(0, 1.0f, mFraction);
	    	float stepz = lineStep(0, 0.8f, mFraction);
	    	
	    	mMatrixState.modelMatrix().reset();
	    	mMatrixState.modelMatrix().rotate3d(-360 * stepx, 1, 0, 0);
			mMatrixState.modelMatrix().rotate3d(-360 * stepy, 0, 1, 0);
			mMatrixState.modelMatrix().rotate3d(-360 * stepz, 0, 0, 1);
			mMatrixState.updateMatrix();
			mShader.update();
	    }
	    
	    protected float lineStep(float edge0, float edge1, float x) {
	        float t = (x - edge0) / (edge1 - edge0);
	        t = Math.min(Math.max(0, t), 1);
	        return t * t * (3 - 2 * t);
	    }
	}
}
