package cn.wps.graphics.shape3d;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.Log;

public class PathDivision {
	public interface DivisionListener {
		Path getShapePath();
		void addVertex(Vector3f v, Vector3f n);
		void forceClosed();
	}
	private boolean forceClosed = false;
	private int similarTanStart = -1; 
	private int pointCount = 0;

    private float firstPos[] = new float[2];
    private float curPos[] = new float[2];
    private float lastPos[] = new float[2];
    private float curTan[] = new float[2];
    private float lastTan[] = new float[2];
    private float preTan[] = new float[2];
    private int triangleCount = 0;
    private int lineCount = 0;
    private int measureLength = 0;
    private PathMeasure measure;
    private DivisionListener mListener;
    
    private boolean mAddOne = false;
    private static final float SIMILAR_TAN_MAX = 0.38f;
    private float similarTanAllow = SIMILAR_TAN_MAX;
    
	public PathDivision(DivisionListener listener, boolean forceClosed) {
		this.mListener = listener;
		this.forceClosed = forceClosed;
		
		this.measure = new PathMeasure(mListener.getShapePath(), forceClosed);
		this.measureLength = (int)measure.getLength();
	}
	
	public void dispose() {
    }
	
	public void makeVertexs() {
		long start = System.currentTimeMillis();
        for (int i = 0; i < measureLength; i++) {
            if (measure.getPosTan(i, curPos, curTan)) {
                if (i == 0) {
                	System.arraycopy(curTan, 0, lastTan, 0, 2);
                	System.arraycopy(curTan, 0, preTan, 0, 2);
                	System.arraycopy(curPos, 0, lastPos, 0, 2);
                	System.arraycopy(curPos, 0, firstPos, 0, 2);
                    similarTanStart = 0;
                    pointCount = 1;
                    continue;
                }

                pointCount++;

                if (isSimilarTan(curTan, lastTan, preTan) && i < measureLength - 1) {
                	System.arraycopy(curTan, 0, preTan, 0, 2);
                    continue;
                }

                meetPartPath();
                similarTanStart = i;
                pointCount = 1;
                System.arraycopy(curTan, 0, lastTan, 0, 2);
                System.arraycopy(curTan, 0, preTan, 0, 2);
                System.arraycopy(curPos, 0, lastPos, 0, 2);
                similarTanAllow  = SIMILAR_TAN_MAX;
            }
        }
        
        // 强制Path闭合进行处理
        if (forceClosed) {
        	mListener.forceClosed();
        }

        Log.d("Simulate3D", "makeVertexs " + measureLength + ",triangleCount " + triangleCount + ",lineCount " + lineCount + ",time " + (System.currentTimeMillis() - start));
	}
	
	private float tempPos[] = new float[2];
    private float tempTan[] = new float[2];
    private Vector3f ZInerVer = new Vector3f(0, 0, 1); // 从z方向向里面的单位向量
	private void addVerts(int start, int end) {
		if (!mAddOne) {
			if (measure.getPosTan(start, tempPos, tempTan)) {
				Vector3f v = Vector3f.obtain().set2(tempPos[0], tempPos[1], 0);
				Vector3f n = Vector3f.obtain().set2(tempTan[0], tempTan[1], 0); 
				n.crossProduct2(ZInerVer).normalize();
				mListener.addVertex(v, n);
	        	mAddOne = true;
			}
		}
		if (measure.getPosTan(end, tempPos, tempTan)) {
			Vector3f v = Vector3f.obtain().set2(tempPos[0], tempPos[1], 0);
			Vector3f n = Vector3f.obtain().set2(tempTan[0], tempTan[1], 0); 
			n.crossProduct2(ZInerVer).normalize();
			mListener.addVertex(v, n);
		}
	}
	
	private void meetPartPath() {
        if (pointCount <= 1) {
            lineCount++;
        } else if (pointCount < 4) {
            addVerts(similarTanStart, similarTanStart + pointCount - 1);
            triangleCount++;
        } else {
            int mid = (int)(pointCount / 2);
            addVerts(similarTanStart, similarTanStart + mid - 1);
            triangleCount++;

            addVerts(similarTanStart + mid - 1, similarTanStart + pointCount - 1);
            triangleCount++;
        }
    }
	
    private boolean isSimilarTan(float[] curTan, float[] lastTan, float[] preTan) {
        if (curTan[0] == 0 && lastTan[0] == 0 ||
                curTan[1] == 0 && lastTan[1] == 0) {
            return true;
        }

        if (curTan[0] == 0 || lastTan[0] == 0)
            return false;

        float tan1 = curTan[1] / curTan[0];
        float tan2 = lastTan[1] / lastTan[0];
        float difTan = tan1 - tan2;
        if (Math.abs(difTan) < similarTanAllow) {
            if (pointCount > 2 && preTan != null && preTan[0] != 0) {
                float difTan2 = Math.abs(curTan[1] / curTan[0] - preTan[1] / preTan[0]);
                similarTanAllow -= difTan2;
            }
            return true;
        } else if (tan1 * tan2 > 1.0f && Math.abs(tan1) > 1.0f && Math.abs(tan2) > 1.0f) {
        	// 两点相对偏向垂直，应检查余切差异
        	difTan = 1.0f / tan1 - 1.0f / tan2;
        	if (Math.abs(difTan) < similarTanAllow) {
                if (pointCount > 2 && preTan != null && preTan[1] != 0) {
                    float difTan2 = Math.abs(1.0f / tan1 - preTan[0] / preTan[1]);
                    similarTanAllow -= difTan2;
                }
                return true;
            }
        }

        return false;
    }
}
