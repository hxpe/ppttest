package cn.wps.graphics.shape3d;

import android.graphics.Canvas;

public interface IShader {
	void init();
	void update();
	void render(Canvas canvas);
	void dispose();
}
