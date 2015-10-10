package cn.wps.show.moffice.drawing.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cn.wps.base.BuildConfig;
import cn.wps.core.runtime.Platform;
import cn.wps.graphics.RectF;
import cn.wps.show.graphics.EffectTool;
import cn.wps.show.moffice.drawing.color.ColorTranslator;
import cn.wps.show.moffice.drawing.effects.Shadow;
import cn.wps.show.moffice.drawing.effects.ShadowDml;
import cn.wps.show.moffice.drawing.effects.ShadowPos;
import cn.wps.show.moffice.drawing.graphics.FillBase;
import cn.wps.show.moffice.drawing.graphics.GraphicsCanvas;
import cn.wps.show.moffice.drawing.graphics.IGraphics;
import cn.wps.show.moffice.drawing.graphics.RenderModel;
import cn.wps.show.moffice.drawing.util.AnyObjPool;
import cn.wps.show.moffice.drawing.util.ShapeBitmapPool;
import cn.wps.show.moffice.util.CanvasUtil;

/**
 * 带阴影效果的对象渲染实现
 */
public class ShadowRender {
	private static final String TAG = BuildConfig.DEBUG ? ShadowRender.class.getSimpleName() : null;
	private static PaintFlagsDrawFilter mDrawFilterFlag = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG);

	private ShapeEnv mEnv;
	private GraphicsCanvas mGc;
	private RenderModel mShape;
	private RectF mShapeRect;
	private float mRotation = 0;

	private Bitmap mShapeBitmap;
	private Canvas mOldCanvas;
	private Canvas mShapeCanvas;

	private android.graphics.Rect mSrcRct;
	private android.graphics.RectF mDstRct;

	private int mShaowColor = 0;
	private float mSx = 1;
	private float mSy = 1;
	private boolean mDiffScaled = false; // 缓存缩放是否和原设备Canvas不同缩放比

	private float[] mCanvasScales = new float[2];
	private Paint mPaint = new Paint();
	private AnyObjPool objPool = AnyObjPool.getPool();

	// 重用GemoRender里面一些基础实现
	public static interface IBaseDrawing {
		public void drawFill(IGraphics gc, RenderModel shape, RectF rectF, float rotation);
		public void drawStroke(IGraphics gc, RenderModel shape, RectF rectF, float rotation);
	}

	public static boolean hasShadow(RenderModel shape) {
		return (hasInnerShadow(shape) || hasOuterShadow(shape));
	}

	private static boolean hasFill(RenderModel shape){
		if (shape.getPicture() != null)
			return true;

		FillBase fill = shape.getFill();
		return (fill != null && fill.getFillOn());
	}

	private static boolean hasInnerShadow(RenderModel shape) {
		if (shape.getShadow() != null && shape.getShadow().isDml()) {
			ShadowDml shadowDml = (ShadowDml)shape.getShadow();
			ShadowPos pos = shadowDml.getShadowPos();
			return  (pos == ShadowPos.Inner && hasFill(shape));
		}

		return false;
	}

	private static boolean hasOuterShadow(RenderModel shape) {
		if (shape.getShadow() != null && shape.getShadow().isDml()) {
			ShadowDml shadowDml = (ShadowDml)shape.getShadow();
			ShadowPos pos = shadowDml.getShadowPos();
			return (pos == ShadowPos.Outer);
		}
		return false;
	}

	public ShadowRender(ShapeEnv env, IGraphics gc, RenderModel shape, RectF rectF, float rotation) {
		mEnv = env;
		mGc = (GraphicsCanvas)gc;
		mShape = shape;
		mShapeRect = rectF;
		mRotation = rotation;
	}

	public boolean draw(IBaseDrawing baseDrawing) {
		boolean bOk = false;
		FenDuanLog log = new FenDuanLog(String.format("ShadowRender.draw time"));
		try {
			if (onBegin()) {
				log.addPart("onBegin");
				onDraw(baseDrawing);
				log.addPart("onDraw");
				bOk = true;
			}
		} catch (Exception e) {
			// 错误情况下，返回仍继续走完基本流程
			e.printStackTrace();
		} finally {
			onEnd();
			log.end();
		}

		return bOk;
	}

	private void onDraw(IBaseDrawing baseDrawing) {
		FenDuanLog log = new FenDuanLog(String.format("ShadowRender.onDraw time diffScale " + mDiffScaled));

		// 1、对象填充，mGc指向mShapeCanvas了，即作用于mShapeBitmap
		baseDrawing.drawFill(mGc, mShape, mShapeRect, mRotation);
		log.addPart("drawFill");

		// 2.叠加内阴影效果，会修改mShapeBitmap
		overLayInnerShadowTo(mShapeBitmap);
		log.addPart("overLayInnerShadowTo");

		// 3.stroke在内阴影之上，对于有缩放的内阴影，stroke不画到中间层
		boolean stroked = false;
		if (hasOuterShadow(mShape) || !mDiffScaled) {
			baseDrawing.drawStroke(mGc, mShape, mShapeRect, mRotation);
			stroked = true;
			log.addPart("drawStroke befor");
		}


		// 4.外阴影不必缓存，直接作用于原Canvas
		drawOuterShadow(mOldCanvas, mShapeBitmap);
		log.addPart("drawOuterShadow");

		// 5.对象本身画在外阴影上面
		mOldCanvas.setDrawFilter(mDrawFilterFlag);
		mOldCanvas.drawBitmap(mShapeBitmap, mSrcRct, mDstRct, null);
		log.addPart("drawBitmap");

		if (!stroked) {
			mGc.setCanvas(mOldCanvas);
			baseDrawing.drawStroke(mGc, mShape, mShapeRect, mRotation);
			stroked = true;
			log.addPart("drawStroke after");
		}
		log.end();
	}

	private boolean onBegin()
	{
		if (mShape.getShadow() == null)
			return false;

		RectF effectRect = EffectTool.getEffectRectNoShadow(mShape, mShapeRect);
		// 内阴影要反色运算，外阴影会溢出对象区域，因此做区域扩充
		float radius = getBlurRadius(mShape.getShadow());
		if (radius > 0) {
			radius *= 1.414f;
			effectRect.set(effectRect.left - radius, effectRect.top - radius,
					effectRect.right + radius, effectRect.bottom + radius);
		}
		mDstRct = objPool.getRectF(effectRect.left, effectRect.top, effectRect.right, effectRect.bottom);

		mOldCanvas = (Canvas)mGc.getCanvas();
		CanvasUtil.calScaleXy(mOldCanvas, mCanvasScales);
		adjustScale(Math.abs(mCanvasScales[0]), Math.abs(mCanvasScales[1]), mDstRct);

		int width = (int) (effectRect.width() * mSx);
		int height = (int) (effectRect.height() * mSy);
		mShapeBitmap = ShapeBitmapPool.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		if (mShapeBitmap == null)
			return false;

		mSrcRct = objPool.getRect(0, 0, width, height);

		mShapeCanvas = new Canvas(mShapeBitmap);
		mShapeCanvas.drawColor(0x00FFFFFF, PorterDuff.Mode.SRC);
		mShapeCanvas.scale(mSx, mSy);
		mShapeCanvas.translate(-mDstRct.left, -mDstRct.top);
		mGc.setCanvas(mShapeCanvas);

		int color = mShape.getShadow().getShadowColor();
		ColorTranslator colorTranslator = new ColorTranslator(mShape);
		int alpha = (int)(0xFF * mShape.getShadow().getShadowOpacity());
		mShaowColor = colorTranslator.translateColor(color, alpha);
		return true;
	}

	/**
	 * 调整缩放比率避免缓存太大
	 * @param sx
	 * @param sy
	 */
	private void adjustScale(float sx, float sy, android.graphics.RectF dstRct) {
		final float max_acreage = 1200f * 800f;
		mSx = sx;
		mSy = sy;
		if (dstRct.width() * mSx * dstRct.height() * mSy > max_acreage) {
			mSx = (float)Math.sqrt(max_acreage * sx / (1.0f * dstRct.width() * dstRct.height() * sy));
			mSy = mSx * sy / sx;
			mDiffScaled = true;
		}
	}

	private void onEnd() {
		if (mOldCanvas !=null)
			mGc.setCanvas(mOldCanvas);
		clear();
	}

	private void clear() {
		ShapeBitmapPool.reuseBitmap(mShapeBitmap);
		mShapeBitmap =  null;
		objPool.tryReuse(mSrcRct);
		mSrcRct = null;
		objPool.tryReuse(mDstRct);
		mDstRct = null;
		mOldCanvas = null;
		mShapeCanvas = null;

		mEnv = null;
		mGc = null;
		mShape = null;
		mShapeRect = null;
		mRotation = 0;
		mShaowColor = 0;
		mSx = 1;
		mSy = 1;
		mDiffScaled = false;
	}

	private void drawOuterShadow(Canvas canvas, Bitmap srcBitmap) {
		if (!hasOuterShadow(mShape))
			return;

		FenDuanLog log = new FenDuanLog(String.format("ShadowRender.drawOuterShadow(%d*%d) time", mSrcRct.width(), mSrcRct.height()));

		canvas.save();
		float[] values = EffectTool.getShadowDmlPerspectiveMatrix((ShadowDml)mShape.getShadow(),
				mShape, mShapeRect, mEnv.getRotate());
		Matrix matrix = objPool.getMatrix();
		matrix.setValues(values);
		canvas.concat(matrix);
		objPool.tryReuse(matrix);

		Paint paint = new Paint();
		paint.setColor(mShaowColor);
		float radius = getBlurRadius(mShape.getShadow());
		if (radius > 0) {
			BlurMaskFilter blur = new BlurMaskFilter(radius / 2 * mSx, BlurMaskFilter.Blur.NORMAL);
			paint.setMaskFilter(blur);
		}

		int[] offsetXY = new int[2];
		Bitmap shadowBitmap = srcBitmap.extractAlpha(paint, offsetXY);
		log.addPart("extractAlpha");

		canvas.translate(offsetXY[0] / mSx, offsetXY[1] / mSy);
		canvas.drawBitmap(shadowBitmap, mSrcRct, mDstRct, paint);
		log.addPart("drawBitmap");
		canvas.restore();
		shadowBitmap.recycle(); // not reuse becouse not isMutable
		log.end();
	}

	private void overLayInnerShadowTo(Bitmap srcBitmap) {
		if (!hasInnerShadow(mShape))
			return;

		FenDuanLog log = new FenDuanLog(String.format("ShadowRender.overLayInnerShadowTo(%d*%d) time", mSrcRct.width(), mSrcRct.height()));

		PointF offset = EffectTool.getShadowOffset((ShadowDml) mShape.getShadow(),
				mEnv.getRotate(), mShapeRect, false, true, mShape.getGRF());
		Bitmap invertBitmap = InvertBitmap(srcBitmap, mShaowColor);
		log.addPart("InvertBitmap");
		float radius = getBlurRadius(mShape.getShadow());
		if (radius > 0) {
			BlurMaskFilter blur = new BlurMaskFilter(radius / 2 * mSx, BlurMaskFilter.Blur.NORMAL);
			mPaint.setColor(mShaowColor);
			mPaint.setMaskFilter(blur);

			int[] offsetXY = new int[2];
			Bitmap alphaShadow = invertBitmap.extractAlpha(mPaint, offsetXY);
			log.addPart("extractAlpha");

			mPaint.setMaskFilter(null);	// 第二次仅为着色
			blendInnerShadow(mShapeCanvas, alphaShadow, mPaint,
					offset.x, offset.y, offsetXY[0], offsetXY[1], radius);
			log.addPart("blendInnerShadow");
			alphaShadow.recycle(); // not reuse becouse not isMutable
		} else if (offset.x != 0 || offset.y != 0) {
			mPaint.setColor(mShaowColor);
			blendInnerShadow(mShapeCanvas, invertBitmap, mPaint, offset.x, offset.y, 0, 0, radius);
			log.addPart("blendInnerShadow");
		}
		ShapeBitmapPool.reuseBitmap(invertBitmap);

		log.end();
	}

	private void blendInnerShadow(Canvas sourceCanvas, Bitmap shadowBitmap, Paint paint,
								float dx, float dy, int offsetX, int offsetY, float blurRadius) {
		Xfermode oriMode = paint.getXfermode();
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

		// 在blurRadius大于0时，shadowBitmap比源shape稍大，截取中间部分
		android.graphics.Rect srcRct = objPool.getRect(mSrcRct.left, mSrcRct.top, mSrcRct.right, mSrcRct.bottom);
		srcRct.offset(-offsetX, -offsetY);

		if (dx == 0 && dy ==0) {
			sourceCanvas.drawBitmap(shadowBitmap, srcRct, mDstRct, paint);
		} else {
			sourceCanvas.save(Canvas.CLIP_SAVE_FLAG);
			float left = mDstRct.left + (dx > 0 ? dx + blurRadius : 0);
			float top = mDstRct.top + (dy > 0 ? dy + blurRadius : 0);
			float right = mDstRct.right + (dx < 0 ? dx - blurRadius : 0);
			float bottom = mDstRct.bottom + (dy < 0 ? dy - blurRadius : 0);
			sourceCanvas.clipRect(left, top, right, bottom);

			sourceCanvas.translate(dx, dy);
			sourceCanvas.drawBitmap(shadowBitmap, srcRct, mDstRct, paint);
			sourceCanvas.translate(-dx, -dy);
			sourceCanvas.clipRect(mDstRct, Region.Op.REVERSE_DIFFERENCE);
			sourceCanvas.drawColor(mShaowColor, PorterDuff.Mode.SRC_ATOP);
			sourceCanvas.restore();
		}

		objPool.tryReuse(srcRct);
		paint.setXfermode(oriMode);
	}

	private void overlayABitmap2(Bitmap srcBitmap, Bitmap overlayBitmap) {
		Canvas sourceCanvas = new Canvas(srcBitmap);
		Paint p = new Paint();
		p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
		sourceCanvas.drawBitmap(overlayBitmap, 0, 0, p);
	}

	public static class FenDuanLog {
		private static boolean debug = true;
		private long start;
		private long last;
		private String tag;
		private Map<String, Long> list = new LinkedHashMap<String, Long>();
		public FenDuanLog(String tag) {
			start = System.currentTimeMillis();
			last = start;
			this.tag = tag;
		}

		public void addPart(String tag) {
			if (!debug)
				return;

			long cur = System.currentTimeMillis();
			list.put(tag, Long.valueOf(cur - last));
			last = cur;
		}

		public void end() {
			if (!debug)
				return;

			StringBuffer buf = new StringBuffer();
			buf.append(this.tag + " " + (System.currentTimeMillis() - start) + ":::");

			Set<String> keys = list.keySet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				String s = (String)it.next();
				buf.append(s + " " + list.get(s).longValue() + ", ");
			}
			Log.d(TAG, buf.toString());
		}
	}

	private static void log(String s) {
		Log.d(TAG, s);
	}

	/**
	 * 移动内阴影，返回移动后的内阴影位图
	 * @param shadowBitmap
	 * @param dx
	 * @param dy
	 * @return
	 */
	private Bitmap translateInnerShadow(Bitmap shadowBitmap, Paint paint,float dx, float dy,
										int shadowOffsetX, int shadowOffsetY, float blurRadius) {
		Bitmap tempBitmap = ShapeBitmapPool.createBitmap(mSrcRct.width(), mSrcRct.height(), Bitmap.Config.ARGB_8888);
		Canvas shadowCanvas = new Canvas(tempBitmap);
		shadowCanvas.drawColor(mShaowColor, PorterDuff.Mode.SRC);
		shadowCanvas.translate(dx + shadowOffsetX, dy + shadowOffsetY);

		shadowCanvas.save(Canvas.CLIP_SAVE_FLAG);
		float left = mSrcRct.left;
		float top = mSrcRct.top;
		if (dx > 0)
			left += blurRadius * 2 * mSx;
		if (dy > 0)
			top += blurRadius * 2 * mSy;
		shadowCanvas.clipRect(left, top, mSrcRct.right, mSrcRct.bottom);
		shadowCanvas.drawColor(0x00FFFFFF, PorterDuff.Mode.SRC);
		shadowCanvas.drawBitmap(shadowBitmap, 0, 0, paint);
		shadowCanvas.restore();
		return tempBitmap;
	}

	private static float getBlurRadius(Shadow shadow) {
		float radius = 0;
		if (shadow.isDml()) {
			radius = ((ShadowDml)shadow).getBlurRadius();
		}
		return  radius;
	}

	/**
	 * 透明度取反，并替换颜色
	 * @param bitmap
	 * @param color
	 * @return
	 */
	private Bitmap InvertBitmap(Bitmap bitmap, int color) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Bitmap newBitmap = ShapeBitmapPool.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		final int[] pixels = new int[w];
		for (int i = 0; i < h; i++)
		{
			bitmap.getPixels(pixels, 0, w, 0, i, w, 1);
			for (int j = 0, length = pixels.length; j < length; j++) {
				int pixel = pixels[j];
				pixels[j] = ((pixel ^ 0xFFFFFFFF) & 0xFF000000) | (color & 0xFFFFFF);
			}
			newBitmap.setPixels(pixels, 0, w, 0, i, w, 1);
		}

		return newBitmap;
	}

	private Bitmap InvertBitmap2(Bitmap bitmap, int color) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Bitmap newBitmap = ShapeBitmapPool.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(newBitmap);
		canvas.drawColor(0xFF000000);

		RenderScript rs = RenderScript.create((Context) Platform.getApplicationContext());

		ScriptIntrinsicBlend scriptIntrinsicBlend = ScriptIntrinsicBlend.create(rs, Element.U8_4(rs));

		Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
		Allocation allOut = Allocation.createFromBitmap(rs, newBitmap);

		scriptIntrinsicBlend.forEachXor(allIn, allOut);

		allOut.copyTo(newBitmap);

		rs.destroy();

		return newBitmap;
	}

	private void filterShadowFromAhpaBitmap(Bitmap alphaBitmap, Bitmap shadowBitmap) {
		int w = alphaBitmap.getWidth();
		int h = alphaBitmap.getHeight();
		final int[] pixelsAlpha = new int[w];
		final int[] pixelsShadow = new int[w];

		for (int i = 0; i < h; i++) {
			alphaBitmap.getPixels(pixelsAlpha, 0, w, 0, i, w, 1);
			shadowBitmap.getPixels(pixelsShadow, 0, w, 0, i, w, 1);

			for (int j = 0, length = pixelsAlpha.length; j < length; j++) {
				int pixelR = pixelsAlpha[j];
				int pixelS = pixelsShadow[j];
				int sourceA = (pixelR >> 24) & 0xFF;
				int shadowA = (pixelS >> 24) & 0xFF;
				int newAlpa = (int)(1.0f * sourceA * shadowA / 256);
				pixelsShadow[j] = (newAlpa << 24) | (pixelS & 0xFFFFFF);
			}

			shadowBitmap.setPixels(pixelsShadow, 0, w, 0, i, w, 1);
		}
	}

	/**
	 * RenderScript方式实现图片虚化
	 * @param bitmap
	 * @return
	 */
	public Bitmap blurBitmap(Bitmap bitmap, float radius){
		long start = System.currentTimeMillis();

		//Let's create an empty bitmap with the same size of the bitmap we want to blur
		Bitmap outBitmap = ShapeBitmapPool.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

		//Instantiate a new Renderscript
		RenderScript rs = RenderScript.create((Context) Platform.getApplicationContext());

		//Create an Intrinsic Blur Script using the Renderscript
		ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

		//Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
		Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
		Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);

		//Set the radius of the blur
		blurScript.setRadius(radius);

		//Perform the Renderscript
		blurScript.setInput(allIn);
		blurScript.forEach(allOut);

		//Copy the final bitmap created by the out Allocation to the outBitmap
		allOut.copyTo(outBitmap);

		//recycle the original bitmap
		bitmap.recycle();

		//After finishing everything, we destroy the Renderscript.
		rs.destroy();

		Log.d(TAG, "blurBitmap time " + (System.currentTimeMillis() - start) +",w " + outBitmap.getWidth() + ",h " + outBitmap.getHeight());
		return outBitmap;
	}

	private Bitmap AphlaBitmap(Bitmap bitmap) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Bitmap newBitmap = ShapeBitmapPool.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		final int[] pixels = new int[w];
		for (int i = 0; i < h; i++)
		{
			bitmap.getPixels(pixels, 0, w, 0, i, w, 1);
			for (int j = 0, length = pixels.length; j < length; j++) {
				int pixel = pixels[j];
				pixels[j] = (pixel & 0xFF) | (0xFFFFFF);
			}
			newBitmap.setPixels(pixels, 0, w, 0, i, w, 1);
		}

		return newBitmap;
	}

	private void innerShadowBlend(Bitmap sourceBitmap, Bitmap shadowBitmap) {
		int w = sourceBitmap.getWidth();
		int h = sourceBitmap.getHeight();
		final int[] pixelsResult = new int[w];
		final int[] pixelsShadow = new int[w];

		for (int i = 0; i < h; i++) {
			sourceBitmap.getPixels(pixelsResult, 0, w, 0, i, w, 1);
			shadowBitmap.getPixels(pixelsShadow, 0, w, 0, i, w, 1);

			for (int j = 0, length = pixelsResult.length; j < length; j++) {
				int pixelR = pixelsResult[j];
				int pixelS = pixelsShadow[j];

				int resultA = (pixelR >> 24) & 0xFF;
				int shadowA = (pixelS >> 24) & 0xFF;
				if (resultA == 0 || shadowA == 0) {
					pixelsShadow[j] = 0;
				} else{
					int resultB = pixelR & 0xFF;
					int resultG = (pixelR >> 8) & 0xFF;
					int resultR = (pixelR >> 16) & 0xFF;

					int shadowB = pixelS & 0xFF;
					int shadowG = (pixelS >> 8) & 0xFF;
					int shadowR = (pixelS >> 16) & 0xFF;

					resultR = (resultB << 16) + ((shadowR << 8) - resultR * resultA) * shadowA;
					resultG = (resultG << 16) + ((shadowG << 8) - resultG * resultA) * shadowA;
					resultB = (resultB << 16) + ((shadowB << 8) - resultB * resultA) * shadowA;

					int subA = 255 - resultA;
					resultR = resultR / (65535 + shadowA * subA);
					resultG = resultG / (65535 + shadowA * subA);
					resultB = resultB / (65535 + shadowA * subA);
					resultA = resultA + ((shadowA * resultA * subA) >> 16);

					pixelsShadow[j] = (resultA << 24) |  (resultR << 16) | (resultG << 8) | resultB;
				}
			}

			shadowBitmap.setPixels(pixelsShadow, 0, w, 0, i, w, 1);
		}
	}

	public static class FastBlur {

		public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {

			// Stack Blur v1.0 from
			// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
			//
			// Java Author: Mario Klingemann <mario at quasimondo.com>
			// http://incubator.quasimondo.com
			// created Feburary 29, 2004
			// Android port : Yahel Bouaziz <yahel at kayenko.com>
			// http://www.kayenko.com
			// ported april 5th, 2012

			// This is a compromise between Gaussian Blur and Box blur
			// It creates much better looking blurs than Box Blur, but is
			// 7x faster than my Gaussian Blur implementation.
			//
			// I called it Stack Blur because this describes best how this
			// filter works internally: it creates a kind of moving stack
			// of colors whilst scanning through the image. Thereby it
			// just has to add one new block of color to the right side
			// of the stack and remove the leftmost color. The remaining
			// colors on the topmost layer of the stack are either added on
			// or reduced by one, depending on if they are on the right or
			// on the left side of the stack.
			//
			// If you are using this algorithm in your code please add
			// the following line:
			//
			// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

			long start = System.currentTimeMillis();
			Bitmap bitmap;
			if (canReuseInBitmap) {
				bitmap = sentBitmap;
			} else {
				bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
			}

			if (radius < 1) {
				return (null);
			}

			int w = bitmap.getWidth();
			int h = bitmap.getHeight();

			int[] pix = new int[w * h];
			bitmap.getPixels(pix, 0, w, 0, 0, w, h);

			int wm = w - 1;
			int hm = h - 1;
			int wh = w * h;
			int div = radius + radius + 1;

			int r[] = new int[wh];
			int g[] = new int[wh];
			int b[] = new int[wh];
			int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
			int vmin[] = new int[Math.max(w, h)];

			int divsum = (div + 1) >> 1;
			divsum *= divsum;
			int dv[] = new int[256 * divsum];
			for (i = 0; i < 256 * divsum; i++) {
				dv[i] = (i / divsum);
			}

			yw = yi = 0;

			int[][] stack = new int[div][3];
			int stackpointer;
			int stackstart;
			int[] sir;
			int rbs;
			int r1 = radius + 1;
			int routsum, goutsum, boutsum;
			int rinsum, ginsum, binsum;

			for (y = 0; y < h; y++) {
				rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
				for (i = -radius; i <= radius; i++) {
					p = pix[yi + Math.min(wm, Math.max(i, 0))];
					sir = stack[i + radius];
					sir[0] = (p & 0xff0000) >> 16;
					sir[1] = (p & 0x00ff00) >> 8;
					sir[2] = (p & 0x0000ff);
					rbs = r1 - Math.abs(i);
					rsum += sir[0] * rbs;
					gsum += sir[1] * rbs;
					bsum += sir[2] * rbs;
					if (i > 0) {
						rinsum += sir[0];
						ginsum += sir[1];
						binsum += sir[2];
					} else {
						routsum += sir[0];
						goutsum += sir[1];
						boutsum += sir[2];
					}
				}
				stackpointer = radius;

				for (x = 0; x < w; x++) {

					r[yi] = dv[rsum];
					g[yi] = dv[gsum];
					b[yi] = dv[bsum];

					rsum -= routsum;
					gsum -= goutsum;
					bsum -= boutsum;

					stackstart = stackpointer - radius + div;
					sir = stack[stackstart % div];

					routsum -= sir[0];
					goutsum -= sir[1];
					boutsum -= sir[2];

					if (y == 0) {
						vmin[x] = Math.min(x + radius + 1, wm);
					}
					p = pix[yw + vmin[x]];

					sir[0] = (p & 0xff0000) >> 16;
					sir[1] = (p & 0x00ff00) >> 8;
					sir[2] = (p & 0x0000ff);

					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];

					rsum += rinsum;
					gsum += ginsum;
					bsum += binsum;

					stackpointer = (stackpointer + 1) % div;
					sir = stack[(stackpointer) % div];

					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];

					rinsum -= sir[0];
					ginsum -= sir[1];
					binsum -= sir[2];

					yi++;
				}
				yw += w;
			}
			for (x = 0; x < w; x++) {
				rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
				yp = -radius * w;
				for (i = -radius; i <= radius; i++) {
					yi = Math.max(0, yp) + x;

					sir = stack[i + radius];

					sir[0] = r[yi];
					sir[1] = g[yi];
					sir[2] = b[yi];

					rbs = r1 - Math.abs(i);

					rsum += r[yi] * rbs;
					gsum += g[yi] * rbs;
					bsum += b[yi] * rbs;

					if (i > 0) {
						rinsum += sir[0];
						ginsum += sir[1];
						binsum += sir[2];
					} else {
						routsum += sir[0];
						goutsum += sir[1];
						boutsum += sir[2];
					}

					if (i < hm) {
						yp += w;
					}
				}
				yi = x;
				stackpointer = radius;
				for (y = 0; y < h; y++) {
					// Preserve alpha channel: ( 0xff000000 & pix[yi] )
					pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

					rsum -= routsum;
					gsum -= goutsum;
					bsum -= boutsum;

					stackstart = stackpointer - radius + div;
					sir = stack[stackstart % div];

					routsum -= sir[0];
					goutsum -= sir[1];
					boutsum -= sir[2];

					if (x == 0) {
						vmin[y] = Math.min(y + r1, hm) * w;
					}
					p = x + vmin[y];

					sir[0] = r[p];
					sir[1] = g[p];
					sir[2] = b[p];

					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];

					rsum += rinsum;
					gsum += ginsum;
					bsum += binsum;

					stackpointer = (stackpointer + 1) % div;
					sir = stack[stackpointer];

					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];

					rinsum -= sir[0];
					ginsum -= sir[1];
					binsum -= sir[2];

					yi += w;
				}
			}

			bitmap.setPixels(pix, 0, w, 0, 0, w, h);

			Log.d(TAG, "fastBlurBitmap time " + (System.currentTimeMillis() - start) +",w " + bitmap.getWidth() + ",h " + bitmap.getHeight());
			return (bitmap);
		}
	}
}
