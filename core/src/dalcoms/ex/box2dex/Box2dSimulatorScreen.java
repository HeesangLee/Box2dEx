package dalcoms.ex.box2dex;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import dalcoms.lib.libgdx.GameGestureListener;
import dalcoms.lib.libgdx.GameTimer;
import dalcoms.lib.libgdx.IGestureInput;
import dalcoms.lib.libgdx.Point2DFloat;
import dalcoms.lib.libgdx.Point2DInt;
import dalcoms.lib.libgdx.Renderable;
import dalcoms.lib.libgdx.SpriteGameObject;
import dalcoms.lib.libgdx.SpriteSimpleButton;
import dalcoms.lib.libgdx.SpriteSimpleToggleButton;
import dalcoms.lib.libgdx.easingfunctions.EaseBounceOut;
import dalcoms.lib.libgdx.easingfunctions.EaseElasticInOut;


class Box2dSimulatorScreen implements Screen, GameTimer.EventListener {
    static final String tag = "Box2dSimulatorScreen";
    static final boolean DEBUG_PHYSICS_RENDER = true;
    final Box2dExGame game;
    OrthographicCamera camera;
    Viewport viewport;
    GameTimer gameTimer;
    private Array<Renderable> renderables;
    private Array<Body> physicsBodies;
    private Array<IGestureInput> gestureDetectables;
    private Array<IGestureInput> gestureDetectablesTop;
    private boolean gestureDetectTop = false;

    private float bgColorR = 1f, bgColorG = 1f, bgColorB = 1f, bgColorA = 1f;

    static final boolean LOG_GAME_TIMER_ON = false;

    World world;
    Box2DDebugRenderer debugRenderer;
    private float accumulator = 0;
    static final float WORLD_TIME_STEP = 1 / 60f;
    static final int WORLD_VELOCITY_ITERATIONS = 6;
    static final int WORLD_POSITION_ITERATIONS = 2;

    private float worldWith, worldHeight, physicScreenRatio;
    Array<SpriteGameObject> upperWalls;
    Array<SpriteGameObject> bottomWalls;
    SpriteSimpleToggleButton addRemoveButton;

    public Box2dSimulatorScreen(final Box2dExGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();

        setWorldSize(game.getGameConfiguration().getPhysicsWorldWidth(),
                     game.getGameConfiguration().getPhysicsWorldHeight());
        setPhysicScreenRatio(game.getGameConfiguration().getPhysicScreenRatio());

        camera.setToOrtho(false, getWorldWith(), getWorldHeight());
        this.viewport = new FitViewport(getWorldWith(), getWorldHeight(), camera);

        Gdx.app.log(tag,
                    "camera:Orthographic,viewport width=" + viewport.getWorldWidth() + ",height=" +
                    viewport.getWorldHeight());
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        loadBgColor();
    }

    @Override
    public void show() {
        renderables = new Array<>();
        physicsBodies = new Array<>();
        gestureDetectables = new Array<>();
        gestureDetectablesTop = new Array<>();

        initPhysicsWorld(0, -9.81f);

        setGameTimer();
        initGameObjects();
        setInputProcessor();
    }

    @Override
    public void render(float delta) {
        doPhysicsStep(delta);
        draw(delta);
        debugPhysicRender();
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
        world.dispose();
        debugRenderer.dispose();
    }

    private float getFloatColor255(float colorInt) {
        return colorInt / 255f;
    }

