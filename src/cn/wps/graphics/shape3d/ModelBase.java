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

public abstract class ModelBase {
	protected int mCacheArrayCount; // 缓存总float数
	protected float[] mVerts;
	protected float[] mTexs;
	protected int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，否则崩溃
	protected short[] mIndices;
    
    protected ArrayList<Vector3f> mListVerts = new ArrayList<Vector3f>();
    protected MatrixState mMatrixState = new MatrixState();
    
    protected Paint mPaint = new Paint();
    
    protected AnyObjPool mAnyObjPool = AnyObjPool.getPool();
    
	public ModelBase() {
		
	}
	
	public void init(RectF viewPort) {
		mMatrixState.init(viewPort);
		initVerts();
	}
	
	protected void initVerts() {
		PathDivision division = new PathDivision(this);
		division.divisionVertexs(mListVerts);
		division.dispose();
	}
	
	public void draw(Canvas canvas) {
		Bitmap textureBimatp = getTextureBitmap();
		Shader s = new BitmapShader(textureBimatp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
		mPaint.setShader(s);
		
		canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mCacheArrayCount, mVerts, 0,
        		mTexs, 0, mColors, 0, mIndices, 0, mIndices == null ? 0 : mIndices.length, mPaint);
        canvas.restore();
	}
	
	protected abstract Path getShapePath();
	protected abstract Bitmap getTextureBitmap();
}
