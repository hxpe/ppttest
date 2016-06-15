package org.example.localbrowser;

/**
 * Created by hxpe on 2016/4/28.
 * 双三次贝塞尔曲面求值器，包括控制点管理和提供求值过程
 * 矩阵表示公式：Q(u,v) = U*Mb*G*MbT*VT，
 *      上式中的Bernstein多项式构成双三次Bezier曲面的一组基，其中
 *      U : [u^3  u^2  u  1]，u取值为[0,1]
 *      VT : [v^3 v^2 v 1]的转置，v取值为[0,1]
 *      Mb : [-1, 3, -3, 1,
 *            3, -6, 3, 0,
 *           -3,  3, 0, 0,
 *            1,  0, 0, 0]
 *      MbT : Mb的转置
 *      G : 几何顶点矩阵，x、y、z分量各有一个
 *          P30--P31--P32--P33
 *           |     |    |    |
 *          P20--P21--P22--P23
 *           |     |    |    |
 *          P10--P11--P12--P13
 *           |     |    |    |
 *          P00--P01--P02--P03
 *
 *  光照法向量计算过程：
 *      求Q(u,v)分别在u、v方向上的偏导数（切线向量），并通过叉积运算得到法向量
 *      u方向的偏导数为：∂G(u,v)/∂u = DU*Mb*G*MbT*VT, DU = [3u^2  2u  1  0]
 *      v方向的偏导数为：∂G(u,v)/∂v = U*Mb*G*MbT*DVT，DVT = [3v^2  2v  1  0]T
 *
 *  smoothPoint()及calcNormal()建议只用来测试，矩阵的运算虽已减少计算过程，但为确保更好的性能
 *  推荐应是把矩阵(Mb*G*MbT的总矩阵)推送到顶点着色器进行顶点及光照法向量计算
 */

import android.opengl.Matrix;
import java.util.ArrayList;

public class CubicBezier {
    // xyz坐标索引号
    public static final int XAxis = 0;
    public static final int YAxis = 1;
    public static final int ZAxis = 2;

    public static class Point{
        private float[] mValue = new float[3];
        public Point() {
            set(0, 0, 0);
        }

        public Point(float x, float y, float z) {
            set(x, y, z);
        }

        public void set(float x, float y, float z) {
            mValue[0] = x;
            mValue[1] = y;
            mValue[2] = z;
        }

        public float get(int axisIndex) {
            return mValue[axisIndex];
        }

        public float length() {
            return (float) Math.sqrt(mValue[0] * mValue[0] + mValue[1] * mValue[1] + mValue[2] * mValue[2]);
        }

        public Point normalize() {
            float dist = length();
            if (dist != 0)
            {
                mValue[0] /= dist;
                mValue[1] /= dist;
                mValue[2] /= dist;
            }

            return this;
        }

        public static Point cross(Point a, Point b) {
            Point result = new Point();
            result.set(a.mValue[1] * b.mValue[2] - a.mValue[2] * b.mValue[1],
                    a.mValue[2] * b.mValue[0] - a.mValue[0] * b.mValue[2],
                    a.mValue[0] * b.mValue[1] - a.mValue[1] * b.mValue[0]);
            return result;
        }

        public static Point minus(Point a, Point b) {
            Point result = new Point();
            result.set(a.mValue[0] - b.mValue[0],
                    a.mValue[1] - b.mValue[1], a.mValue[2] - b.mValue[2]);
            return result;
        }
    }

    // 获取Bernstein多项式的调和矩阵(其中的常量矩阵)，或其转置
    private static GlMatrix getBernsteinMatrix(boolean transpose) {
        final float[] bernsteinConstMatrix = {
                -1, 3, -3, 1,
                3, -6, 3, 0,
                -3, 3, 0, 0,
                1, 0, 0, 0
        };

        GlMatrix m = new GlMatrix();
        System.arraycopy(bernsteinConstMatrix, 0, m.getValues(), 0, bernsteinConstMatrix.length);
        if (transpose) {
            Matrix.transposeM(m.getValues(), 0, bernsteinConstMatrix, 0);
        }
        return m;
    }

    private static GlMatrix sBezierBaseMatrix = getBernsteinMatrix(false);
    private static GlMatrix sBezierTransposeMatrix = getBernsteinMatrix(true);

