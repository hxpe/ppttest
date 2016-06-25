package cn.wps.graphics.shape3d;

import java.util.ArrayList;

import org.example.localbrowser.AnyObjPool;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Log;

public class PathDivision {
	public interface DivisionListener {
		Path getShapePath();
		void addVertex(Vector3f v, Vector3f n);
	}
	private final int MAX_POINT_COUNT = 60;
    private float posArray[] = new float[MAX_POINT_COUNT * 2];
    int pointCount = 0;

    private float firstPos[] = new float[2];
    private float curPos[] = new float[2];
    private float lastPos[] = new float[2];
    private float curTan[] = new float[2];
    private float lastTan[] = new float[2];
    private float preTan[] = new float[2];
    private PointF startPoint;
    private PointF endPoint;
    private int triangleCount = 0;
    private int lineCount = 0;
    private int measureLength = 0;
    private PathMeasure measure;
    private DivisionListener mListener;
    private boolean mAddOne = false;
    
    private static final float SIMILAR_TAN_MAX = 0.5f;
    private float similarTanAllow = SIMILAR_TAN_MAX;
    
    private AnyObjPool mAnyObjPool = AnyObjPool.getPool();
    
	public PathDivision(DivisionListener listener) {
		this.mListener = listener;
		
		this.measure = new PathMeasure(mListener.getShapePath(), true);
		this.measureLength = (int)measure.getLength();
		startPoint = mAnyObjPool.getPoinF();
        endPoint = mAnyObjPool.getPoinF();
	}
	
	public void dispose() {
		mAnyObjPool.tryReuse(startPoint);
		mAnyObjPool.tryReuse(endPoint);
		mAnyObjPool = null;
    }
	
	public void makeVertexs() {
		long start = System.currentTimeMillis();
        for (int i = 0; i < measureLength; i++) {
            if (measure.getPosTan(i, curPos, curTan)) {
                if (i == 0) {
                    copyTan(curTan, lastTan);
                    copyTan(curTan, preTan);
                    copyPoint(curPos, lastPos);
                    copyPoint(curPos, firstPos);
                    copyToPointArray(curPos, posArray, pointCount);
                    pointCount++;
                    continue;
                }

                // 强制Path闭合进行处理
                if (i == measureLength - 1) {
                    copyPoint(firstPos, curPos);
                }

                copyToPointArray(curPos, posArray, pointCount);
                pointCount++;

                if (isSimilarTan(curTan, lastTan, preTan) && i < measureLength - 1 && pointCount < MAX_POINT_COUNT) {
                    copyTan(curTan, preTan);
                    continue;
                }

                meetPartPath();
                pointCount = 1;
                copyToPointArray(curPos, posArray, 0);
                copyTan(curTan, lastTan);
                copyTan(curTan, preTan);
                copyPoint(curPos, lastPos);
                similarTanAllow  = SIMILAR_TAN_MAX;
            }
        }

        Log.d("testShadeGrade", "testShadeGrade len " + measureLength + ",triangleCount " + triangleCount + ",lineCount " + lineCount + ",time " + (System.currentTimeMillis() - start));
	}
	
	private void addVerts(PointF start, PointF end) {
		if (!mAddOne) {
        	mListener.addVertex(Vector3f.obtain().set2(start.x, start.y, 0), 
        			Vector3f.obtain());
        	mAddOne = true;
		}
        mListener.addVertex(Vector3f.obtain().set2(end.x, end.y, 0), 
        		Vector3f.obtain());
	}
	
	private void meetPartPath() {
        if (pointCount <= 1) {
            lineCount++;
        } else if (pointCount < 4) {
            startPoint.set(posArray[0], posArray[1]);
            endPoint.set(posArray[(pointCount - 1) * 2], posArray[(pointCount - 1) * 2 + 1]);
            addVerts(startPoint, endPoint);
            triangleCount++;
        } else {
            int mid = (int)(pointCount / 2);
            startPoint.set(posArray[0], posArray[1]);
            endPoint.set(posArray[mid * 2], posArray[mid * 2 + 1]);
            addVerts(startPoint, endPoint);
            triangleCount++;

            startPoint.set(posArray[mid * 2 ], posArray[mid * 2 + 1]);
            endPoint.set(posArray[(pointCount - 1) * 2], posArray[(pointCount - 1) * 2 + 1]);
            addVerts(startPoint, endPoint);
            triangleCount++;
        }
    }
	
	private void copyPoint(float[] point1, float[] point2) {
        point2[0] = point1[0];
        point2[1] = point1[1];
    }

    private void copyTan(float[] tan1, float[] tan2) {
        tan2[0] = tan1[0];
        tan2[1] = tan1[1];
    }

    private void copyToPointArray(float[] point1, float[] pointArray, int index) {
        if (index * 2 + 1 < pointArray.length) {
            pointArray[index * 2] = point1[0];
            pointArray[index * 2 + 1] = point1[1];
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
