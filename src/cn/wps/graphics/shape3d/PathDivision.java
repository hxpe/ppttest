package cn.wps.graphics.shape3d;

import java.util.ArrayList;

import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Log;

public class PathDivision {
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
    private ModelBase mModel;
    
    private static final float SIMILAR_TAN_MAX = 0.16f;
    private float similarTanAllow = SIMILAR_TAN_MAX;
    
	public PathDivision(ModelBase model) {
		this.mModel = model;
		
		this.measure = new PathMeasure(model.getShapePath(), true);
		this.measureLength = (int)measure.getLength();
		startPoint = model.mAnyObjPool.getPoinF();
        endPoint = model.mAnyObjPool.getPoinF();
	}
	
	public void dispose() {
		mModel.mAnyObjPool.tryReuse(startPoint);
		mModel.mAnyObjPool.tryReuse(endPoint);
    }
	
	public void divisionVertexs(ArrayList<Vector3f> listVerts) {
		long start = System.currentTimeMillis();
		listVerts.clear();
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

                meetPartPath(listVerts);
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
	
	private void meetPartPath(ArrayList<Vector3f> listVerts) {
        if (pointCount <= 1) {
            lineCount++;
        } else if (pointCount < 4) {
            startPoint.set(posArray[0], posArray[1]);
            endPoint.set(posArray[(pointCount - 1) * 2], posArray[(pointCount - 1) * 2 + 1]);
            if (listVerts.size() == 0)
            	listVerts.add(Vector3f.obtain().set2(startPoint.x, startPoint.y, 0));
            listVerts.add(Vector3f.obtain().set2(endPoint.x, endPoint.y, 0));
            triangleCount++;
        } else {
            int mid = (int)(pointCount / 2);
            startPoint.set(posArray[0], posArray[1]);
            endPoint.set(posArray[mid * 2], posArray[mid * 2 + 1]);
            if (listVerts.size() == 0)
            	listVerts.add(Vector3f.obtain().set2(startPoint.x, startPoint.y, 0));
            listVerts.add(Vector3f.obtain().set2(endPoint.x, endPoint.y, 0));
            triangleCount++;

            startPoint.set(posArray[mid * 2 ], posArray[mid * 2 + 1]);
            endPoint.set(posArray[(pointCount - 1) * 2], posArray[(pointCount - 1) * 2 + 1]);
            listVerts.add(Vector3f.obtain().set2(endPoint.x, endPoint.y, 0));
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

        float difTan = curTan[1] / curTan[0] - lastTan[1] / lastTan[0];
        if (Math.abs(difTan) < similarTanAllow) {
            if (pointCount > 2 && preTan != null && preTan[0] != 0) {
                float difTan2 = Math.abs(curTan[1] / curTan[0] - preTan[1] / preTan[0]);
                similarTanAllow -= difTan2;
            }
            return true;
        }

        return false;
    }
}
