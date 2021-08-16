package dalcoms.ex.box2dex;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Date;

public class GameConfiguration {
    private static final GameConfiguration instance = new GameConfiguration();

    Preferences preferences = Gdx.app.getPreferences("hs.app.skinvibrator.preference");
    private final String prefKey_Date = "date";


    private final float viewportWidth = 1080f;
    private final float viewportHeight = 1920f;
    private final float REF_HperW = 1.64f;
    private float HperW;

    private int afterInterstitialAdCount = 0; //Clear to 0 as InterAd popup


    static GameConfiguration getInstance() {
        return instance;
    }

    private GameConfiguration() {
    }

    public float getViewportWidth() {
        return this.viewportWidth;
    }

    public float getViewportHeight() {
        float height;
        if (getHperW() < this.REF_HperW) {
            height = viewportHeight;
        } else {
            height = Gdx.graphics.getHeight() / (Gdx.graphics.getWidth() / viewportWidth);
        }
        return height;
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

    public float getHperW() {
        return HperW;
    }

    public void setHperW(float hperW) {
        HperW = hperW;
    }

}
