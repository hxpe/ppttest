package cn.wps.graphics.shape3d;

import java.util.ArrayList;

import org.example.localbrowser.AnyObjPool;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Style;

public abstract class ModelBase implements PathDivision.DivisionListener {
    protected ArrayList<Vector3f> mListVerts = new ArrayList<Vector3f>();
    protected ArrayList<Vector3f> mListNormals = new ArrayList<Vector3f>();
    protected MatrixState mMatrixState = new MatrixState();
    
    protected AnyObjPool mAnyObjPool = AnyObjPool.getPool();
    
    protected Object3D mObject3d = new Object3D();
    protected Shader2DImpl mShader2d;
    
    public Debugger mDebugger;
    
	public ModelBase() {
		mShader2d = new Shader2DImpl(this);
		mDebugger = new Debugger(this);
	}
	
	public void init(RectF viewPort) {
		mMatrixState.init(viewPort);
		mMatrixState.updateMatrix();
		initVerts();
		mShader2d.init();
	}
	
	protected void initVerts() {
		mListVerts.clear();
		mListNormals.clear();
		PathDivision division = new PathDivision(this, true);
		division.makeVertexs();
		division.dispose();
	}
	
	public void draw(Canvas canvas) {
		mShader2d.draw(canvas);
        mDebugger.drawFrame(canvas);
	}
	
	public abstract Path getShapePath();
	
	public void addVertex(Vector3f v, Vector3f n) {
		if (v == null || n == null) {
			return;
		}
		mListVerts.add(v);
		mListNormals.add(n);
	}
	
	public void forceClosed() {
		int size = mListVerts.size();
		if (size > 0) {
			// 最后一个代替用第一个形成闭合
			mListVerts.get(size - 1).set2(mListVerts.get(0));
			mListNormals.get(size - 1).set2(mListNormals.get(0));
		}
	}
	
	protected abstract Bitmap getTextureBitmap();
}