    private void loadBgColor() {
        //load color from config or set color to default.
        bgColorR = getFloatColor255(0x35);
        bgColorG = getFloatColor255(0x2c);
        bgColorB = getFloatColor255(0x41);
        bgColorA = getFloatColor255(255f);
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(bgColorR, bgColorG, bgColorB, bgColorA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();

        for (Renderable renderable : renderables) {
            renderable.render(delta);
        }

        world.getBodies(physicsBodies);
        for (Body body : physicsBodies) {
            Sprite sprite = (Sprite) body.getUserData();

            if (sprite != null) {
                Gdx.app.log(tag, "body x." + body.getPosition().x);
                sprite.setCenter(body.getPosition().x, body.getPosition().y);
                sprite.setRotation(MathUtils.radiansToDegrees * body.getAngle());
                sprite.draw(game.getSpriteBatch());
            }
        }

        game.getSpriteBatch().end();
    }


    private void initPhysicsWorld(float gravityX, float gravityY) {
        Box2D.init();
        world = new World(new Vector2(gravityX, gravityY), true);
        debugRenderer = new Box2DDebugRenderer();

    }

    private void debugPhysicRender() {
        if (DEBUG_PHYSICS_RENDER) {
            debugRenderer.render(world, camera.combined);
        }
    }

    private void doPhysicsStep(float deltaTime) {//https://github.com/libgdx/libgdx/wiki/Box2d
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        float frameTime = Math.min(deltaTime, 0.25f);
        accumulator += frameTime;
        while (accumulator >= WORLD_TIME_STEP) {
            world.step(WORLD_TIME_STEP, WORLD_VELOCITY_ITERATIONS, WORLD_POSITION_ITERATIONS);
            accumulator -= WORLD_TIME_STEP;
        }
    }

    private void initGameObjects() {
        addWalls();
        addTestBodies();
        initBrickLocationButtons(7, 9);
        initTopButtons();
    }

    Array<SpriteSimpleButton> brickLocationButtons;

    private void initBrickLocationButtons(int cntX, int cntY) {
        brickLocationButtons = new Array<>();
        for (int x = 0; x < cntX; x++) {
            for (int y = 0; y < cntY; y++) {
                SpriteSimpleButton ssbBrickLocBtn =
                        new SpriteSimpleButton(
                                game.getAssetManager().get("img/btnAddPos.png", Texture.class),
                                viewport, game.getSpriteBatch(),
                                0, 0);
                ssbBrickLocBtn.setUserData(new Point2DInt(x, y));
                ssbBrickLocBtn.setSize(toWorldSize(ssbBrickLocBtn.getSize()));
                SpriteGameObject sgoHolo = new SpriteGameObject(
                        game.getAssetManager().get("img/brickCircle.png", Texture.class),
                        0, 0);
                sgoHolo.setColor(new Color(0x00000080));
                sgoHolo.setSize(toWorldSize(sgoHolo.getSize()));
                ssbBrickLocBtn.setSgoTouchHolo(sgoHolo);
                ssbBrickLocBtn.setOnTouchEffect(SpriteSimpleButton.OnTouchEffect.HOLO);
            }
        }
    }


    private void initTopButtons() {
        initEditRunButton();
        initAddRemoveButton();
    }

    private void initEditRunButton() {
        final SpriteSimpleToggleButton editRunButton =
                new SpriteSimpleToggleButton(
                        game.getAssetManager().get("img/modeEdit.png", Texture.class),
                        game.getAssetManager().get("img/modePlay.png", Texture.class),
                        viewport, this.game.getSpriteBatch(),
                        toWorldDimension(32f),
                        toWorldDimension(game.getLocationYFromTop(207f)));
        editRunButton.setSize(toWorldSize(editRunButton.getSize()));
        editRunButton.setSpriteOriginCenter();

        editRunButton.setBtnToggleState(SpriteSimpleToggleButton.ButtonState.DEFAULT);
        editRunButton.setEventListenerTab(new GameGestureListener.TabEventListener() {
            @Override
            public void onEvent(float v, float v1, int i, int i1) {
                if (editRunButton.getBtnToggleState() ==
                    SpriteSimpleToggleButton.ButtonState.DEFAULT) {
                    Gdx.app.log(tag, "Edit/Run Button : Tab = Default");
                    if (addRemoveButton != null) {//Edit mode
                        addRemoveButton
                                .moveY(toWorldDimension(game.getLocationYFromTop(207f)), 0.5f,
                                       EaseBounceOut.getInstance());
                    }
                } else {
                    Gdx.app.log(tag, "Edit/Run Button : Tab = Toggle");
                    if (addRemoveButton != null) {//Run mode
                        addRemoveButton.moveY(toWorldDimension(game.getLocationYFromTop(0)), 0.1f);
                    }
                }
            }
        });
        renderables.add(editRunButton);
        gestureDetectables.add(editRunButton);
    }

    private void initAddRemoveButton() {
        addRemoveButton =
                new SpriteSimpleToggleButton(
                        game.getAssetManager().get("img/editAdd.png", Texture.class),
                        game.getAssetManager().get("img/editDelete.png", Texture.class),
                        viewport, this.game.getSpriteBatch(),
                        toWorldDimension(32f * 2f + 136f),
                        toWorldDimension(game.getLocationYFromTop(207f)));
        addRemoveButton.setSize(toWorldSize(addRemoveButton.getSize()));
        addRemoveButton.setSpriteOriginCenter();

        addRemoveButton.setBtnToggleState(SpriteSimpleToggleButton.ButtonState.DEFAULT);
        addRemoveButton.setEventListenerTab(new GameGestureListener.TabEventListener() {
            @Override
            public void onEvent(float v, float v1, int i, int i1) {
                if (addRemoveButton.getBtnToggleState() ==
                    SpriteSimpleToggleButton.ButtonState.DEFAULT) {
                    Gdx.app.log(tag, "Add/Remove Button : Tab = Default");
                } else {
                    Gdx.app.log(tag, "Add/Remove Button : Tab = Toggle");
                }
            }
        });
        renderables.add(addRemoveButton);
        gestureDetectables.add(addRemoveButton);
    }

    private void addWalls() {
        addUpperWall();
        addSideWalls();
        addBottomWall(true);
    }


    private void addUpperWall() {
        final int WALL_CNT = 40;
        upperWalls = new Array<>();

        for (int i = 0; i < WALL_CNT; i++) {
            SpriteGameObject wallUnit =
                    new SpriteGameObject(game.getAssetManager().get("img/rect_16x62.png",
                                                                    Texture.class),
                                         getWorldWith() / 2f +
                                         toWorldDimension(
                                                 game.getAssetManager().get("img/rect_16x62.png",
                                                                            Texture.class)
                                                     .getWidth() / 2f),
                                         toWorldDimension(game.getLocationYFromTop(340f)))
                            .setSpriteBatch(game.getSpriteBatch());
            wallUnit.setSize(toWorldSize(wallUnit.getSize()));
            wallUnit.getSprite().setOriginCenter();
            wallUnit.setColor(new Color(0xfbe5b3ff));
            upperWalls.add(wallUnit);
        }
        int i = 0;
        float gap_wallWidth =
                (getWorldWith() - upperWalls.get(0).getWidth() * WALL_CNT) / (WALL_CNT - 1)
                + upperWalls.get(0).getWidth();
        for (SpriteGameObject wallUnit : upperWalls) {
            float toX = gap_wallWidth * i++;
            wallUnit.moveX(toX, 1f, EaseBounceOut.getInstance());
            renderables.add(wallUnit);
        }
        createStaticBoxBody(new Vector2(getWorldWith() / 2, upperWalls.get(0).getCenterLocationY()),
                            getWorldWith() / 2f, upperWalls.get(0).getHeight() / 2f);
    }

    private void addSideWalls() {
        final float width = toWorldDimension(16f);
        final float height = toWorldDimension(136f * 9f + 16f * 10f);
        final float xLeft = width / 2f;
        final float xRight = getWorldWith() - width / 2f;
        final float y = upperWalls.get(0).getLocationY() -
                        toWorldDimension((136f * 9f + 16f * 10f) / 2f);

        createStaticBoxBody(new Vector2(xLeft, y), width / 2f, height / 2f);
        createStaticBoxBody(new Vector2(xRight, y), width / 2f, height / 2f);
    }

    private void addBottomWall(boolean isPhysicsBody) {
        final int WALL_CNT = 40;
        bottomWalls = new Array<>();

        for (int i = 0; i < WALL_CNT; i++) {
            SpriteGameObject wallUnit =
                    new SpriteGameObject(game.getAssetManager().get("img/rect_16x62.png",
                                                                    Texture.class),
                                         getWorldWith() / 2f +
                                         toWorldDimension(
                                                 game.getAssetManager().get("img/rect_16x62.png",
                                                                            Texture.class)
                                                     .getWidth() / 2f),
                                         toWorldDimension(game.getLocationYFromTop(
                                                 340f + 62 + 136f * 9f + 16f * 10f)))
                            .setSpriteBatch(game.getSpriteBatch());
            wallUnit.setSize(toWorldSize(wallUnit.getSize()));
            wallUnit.getSprite().setOriginCenter();
            wallUnit.setColor(new Color(isPhysicsBody ? 0xfbe5b3ff : 0x000000ff));
            bottomWalls.add(wallUnit);
        }
        int i = 0;
        float gap_wallWidth =
                (getWorldWith() - bottomWalls.get(0).getWidth() * WALL_CNT) / (WALL_CNT - 1)
                + bottomWalls.get(0).getWidth();
        for (SpriteGameObject wallUnit : bottomWalls) {
            float toX = gap_wallWidth * i++;
            wallUnit.moveX(toX, 1f, EaseBounceOut.getInstance());
            renderables.add(wallUnit);
        }
        if (isPhysicsBody) {
            createStaticBoxBody(
                    new Vector2(getWorldWith() / 2, bottomWalls.get(0).getCenterLocationY()),
                    getWorldWith() / 2f, bottomWalls.get(0).getHeight() / 2f);
        }

    }

    private void addTestBodies() {
        // First we create a body definition
        BodyDef bodyDef = new BodyDef();
// We set our body to dynamic, for something like ground which doesn't move we would set it to StaticBody
        bodyDef.type = BodyDef.BodyType.DynamicBody;
// Set our body's starting position in the world
        bodyDef.position.set(getWorldWith() / 2f, getWorldHeight() / 2f);

// Create our body in the world using our body definition
        Body body = world.createBody(bodyDef);
        Sprite sCircle =
                new Sprite(game.getAssetManager().get("img/circle_100px.png", Texture.class));
        sCircle.setPosition(body.getPosition().x, body.getPosition().y);
//        sCircle.setOrigin(getPhysicScreenRatio() * sCircle.getWidth() / 2f,
//                          getPhysicScreenRatio() * sCircle.getHeight() / 2f);
//        sCircle.setScale(getPhysicScreenRatio());
        sCircle.setSize(getPhysicScreenRatio() * sCircle.getWidth(),
                        getPhysicScreenRatio() * sCircle.getHeight());
        sCircle.setOriginCenter();
        body.setUserData(sCircle);

// Create a circle shape and set its radius to 6
        CircleShape circle = new CircleShape();
        circle.setRadius(sCircle.getWidth() / 2f);

// Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 1f; // Make it bounce a little bit

// Create our fixture and attach it to the body
        Fixture fixture = body.createFixture(fixtureDef);

// Remember to dispose of any shapes after you're done with them!
// BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();

    }

    private void createStaticBoxBody(Vector2 position, float hx, float hy) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        Body body = world.createBody(bodyDef);
        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(hx, hy);
        body.createFixture(polygonShape, 0.0f);
        polygonShape.dispose();
    }


