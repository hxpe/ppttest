package org.example.localbrowser;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * 无状态的对象重用缓存，每个线程各自一个pool，仅为解决同一线程内对象重用(pool本身不线程安全)
 */
public class AnyObjPool {
    private static int sDefCapacity = 16;

    private static ThreadLocal<AnyObjPool> sThreadMap = new ThreadLocal<AnyObjPool>();

    /**
     * 获取当前线程的重用对象pool，建议减少调用次数，外部保留引用
     * @return
     */
    public static AnyObjPool getPool() {
        AnyObjPool value = sThreadMap.get();
        if (value == null) {
            value = new AnyObjPool();
            sThreadMap.set(value);
        }

        return value;
    }

    public android.graphics.Rect getRect() {
        android.graphics.Rect rc = allocateOrNew(android.graphics.Rect.class);
        rc.set(0, 0, 0, 0);
        return rc;
    }

    public android.graphics.Rect getRect(int left, int top, int right, int bottom) {
        android.graphics.Rect rc = allocateOrNew(android.graphics.Rect.class);
        rc.set(left, top, right, bottom);
        return rc;
    }

    public android.graphics.RectF getRectF() {
        android.graphics.RectF rcf = allocateOrNew(android.graphics.RectF.class);
        rcf.set(0, 0, 0, 0);
        return rcf;
    }

    public android.graphics.RectF getRectF(float left, float top, float right, float bottom) {
        android.graphics.RectF rcf = allocateOrNew(android.graphics.RectF.class);
        rcf.set(left, top, right, bottom);
        return rcf;
    }

    public android.graphics.PointF getPoinF() {
        android.graphics.PointF ptf = allocateOrNew(android.graphics.PointF.class);
        ptf.set(0, 0);
        return ptf;
    }

    public android.graphics.PointF getPoinF(float x, float y) {
        android.graphics.PointF ptf = allocateOrNew(android.graphics.PointF.class);
        ptf.set(x, y);
        return ptf;
    }

    public android.graphics.Path getPath() {
        android.graphics.Path path = allocateOrNew(android.graphics.Path.class);
        path.reset();
        return path;
    }

    public android.graphics.Matrix getMatrix() {
        android.graphics.Matrix m = allocateOrNew(android.graphics.Matrix.class);
        m.reset();
        return m;
    }

    private Map<Class, Stack> sPoolMap = new HashMap<Class, Stack>();

    public <T> T allocateOrNew(Class<T> tClass) {
        Stack pool = sPoolMap.get(tClass);
        T val = null;
        if (pool != null && pool.size() > 0) {
            val = (T) pool.pop();
        }
        if (val == null) {
            val = tryAllocate(tClass);
        }
        return val;
    }

    public <T> boolean tryReuse(T val) {
        if (val != null) {
            Class cls = val.getClass();
            Stack  pool = sPoolMap.get(cls);
            if (pool == null) {
                pool = new Stack();
                sPoolMap.put(cls, pool);
            }
            if (pool.size() < sDefCapacity) {
//				// 不显式（不鼓励）做排重判断，debug阶段可以暂时放开检查调用层哪里重复回收了
//				for (int i = 0; i < pool.size(); ++i) {
//					if (pool.get(i) == val) {
//						return false; // 断点处
//					}
//				}
                pool.push(val);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        sPoolMap.clear();
    }

    private <T> T tryAllocate(Class<T> tClass) {
        T val;
        try {
            val = tClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("InstantiationException", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("IllegalAccessException", e);
        }
        return val;
    }
}