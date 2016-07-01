package cn.wps.graphics.shape3d.shader2D;

import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;
import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class Mesh2D extends Shader2DBase {
	protected int mCacheArrayCount = 0; // 缓存总float数
	protected int mIndicesRealCount = 0; // 缓存可能还有冗余空间，这个标记真实数目
	protected float[] mVerts;
	protected float[] mTexs;
	protected int[] mColors; // 虽然只用到一半，但要保留和顶点一样的长度，系统bug
	protected short[] mIndices;
	
	public Mesh2D(ModelBase model) {
		super(model);
	}
	
	public void render(Canvas canvas) {
		if (!mInited || !mIsVisible) {
			return;
		}
		canvas.save();
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mCacheArrayCount, mVerts, 0,
        		mTexs, 0, mColors, 0, mIndices, 0, mIndicesRealCount, mPaint);
        canvas.restore();
	}
	
	public float[] getVerts() {
		return mVerts;
	}
	
	public void dispose() {
		mInited = false;
	}
}