    @Override
    public void onTimer1sec(float v, int i) {
        if (LOG_GAME_TIMER_ON) {
            Gdx.app.log(tag, "onTimer1sec : " + v + "," + i);
        }
    }

    @Override
    public void onTimer500msec(float v, int i) {
        if (LOG_GAME_TIMER_ON) {
            Gdx.app.log(tag, "onTimer500msec : " + v + "," + i);
        }
    }

    @Override
    public void onTimer250msec(float v, int i) {
        if (LOG_GAME_TIMER_ON) {
            Gdx.app.log(tag, "onTimer250msec : " + v + "," + i);
        }
    }

    private void setGameTimer() {
        gameTimer = new GameTimer().start();
        gameTimer.setEventListener(this);
        renderables.add(gameTimer);
    }

    Vector2 getNewTouchPoint(float x, float y) {
        return viewport.unproject(new Vector2(x, y));
    }

    public boolean isGestureDetectTop() {
        return gestureDetectTop;
    }

    public void setGestureDetectTop(boolean gestureDetectTop) {
        this.gestureDetectTop = gestureDetectTop;
    }

    private void setInputProcessor() {
        InputMultiplexer inputMultiplexer = new InputMultiplexer();

        inputMultiplexer.addProcessor(new GestureDetector(new GameGestureListener() {
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                Vector2 newTouchPoint = getNewTouchPoint(x, y);
                for (IGestureInput iGestureInput : isGestureDetectTop() ? gestureDetectablesTop :
                        gestureDetectables) {
                    iGestureInput.touchDown(newTouchPoint.x, newTouchPoint.y, pointer, button);
                }
                return super.touchDown(x, y, pointer, button);
            }

            @Override
            public boolean tap(float x, float y, int count, int button) {
                Vector2 newTouchPoint = getNewTouchPoint(x, y);
                for (IGestureInput iGestureInput : isGestureDetectTop() ? gestureDetectablesTop :
                        gestureDetectables) {
                    iGestureInput.tap(newTouchPoint.x, newTouchPoint.y, count, button);
                }
//                if (isGestureDetectTop()) {
//                    if (isOutsideOfSettingMenu(newTouchPoint)) {
//                        switch (getGestureTop()) {
//                            case ESK_EXIT:
//                                closeEskExitAppDlg();
//                                break;
//                            default:
//                                Gdx.app.log(tag, getGestureTop().toString());
//                                break;
//                        }
//                    }
//                }

                return super.tap(x, y, count, button);
            }

            @Override
            public boolean longPress(float x, float y) {
                Vector2 newTouchPoint = getNewTouchPoint(x, y);
                for (IGestureInput iGestureInput : isGestureDetectTop() ? gestureDetectablesTop :
                        gestureDetectables) {
                    iGestureInput.longPress(newTouchPoint.x, newTouchPoint.y);
                }
                return super.longPress(x, y);
            }

            @Override
            public boolean fling(float velocityX, float velocityY, int button) {
                return super.fling(velocityX, velocityY, button);
            }
        }));

