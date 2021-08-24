package dalcoms.ex.box2dex;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Box2dExGame extends Game {
    final String tag = "Box2dExGame";
    private SpriteBatch batch;
    private GameConfiguration gameConfiguration;
    private AssetManager assetManager;
    private TextureLoader.TextureParameter para;
    private FPSLogger fpsLogger;

    final boolean FPS_LOGGING = false;
    final boolean REMOVE_PREFERENCES = false;

    public Box2dExGame() {
        assetManager = new AssetManager();
        para = new TextureLoader.TextureParameter();
        para.minFilter = Texture.TextureFilter.Linear; // 축소필터
        para.magFilter = Texture.TextureFilter.Linear; // 확대필터
        fpsLogger = new FPSLogger();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();

        gameConfiguration = GameConfiguration.getInstance();
        gameConfiguration.setViewportSize(1080f, 1920f, true);
        gameConfiguration.setPhysicsWorldSize(50f);
        logViewPortSize();

        if (REMOVE_PREFERENCES) {
            gameConfiguration.clearAllGamePreferences();
        }

        loadAssets();

        gotoSplashScreen("img/rect_18x18.png", Texture.class);
    }

    @Override
    public void render() {
        super.render();
        if (!assetManager.update()) {
            Gdx.app.log(tag, "asset : " + assetManager.getProgress() * 100 + "%");
        }
        if (FPS_LOGGING) {
            this.fpsLogger.log();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        batch.dispose();
        assetManager.dispose();
    }

    private void logViewPortSize() {
        Gdx.app.log(tag, "Graphic width = " + (float) Gdx.graphics.getWidth() + ", Height = " +
                         (float) Gdx.graphics.getHeight());
        Gdx.app.log(tag,
                    "Viewport width = " + getViewportWidth() + ", Height = " + getViewportHeight());
    }

    private void loadAssets() {
        loadAssetsSplashScreen();
        loadAssetGameScreen();
    }

    private void loadAssetsSplashScreen() {
        assetManager.load("img/txt_dalcoms.png", Texture.class, para);
        assetManager.finishLoadingAsset("img/txt_dalcoms.png");
        assetManager.load("img/rect_18x18.png", Texture.class, para);
        assetManager.finishLoadingAsset("img/rect_18x18.png");
    }

    private void loadAssetGameScreen() {
        assetManager.load("img/circle_100px.png", Texture.class, para);
    }

    private void gotoSplashScreen(String assetTobeLoading, Class classType) {
        int i = 0;
        do {
            Gdx.app.log(tag, "wait to gotoSplash : count = " + i);
            i++;
        } while (!assetManager.isLoaded(assetTobeLoading, classType));

        setScreen(new SplashScreen(this));
        Gdx.app.log(tag, "gotoSplash");
    }

    public SpriteBatch getSpriteBatch() {
        return this.batch;
    }


    public GameConfiguration getGameConfiguration() {
        return gameConfiguration;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public float getLocationYFromTop(float lengthFromTop) {
        return getGameConfiguration().getViewportHeight() - lengthFromTop;
    }

    public float getViewportWidth() {
        return getGameConfiguration().getViewportWidth();
    }

    public float getViewportHeight() {
        return getGameConfiguration().getViewportHeight();
    }
}
