package cn.wps.graphics.shape3d.shader2D;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.util.Log;
import cn.wps.graphics.shape3d.ModelBase;
import cn.wps.graphics.shape3d.Vector3f;

/**
 * 对象侧面渲染实现
 */
public class SlideFaceMesh extends Mesh2D {
	public SlideFaceMesh(ModelBase model) {
		super(model);
	}
	@Override
	public void init() {
		// 缓存区按最大算，可能实际有冗余
		ArrayList<Vector3f> verts = mModel.getPathDivision().getVerts();
		mCacheArrayCount = verts.size() * 2 * 2; 
    	
    	mVerts = new float[mCacheArrayCount];
    	mColors = new int[mCacheArrayCount]; 
    	mIndices = new short[(verts.size() - 1) * 2 * 3]; // VertexMode.TRIANGLES
    	
    	mInited = true;
	}
	
	@Override
	protected void updateVisible() {
		// TODO：整体没有x或Y方向的旋转，则侧面整体不可见
		mIsVisible = true;
	}
	
	@Override
	public void render(Canvas canvas) {
		long start = System.currentTimeMillis();
		super.render(canvas);
		Log.d("ModelBase", "draw slide " + (System.currentTimeMillis() - start));
	}
	
	@Override
	public void update() {
		super.update();
		if (!mIsVisible) {
			return;
		}
		long start = System.currentTimeMillis();
		
		int vertCount = 0;
        int colorCount = 0;
        mIndicesRealCount = 0;
        Vector3f lastFront = new Vector3f();
        Vector3f lastBottom = new Vector3f();
        Vector3f front = new Vector3f();
        Vector3f bottom = new Vector3f();
        ArrayList<Vector3f> verts = mModel.getPathDivision().getVerts();
        ArrayList<Vector3f> normals = mModel.getPathDivision().getNormals();
        for (int i = 0; i < verts.size(); i++) {
        	front.set2(verts.get(i));
        	bottom.set2(front).subZ(mModel.getObject3d().height);
        	
        	mModel.getMatrixState().projectionMap(front, front);
        	mVerts[vertCount++] = front.x;
        	mVerts[vertCount++] = front.y;
        	
        	int color = mModel.getLight().calcLight(normals.get(i));
        	mColors[colorCount++] = color;
        	
        	mModel.getMatrixState().projectionMap(bottom, bottom);
        	mVerts[vertCount++] = bottom.x;
        	mVerts[vertCount++] = bottom.y;
        	mColors[colorCount++] = color; // 对于方向光，法向相同，则认为光照相同
        	
        	if (lastFront.notZero() && lastBottom.notZero()) {
        		if (isTriangleFront(lastFront, lastBottom, front)) {
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 4);
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
        			
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 2);
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 3);
        			mIndices[mIndicesRealCount++] = (short)(colorCount - 1);
        		}
        	}
        	
        	lastFront.set(front);
        	lastBottom.set(bottom);
        }
        
        Log.d("Simulate3D", "update Mesh of Profile " + (System.currentTimeMillis() - start));
	}
}