        inputMultiplexer.addProcessor(new InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.BACK) {
                    Gdx.app.log(tag, "Input.Keys.BACK : Key down");
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.BACK) {
                    Gdx.app.log(tag, "Input.Keys.BACK : Key up");
//                    if (isGestureDetectTop()) {
//                        if (getGestureTop() == GestureTop.ESK_EXIT) {
//                            closeEskExitAppDlg();
//                        } else {
//                            Gdx.app.log(tag, "Unknown gestureTop" + getGestureTop().toString());
//                        }
//                    } else {
//                        showAskExitGame();
//                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                Gdx.app.log(tag, String.valueOf(character));
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                Vector2 newTouchPoint = getNewTouchPoint(screenX, screenY);
                for (IGestureInput iGestureInput : isGestureDetectTop() ? gestureDetectablesTop :
                        gestureDetectables) {
                    iGestureInput
                            .touchUp((int) newTouchPoint.x, (int) newTouchPoint.y, pointer, button);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                Vector2 newTouchPoint = getNewTouchPoint(screenX, screenY);
                for (IGestureInput iGestureInput : isGestureDetectTop() ? gestureDetectablesTop :
                        gestureDetectables) {
                    iGestureInput
                            .touchDragged((int) newTouchPoint.x, (int) newTouchPoint.y, pointer);
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                return false;
            }

        });

        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    public float getWorldWith() {
        return worldWith;
    }

    public void setWorldWith(float worldWith) {
        this.worldWith = worldWith;
    }

    public float getWorldHeight() {
        return worldHeight;
    }

    public void setWorldHeight(float worldHeight) {
        this.worldHeight = worldHeight;
    }

    private void setWorldSize(float w, float h) {
        setWorldWith(w);
        setWorldHeight(h);
    }

    public float getPhysicScreenRatio() {
        return physicScreenRatio;
    }

    public void setPhysicScreenRatio(float physicScreenRatio) {
        this.physicScreenRatio = physicScreenRatio;
    }

    /**
     * @param designDimension gameDesign dimension
     * @return converted dimension to physics world
     */
    private float toWorldDimension(float designDimension) {
        return getPhysicScreenRatio() * designDimension;
    }

    private Point2DFloat toWorldSize(Point2DFloat objSize) {
        return new Point2DFloat(objSize.getX() * getPhysicScreenRatio(),
                                objSize.getY() * getPhysicScreenRatio());
    }
}