    private static final int mRowNum = 4;
    private static final int mCulNum = 4;
    private ArrayList<GlMatrix> mlistBezierMatrix = new ArrayList<GlMatrix>();
    private GlMatrix mTempMatrix = new GlMatrix();
    private ArrayList<Point> mControlPoints = new ArrayList<Point>();
    private boolean mDirty = true;
    public CubicBezier() {
        init();
    }

    private void init() {
        mlistBezierMatrix.clear();
        for (int i = 0; i < 3; i++) {
            mlistBezierMatrix.add(new GlMatrix());
        }

        mControlPoints.clear();
        int count = mRowNum * mCulNum;
        for (int i = 0; i < count; i++) {
            mControlPoints.add(new Point());
        }
    }

    public void makeDirty() {
        mDirty = true;
    }

    public void update(boolean fource) {
        if (fource || mDirty) {
            mergeToAxisMatrix(XAxis);
            mergeToAxisMatrix(YAxis);
            mergeToAxisMatrix(ZAxis);

            mDirty = false;
        }
    }

    /**
     * 获取特征多面体的控制点
     * @param rowIndex 行索引 从下往上
     * @param culIndex 列索引，从左往右
     * @return 控制点
     */
    public Point get(int rowIndex, int culIndex) {
        if (rowIndex >= mRowNum || culIndex >= mCulNum) {
            throw new RuntimeException("invalid index for BezierPoint!");
        }

        return mControlPoints.get(rowIndex * mRowNum + culIndex);
    }

    /**
     * 获取平滑曲面的插值顶点
     * 精度由uv坐标的细分数目决定，如果数目较多，应转移到顶点着色器中进行运算
     * @param u u参数取值，范围[0,1] 方向向右
     * @param v v参数取值，范围[0,1] 方向向上
     * @return 插值顶点
     */
    public Point smoothPoint(float u, float v) {
        float[] vecU = {u * u * u, u * u, u, 1};
        float[] vecV = {v * v * v, v * v, v, 1};

        return new Point(calcForAxis(vecU, getXMatrix(), vecV),
                calcForAxis(vecU, getYMatrix(), vecV), calcForAxis(vecU, getZMatrix(), vecV));
    }

    /**
     * 计算法向量
     * @param u u参数取值，范围[0,1] 方向向右
     * @param v v参数取值，范围[0,1] 方向向上
     * @return 法向量，已经规格化
     */
    public Point calcNormal(float u, float v) {
        float[] vecU = {u * u * u, u * u, u, 1};
        float[] vecV = {v * v * v, v * v, v, 1};
        float[] vecDU = {3.0f * u * u, 2.0f * u, 1.0f, 0};
        float[] vecDV = {3.0f * v * v, 2.0f * v, 1.0f, 0};

        Point dpdu = new Point(calcForAxis(vecDU, getXMatrix(), vecV),
                calcForAxis(vecDU, getYMatrix(), vecV), calcForAxis(vecDU, getZMatrix(), vecV));
        Point dpdv = new Point(calcForAxis(vecU, getXMatrix(), vecDV),
                calcForAxis(vecU, getYMatrix(), vecDV), calcForAxis(vecU, getZMatrix(), vecDV));
        return Point.cross(dpdu, dpdv).normalize();
    }

    private float calcForAxis(float[] U, float[] matrix, float[] V) {
        // G = U(M*V)，右边先算
        float[] rightPart = new float[4];
        Matrix.multiplyMV(rightPart, 0, matrix, 0, V, -0);
        // 两向量点积
        return U[0] * rightPart[0] + U[1] * rightPart[1] + U[2] * rightPart[2] + U[3] * rightPart[3];
    }

    private void mergeToAxisMatrix(int axisIndex) {
        float[] value = mTempMatrix.getValues();
        for (int i = 0; i < mControlPoints.size(); i++) {
            value[i] = mControlPoints.get(i).get(axisIndex);
        }

        GlMatrix m = mlistBezierMatrix.get(axisIndex);
        m.setMatrix(sBezierBaseMatrix);
        m.preConcat(mTempMatrix);
        m.preConcat(sBezierTransposeMatrix);
    }

    private GlMatrix getMatrix(int axisIndex) {
        update(false);
        return mlistBezierMatrix.get(axisIndex);
    }

    public float[] getXMatrix() {
        return getMatrix(XAxis).getValues();
    }

    public float[] getYMatrix() {
        return getMatrix(YAxis).getValues();
    }

    public float[] getZMatrix() {
        return getMatrix(ZAxis).getValues();
    }
}
