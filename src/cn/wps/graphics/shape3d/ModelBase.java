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
    public static boolean sDebug = true;
    
    protected Object3D mObject3d = new Object3D();
    private Simulate3D mCache2d;
    
	public ModelBase() {
		mCache2d = new Simulate3D(this);
	}
	
	public void init(RectF viewPort) {
		mMatrixState.init(viewPort);
		initVerts();
	}
	
	protected void initVerts() {
		mListVerts.clear();
		mListNormals.clear();
		PathDivision division = new PathDivision(this, true);
		division.makeVertexs();
		division.dispose();
		mMatrixState.updateMatrix();
		mCache2d.init();
	}
	
	public void addVertex(Vector3f v, Vector3f n) {
		if (v == null || n == null) {
			return;
		}
		mListVerts.add(v);
		mListNormals.add(n);
	}
	
	public abstract Path getShapePath();
	
	public void draw(Canvas canvas) {
		drawFrame(canvas);
		mCache2d.draw(canvas);
//        if (sDebug) {
//        	drawPoints(canvas);
//        }
	}
	
	private Vector3f temp3f = new Vector3f();
	private Paint testPaint = new Paint();
	private void drawPoints(Canvas canvas) {
		testPaint.setStrokeWidth(5);
		for (int i = 0, size = mListVerts.size(); i < size; i++) {
			if (i == size - 1) {
				testPaint.setColor(0xffff0000);
			} else {
				testPaint.setColor(0xff00ff00);
			}
			Vector3f v = mListVerts.get(i);
//			temp3f.set2(mListNormals.get(i)).scale(100);
//			canvas.drawLine(v.x, v.y, v.x + temp3f.x, v.y + temp3f.y, testPaint);
			canvas.drawPoint(v.x, v.y, testPaint);
		}
	}
	
	private void drawFrame(Canvas canvas) {
		testPaint.setStyle(Style.STROKE);
		testPaint.setStrokeWidth(2);
		testPaint.setColor(0xFF0000ff);
		canvas.drawRect(mMatrixState.mViewPort, testPaint);
	}
	
	protected abstract Bitmap getTextureBitmap();
}
