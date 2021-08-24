package dalcoms.ex.box2dex;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameConfiguration {
    private static final String tag = "GameConfiguration";
    private static final GameConfiguration instance = new GameConfiguration();

    Preferences preferences = Gdx.app.getPreferences("hs.app.skinvibrator.preference");
    private final String prefKey_Date = "date";


    private float viewportWidth = 1080f;
    private float viewportHeight = 1920f;
    private final float REF_HperW = 1.64f;
    private float HperW;

    private float physicsWorldWidth = 50f;
    private float physicsWorldHeight = 50f;
    private float physicScreenRatio = 0.0463f;

    private int afterInterstitialAdCount = 0; //Clear to 0 as InterAd popup


    static GameConfiguration getInstance() {
        return instance;
    }

    private GameConfiguration() {
    }

    public void clearAllGamePreferences() {
        preferences.clear();
        flushingPreferences();
    }


    public void flushingPreferences() {
        preferences.flush();
    }

    public int getAfterInterstitialAdCount() {
        return afterInterstitialAdCount;
    }

    public void setAfterInterstitialAdCount(int afterInterstitialAdCount) {
        this.afterInterstitialAdCount = afterInterstitialAdCount;
    }

    public float getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(float viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public float getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(float viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public void setViewportSize(float width, float height, boolean isResetH) {
        setViewportWidth(width);
        setViewportHeight(height);
        if (isResetH) {
            if (((float) Gdx.graphics.getHeight() / (float) Gdx.graphics.getWidth()) >=
                this.REF_HperW) {
                setViewportHeight(
                        Gdx.graphics.getHeight() / (Gdx.graphics.getWidth() / viewportWidth));
            }
        }
    }

    public void setPhysicsWorldSize(float worldWidth) {
        this.physicsWorldWidth = worldWidth;
        this.physicScreenRatio = this.physicsWorldHeight / getViewportWidth();
        this.physicsWorldHeight = this.physicScreenRatio * this.viewportHeight;

        Gdx.app.log(tag, "Physics world size : ratio=" + physicScreenRatio + ",width=" +
                         physicsWorldWidth + ",height=" + physicsWorldHeight);
    }

    public float getPhysicsWorldWidth() {
        return physicsWorldWidth;
    }

    public void setPhysicsWorldWidth(float physicsWorldWidth) {
        this.physicsWorldWidth = physicsWorldWidth;
    }

    public float getPhysicsWorldHeight() {
        return physicsWorldHeight;
    }

    public void setPhysicsWorldHeight(float physicsWorldHeight) {
        this.physicsWorldHeight = physicsWorldHeight;
    }

    public float getPhysicScreenRatio() {
        return physicScreenRatio;
    }

    public void setPhysicScreenRatio(float physicScreenRatio) {
        this.physicScreenRatio = physicScreenRatio;
    }
}
