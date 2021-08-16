package dalcoms.ex.box2dex;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import dalcoms.lib.libgdx.GameTimer;
import dalcoms.lib.libgdx.Renderable;
import dalcoms.lib.libgdx.SpriteGameObject;
import dalcoms.lib.libgdx.VariationPerTime;

class SplashScreen implements Screen, GameTimer.EventListener {
    final String tag = "SplashScreen";
    final Box2dExGame game;
    OrthographicCamera camera;
    Viewport viewport;
    GameTimer gameTimer;
    private Array<Renderable> renderables;

    private float bgColorR = 0f, bgColorG = 0f, bgColorB = 0f, bgColorA = 1f;
    private boolean screenIsDone = false, nextScreen = false;
    int timeOfScreenDone250msec = 0;
    Array<SpriteGameObject> sgoProgressRects;
    float rectRefY;

    public SplashScreen(final Box2dExGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, game.getGameConfiguration().getViewportWidth(),
                          game.getGameConfiguration().getViewportHeight());
        this.viewport = new FitViewport(game.getGameConfiguration().getViewportWidth(),
                                        game.getGameConfiguration().getViewportHeight(),
                                        camera);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Gdx.input.setCatchKey(Input.Keys.HOME, true);
        loadBgColor();
    }

    @Override
    public void show() {
        renderables = new Array<>();
        setGameTimer();
        initGameObjects();
    }

    @Override
    public void render(float delta) {
        draw(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

    private float getFloatColor255(float colorInt) {
        return colorInt / 255f;
    }

    private void loadBgColor() {
        bgColorR = getFloatColor255(31f);
        bgColorG = getFloatColor255(32f);
        bgColorB = getFloatColor255(37f);
        bgColorA = getFloatColor255(255f);
    }

    private void draw(float delta) {
        ScreenUtils.clear(bgColorR, bgColorG, bgColorB, bgColorA);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        for (Renderable renderable : renderables) {
            renderable.render(delta);
        }

        game.getSpriteBatch().end();
    }

    private void initGameObjects() {
        sgoProgressRects = new Array<>();
        final float countX = 10f;
        final float rectWidth = 18f;
        float marginX, offsetX;
        rectRefY = game.getLocationYFromTop(900f);
        final float rectUpDownT = 0.2f;

        final SpriteGameObject sgoTitleText =
                new SpriteGameObject(game.getAssetManager().get("img/txt_dalcoms.png",
                                                                Texture.class), 0,
                                     game.getLocationYFromTop(811f))
                        .setSpriteBatch(game.getSpriteBatch());
        sgoTitleText.setCenterLocationX(game.getGameConfiguration().getViewportWidth() / 2f);
        sgoTitleText.setScale(4f);
        sgoTitleText.scale(1.2f, 1.2f, 0.4f);
        sgoTitleText.setEventListenerScaleX(new VariationPerTime.EventListener() {
            @Override
            public void onUpdate(float v, float v1) {

            }

            @Override
            public void onStart(float v) {

            }

            @Override
            public void onFinish(float v, float v1) {
                if (v1 > 1.2f) {
                    sgoTitleText.scale(1.2f, 1.2f, 1f);
                } else {
                    sgoTitleText.scale(1.3f, 1.3f, 1f);
                }
            }
        });

        marginX = sgoTitleText.getLocationX();
        offsetX = ((game.getViewportWidth() - marginX * 2) - rectWidth * countX) / (countX - 1);
        for (int i = 0; i < countX; i++) {
            SpriteGameObject sgoRect =
                    new SpriteGameObject(game.getAssetManager().get("img/rect_18x18.png",
                                                                    Texture.class),
                                         marginX + (offsetX + rectWidth) * i,
                                         rectRefY - (i % 2) * 20f)
                            .setSpriteBatch(game.getSpriteBatch());
            sgoRect.setColor(getProgressRectColor(i));
            sgoProgressRects.add(sgoRect);
        }

        for (final SpriteGameObject sgo : sgoProgressRects) {

            if (sgo.getLocationY() < game.getLocationYFromTop(900f)) {
                sgo.moveY(rectRefY, rectUpDownT);
            } else {
                sgo.moveY(rectRefY - 20f, rectUpDownT);
            }
            sgo.setEventListenerMoveY(new VariationPerTime.EventListener() {
                @Override
                public void onUpdate(float v, float v1) {

                }

                @Override
                public void onStart(float v) {

                }

                @Override
                public void onFinish(float v, float v1) {
                    if (v1 < rectRefY) {
                        sgo.moveY(rectRefY, rectUpDownT);
                    } else {
                        sgo.moveY(rectRefY - 20f, rectUpDownT);
                    }
                }
            });

            renderables.add(sgo);
        }

        renderables.add(sgoTitleText);
    }

    Color getProgressRectColor(int num) {
        final int[] colors = {
                0x05f9f9ff, 0x40b2b2ff, 0x658686ff, 0x944e4eff, 0xbb1e1eff,
                0xd81600ff, 0xe04800ff, 0xe87700ff, 0xf2b000ff, 0xfefc00ff};
        return new Color(colors[num]);
    }

    @Override
    public void onTimer1sec(float v, int i) {

    }

    @Override
    public void onTimer500msec(float v, int i) {

    }

    @Override
    public void onTimer250msec(float v, int i) {
        final int countDone = 6;
        Gdx.app.log(tag, "asset : " + gameTimer.getCurTimeSec() + "sec : " +
                         game.getAssetManager().getProgress() * 100 + "%");

        if (++timeOfScreenDone250msec == countDone) {
            screenIsDone = true;
            timeOfScreenDone250msec = countDone + 1;

        }
        if (screenIsDone && !nextScreen) {
            if (game.getAssetManager().isFinished()) {
                nextScreen = true;
                gameTimer.pause();
                Gdx.app.log(tag, "goto home : " + gameTimer.getCurTimeSec() + "sec");
                gotoNextScreen();
            }
        }

    }

    private void gotoNextScreen() {
        game.setScreen(new Box2dSimulatorScreen(game));
    }

    private void setGameTimer() {
        gameTimer = new GameTimer().start();
        gameTimer.setEventListener(this);
        renderables.add(gameTimer);
    }
}
