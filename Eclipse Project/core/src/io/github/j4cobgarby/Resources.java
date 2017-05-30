package io.github.j4cobgarby;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Model;

public class Resources {
	public static class Assets {
		public static AssetManager assets = new AssetManager();
		
		public static void init() {
			assets.load("ico.g3dj", Model.class);
			assets.load("suz.g3dj", Model.class);
			assets.load("basic_suz.g3dj", Model.class);
			assets.load("test.g3dj", Model.class);
			assets.finishLoading();
		}
	}
}
