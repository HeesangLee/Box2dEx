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
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
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
import dalcoms.lib.libgdx.VariationPerTime;
import dalcoms.lib.libgdx.easingfunctions.EaseBounceOut;
import dalcoms.lib.libgdx.easingfunctions.EaseCircOut;


class Box2dSimulatorScreen implements Screen, GameTimer.EventListener {
    static final String tag = "Box2dSimulatorScreen";
    static final boolean DEBUG_PHYSICS_RENDER = false;
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
    boolean onShootingMode = false;
    Vector2 initPointToShoot;
    Array<SpriteGameObject> dirTouchDots;
    Array<SpriteGameObject> dirDots;

    Array<Body> bullets;
    double shootingAngle;
    SpriteSimpleToggleButton editRunButton, addRemoveButton;
    Array<SpriteSimpleButton> brickLocationButtons;
    Array<SpriteSimpleButton> brickSelGroup;
    private int brickSelGroupSelected = 0;

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

//        initPhysicsWorld(0, -9.81f);
        initPhysicsWorld(0, 0);

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
            SpriteGameObject sgo = (SpriteGameObject) body.getUserData();

            if (sgo != null) {
//                Gdx.app.log(tag, "body x." + body.getPosition().x);
//                sprite.setCenter(body.getPosition().x, body.getPosition().y);
//                sprite.setRotation(MathUtils.radiansToDegrees * body.getAngle());
//                sprite.draw(game.getSpriteBatch());
                sgo.setCenterLocation(body.getPosition().x, body.getPosition().y);
                sgo.setRotationAngle(MathUtils.radiansToDegrees * body.getAngle());
            }
        }

        game.getSpriteBatch().end();
    }


    private void initPhysicsWorld(float gravityX, float gravityY) {
        Box2D.init();
        world = new World(new Vector2(gravityX, gravityY), true);
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture fa = contact.getFixtureA();
                Fixture fb = contact.getFixtureB();
                boolean isFaValid = false;
                boolean isFbValid = false;
                SpriteGameObject sgoFa = null, sgoFb = null;

                if (fa.getBody().getUserData() != null) {
                    sgoFa = (SpriteGameObject) fa.getBody().getUserData();
                    isFaValid = true;
                    if (fb.getBody().getUserData() != null) {
                        sgoFb = (SpriteGameObject) fb.getBody().getUserData();
                        isFbValid = true;
                    }
                }
                if (isFaValid & isFbValid) {
                    if ((sgoFa.getIndex() >= 0) && (sgoFb.getIndex() == -1)) {//brick-bullet
                        sgoFa.setIndexA(sgoFa.getIndexA() - 1);
                        Gdx.app.log(tag, "Box2d contact begin : " +
                                         BRICK.getValue(sgoFa.getIndex()).toString() + " : " +
                                         sgoFa.getIndexA());
                        if(sgoFa.getIndexA()==0){
                            renderables.removeValue(sgoFa,false);
//                            world.destroyBody(fa.getBody());

                        }
                    }else if((sgoFa.getIndex() ==-1) && (sgoFb.getIndex() >= 0)){//bullet-brick
                        sgoFb.setIndexA(sgoFb.getIndexA() - 1);
                        Gdx.app.log(tag, "Box2d contact begin : " +
                                         BRICK.getValue(sgoFb.getIndex()).toString() + " : " +
                                         sgoFb.getIndexA());
                        if(sgoFb.getIndexA()==0){
                            renderables.removeValue(sgoFb,false);
//                            world.destroyBody(fb.getBody());

                        }
                    }
                }
            }

            @Override
            public void endContact(Contact contact) {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {

            }
        });
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
//        addTestBodies();
        initBrickLocationButtons(7, 9);
        initTopMenu();
        initBottomMenu();
        initDirTouchDots(30);
        initDirDots(36);
        initBullets(10);
    }

    private void initBullets(int count) {
        bullets = new Array<>();

        float radiusBullet = toWorldDimension(52) / 2f;
        float yCenter =
                (bottomWalls.get(0).getLocationY() + bottomWalls.get(0).getHeight()) + radiusBullet;
        for (int i = 0; i < count; i++) {
            createCircleBullet(
                    new Vector2(getWorldWith() / 2f + 0 * radiusBullet * 2f * i, yCenter),
                    radiusBullet,
                    "img/circle_52px.png", new Color(0xfbe5b3ff));
        }
    }

    private void showBrickLocButtons() {
        final float gap = toWorldDimension(16f);
        final float wh = toWorldDimension(136f);
        final float headerY = toWorldDimension(game.getLocationYFromTop(340f));
        final float movingT = 0.5f;
        float locX, locY;

        for (SpriteSimpleButton ssb : brickLocationButtons) {
            Point2DInt ref2d = (Point2DInt) ssb.getUserData();
            locX = (wh + gap) * (float) ref2d.getX() + gap;
            locY = headerY - (wh + gap) * (float) (9 - ref2d.getY());
            ssb.move(locX, locY, movingT, EaseCircOut.getInstance());
        }
    }

    private void hideBrickLocButtons() {
        for (SpriteSimpleButton ssb : brickLocationButtons) {
            ssb.move(getWorldWith() / 2f - toWorldDimension(136f / 2f),
                     getWorldHeight() * 1.1f,
                     0.1f);
        }
    }

    private void onTabBrickLocButton(Point2DInt refPos) {
        boolean isModeEdit = editRunButton.getBtnToggleState() ==
                             SpriteSimpleToggleButton.ButtonState.DEFAULT;
        boolean isAddRemove = addRemoveButton.getBtnToggleState() ==
                              SpriteSimpleToggleButton.ButtonState.DEFAULT;

        if (isAddRemove) {//add
//            createBoxBrick(getCenterPosByRef(refPos));
            createBrick(refPos, BRICK.getValue(getBrickSelGroupSelected()));
        } else {//remove

        }
    }

    private void initBrickLocationButtons(int cntX, int cntY) {
        brickLocationButtons = new Array<>();
        for (int x = 0; x < cntX; x++) {
            for (int y = 0; y < cntY; y++) {
                final SpriteSimpleButton ssbBrickLocBtn =
                        new SpriteSimpleButton(
                                game.getAssetManager().get("img/btnAddPos.png", Texture.class),
                                viewport, game.getSpriteBatch(),
                                0, 0);
                ssbBrickLocBtn.setUserData(new Point2DInt(x, y));
                ssbBrickLocBtn.setSize(toWorldSize(ssbBrickLocBtn.getSize()));
                SpriteGameObject sgoHolo = new SpriteGameObject(
                        game.getAssetManager().get("img/brickCircle.png", Texture.class),
                        0, 0);
                sgoHolo.setColor(new Color(0xffffff80));
                sgoHolo.setSize(toWorldSize(sgoHolo.getSize()));
                sgoHolo.setSpriteOriginCenter();
                ssbBrickLocBtn.setSgoTouchHolo(sgoHolo);
                ssbBrickLocBtn.setOnTouchEffect(SpriteSimpleButton.OnTouchEffect.HOLO);

                ssbBrickLocBtn.setEventListenerTab(new GameGestureListener.TabEventListener() {
                    @Override
                    public void onEvent(float v, float v1, int i, int i1) {
                        onTabBrickLocButton((Point2DInt) ssbBrickLocBtn.getUserData());
                    }
                });

                brickLocationButtons.add(ssbBrickLocBtn);
                renderables.add(ssbBrickLocBtn);
                gestureDetectables.add(ssbBrickLocBtn);
            }
        }
    }


    private void onTabBrickSelBtnGroup(int btnNum) {
        Gdx.app.log(tag, "onTabBrickSelBtnGroup : " + btnNum);
        Color disColor = new Color(0xffffff80);
        Color[] enColorArr = {
                new Color(0x759f91ff), new Color(0xdb045bff), new Color(0xfa833dff),
                new Color(0xfa833dff), new Color(0xfa833dff), new Color(0xfa833dff)};

        setBrickSelGroupSelected(btnNum);

        for (SpriteSimpleButton ssb : brickSelGroup) {
            ssb.setColor(
                    ssb.getIndex() == getBrickSelGroupSelected() ?
                            enColorArr[brickSelGroupSelected] :
                            disColor);
        }
    }

    public int getBrickSelGroupSelected() {
        return brickSelGroupSelected;
    }

    public void setBrickSelGroupSelected(int brickSelGroupSelected) {
        this.brickSelGroupSelected = brickSelGroupSelected;
    }

    private void initBottomMenu() {
        brickSelGroup = new Array<>();
        String[] imgPathArr = {
                "img/brickRect.png", "img/brickCircle.png", "img/brickTri1.png",
                "img/brickTri2.png", "img/brickTri3.png", "img/brickTri4.png"};
        Color disColor = new Color(0xffffff80);
        Color[] enColorArr = {
                new Color(0x759f91ff), new Color(0xdb045bff), new Color(0xfa833dff),
                new Color(0xfa833dff), new Color(0xfa833dff), new Color(0xfa833dff)};
        final float w = toWorldDimension(136f);
        final float g = (getWorldWith() - w * imgPathArr.length) / (imgPathArr.length + 1);

        for (int i = 0; i < imgPathArr.length; i++) {
            final SpriteSimpleButton ssb = new SpriteSimpleButton(
                    game.getAssetManager().get(imgPathArr[i], Texture.class),
                    viewport, game.getSpriteBatch(),
                    g + (w + g) * i, toWorldDimension(game.getLocationYFromTop(1982f)));
            ssb.setSize(toWorldSize(ssb.getSize()));
            ssb.setSpriteOriginCenter();
            ssb.setColor(i == 0 ? enColorArr[i] : disColor);
            ssb.setIndex(i);
            brickSelGroup.add(ssb);
            renderables.add(ssb);
            gestureDetectables.add(ssb);
            ssb.setEventListenerTab(new GameGestureListener.TabEventListener() {
                @Override
                public void onEvent(float v, float v1, int i, int i1) {
                    onTabBrickSelBtnGroup(ssb.getIndex());
                }
            });
        }

    }

    private void initTopMenu() {
        initEditRunButton();
        initAddRemoveButton();
    }

    private void checkShowRefLocBrick() {
        boolean isModeEdit = editRunButton.getBtnToggleState() ==
                             SpriteSimpleToggleButton.ButtonState.DEFAULT;
        boolean isAddRemove = addRemoveButton.getBtnToggleState() ==
                              SpriteSimpleToggleButton.ButtonState.DEFAULT;
        if (isModeEdit) {
            showBrickLocButtons();
        } else {
            hideBrickLocButtons();
        }
    }

    private void initEditRunButton() {
        editRunButton =
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
                checkShowRefLocBrick();
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
        showBrickLocButtons();

        addRemoveButton.setEventListenerTab(new GameGestureListener.TabEventListener() {
            @Override
            public void onEvent(float v, float v1, int i, int i1) {
                checkShowRefLocBrick();
                if (addRemoveButton.getBtnToggleState() ==
                    SpriteSimpleToggleButton.ButtonState.DEFAULT) {
                    Gdx.app.log(tag, "Add/Remove Button : Tab = Default=Add");
                } else {
                    Gdx.app.log(tag, "Add/Remove Button : Tab = Toggle=Remove");
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

    private Body createStaticBoxBody(Vector2 position, float hx, float hy) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(position);
        Body body = world.createBody(bodyDef);
        PolygonShape polygonShape = new PolygonShape();
        polygonShape.setAsBox(hx, hy);
        body.createFixture(polygonShape, 0.0f);
        polygonShape.dispose();

        return body;
    }

    private Body createPolygonBody(BodyDef.BodyType bodyType, Vector2 pos, Vector2[] vertices,
            FixtureDef fixtureDef) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(pos);
        body = world.createBody(bodyDef);

        PolygonShape polygonShape = new PolygonShape();
        polygonShape.set(vertices);
        fixtureDef.shape = polygonShape;
        fixtureDef.density = bodyType == BodyDef.BodyType.StaticBody ? 0.0f : fixtureDef.density;
        body.createFixture(fixtureDef);

        polygonShape.dispose();

        return body;
    }

    private Body createCircleBody(BodyDef.BodyType bodyType, Vector2 pos, float radius,
            FixtureDef fixtureDef) {
        Body body;
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = bodyType;
        bodyDef.position.set(pos);
        body = world.createBody(bodyDef);

        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(radius);
        fixtureDef.shape = circleShape;
        fixtureDef.density = bodyType == BodyDef.BodyType.StaticBody ? 0.0f : fixtureDef.density;
        body.createFixture(fixtureDef);

        circleShape.dispose();

        return body;
    }

    private Vector2 getCenterPosByRef(Point2DInt refPos) {
        final float gap = toWorldDimension(16f);
        final float wh = toWorldDimension(136f);
        final float headerY = toWorldDimension(game.getLocationYFromTop(340f));
        float centLocX, centLocY;

        centLocX = (wh + gap) * (float) refPos.getX() + gap + wh / 2f;
        centLocY = headerY - (wh + gap) * (float) (9 - refPos.getY()) + wh / 2f;

        return new Vector2(centLocX, centLocY);
    }

    private void createBoxBrick(Vector2 centerPos) {
        float half = toWorldDimension(136f / 2f);
//        float brickLength = toWorldDimension(136f);
//        Body body = createStaticBoxBody(centerPos, half, half);
        Vector2[] vertices = {
                new Vector2(-half, -half), new Vector2(half, -half),
                new Vector2(half, half), new Vector2(-half, half)};
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 0f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.8f;
        Body body = createPolygonBody(BodyDef.BodyType.StaticBody, centerPos, vertices, fixtureDef);
        SpriteGameObject sgoBrick =
                new SpriteGameObject(game.getAssetManager().get("img/brickRect.png",
                                                                Texture.class), 0, 0)
                        .setSpriteBatch(game.getSpriteBatch());

        sgoBrick.setSize(toWorldSize(sgoBrick.getSize()));
        sgoBrick.setSpriteOriginCenter();
        sgoBrick.setColor(new Color(0x759f91ff));
        sgoBrick.setCenterLocation(centerPos.x, centerPos.y);
        renderables.add(sgoBrick);
        body.setUserData(sgoBrick);
    }

    private void createPolygonBrick(Vector2 centerPos, Vector2[] vertices, String texturePath,
            Color color, int brickIndex, int count) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 0f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0f;
        Body body = createPolygonBody(BodyDef.BodyType.StaticBody, centerPos, vertices, fixtureDef);

        body.setUserData(
                setBasicSpriteGameObject(centerPos, texturePath, color, brickIndex, count));
    }


    private void createCircleBullet(Vector2 centerPos, float radius, String texturePath,
            Color color) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 1f;
        fixtureDef.filter.groupIndex = -1;
        Body body = createCircleBody(BodyDef.BodyType.DynamicBody, centerPos, radius, fixtureDef);
        body.setBullet(true);

        body.setUserData(setBasicSpriteGameObject(centerPos, texturePath, color, -1, 0));
        bullets.add(body);
    }

    private void createCircleBrick(Vector2 centerPos, float radius, String texturePath,
            Color color, int brickIndex, int count) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 0f;
        fixtureDef.friction = 0.2f;
        fixtureDef.restitution = 0f;
        Body body = createCircleBody(BodyDef.BodyType.StaticBody, centerPos, radius, fixtureDef);

        body.setUserData(
                setBasicSpriteGameObject(centerPos, texturePath, color, brickIndex, count));
    }

    private SpriteGameObject setBasicSpriteGameObject(Vector2 centerPos, String texturePath,
            Color color) {
        SpriteGameObject sgo =
                new SpriteGameObject(game.getAssetManager().get(texturePath,
                                                                Texture.class), 0, 0)
                        .setSpriteBatch(game.getSpriteBatch());

        sgo.setSize(toWorldSize(sgo.getSize()));
        sgo.setSpriteOriginCenter();
        sgo.setColor(color);
        sgo.setCenterLocation(centerPos.x, centerPos.y);
        renderables.add(sgo);
        return sgo;
    }

    private SpriteGameObject setBasicSpriteGameObject(Vector2 centerPos, String texturePath,
            Color color, int brickIndex, int count) {
        SpriteGameObject sgo = setBasicSpriteGameObject(centerPos, texturePath, color);

        sgo.setIndex(brickIndex);
        sgo.setIndexA(count);
        return sgo;
    }


    private void createBrick(Point2DInt refPos, BRICK brickKind) {
        float half = toWorldDimension(136f / 2f);
        Vector2[] vertices = {
                new Vector2(-half, -half), new Vector2(half, -half),
                new Vector2(half, half), new Vector2(-half, half)};
        Vector2[] squreVertices = {vertices[0], vertices[1], vertices[2], vertices[3]};
        Vector2[] tri1Vertices = {vertices[0], vertices[1], vertices[3]};
        Vector2[] tri2Vertices = {vertices[1], vertices[2], vertices[3]};
        Vector2[] tri3Vertices = {vertices[0], vertices[2], vertices[3]};
        Vector2[] tri4Vertices = {vertices[0], vertices[1], vertices[2]};

        final int count = 50;

        switch (brickKind) {
            case BOX:
                createPolygonBrick(getCenterPosByRef(refPos), squreVertices, "img/brickRect.png",
                                   new Color(0x759f91ff), 0, count);
                break;
            case CIRCLE:
                createCircleBrick(getCenterPosByRef(refPos), half, "img/brickCircle.png",
                                  new Color(0xdb045bff), 1, count);
                break;
            case TRIANGLE1:
                createPolygonBrick(getCenterPosByRef(refPos), tri1Vertices, "img/brickTri1.png",
                                   new Color(0xfa833dff), 2, count);
                break;
            case TRIANGLE2:
                createPolygonBrick(getCenterPosByRef(refPos), tri2Vertices, "img/brickTri2.png",
                                   new Color(0xfa833dff), 3, count);
                break;
            case TRIANGLE3:
                createPolygonBrick(getCenterPosByRef(refPos), tri3Vertices, "img/brickTri3.png",
                                   new Color(0xfa833dff), 4, count);
                break;
            case TRIANGLE4:
                createPolygonBrick(getCenterPosByRef(refPos), tri4Vertices, "img/brickTri4.png",
                                   new Color(0xfa833dff), 5, count);
                break;
        }


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
                checkToShoot(TOUCH_EVENT.DOWN, newTouchPoint);
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
                checkToShoot(TOUCH_EVENT.UP, newTouchPoint);
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
                checkToShoot(TOUCH_EVENT.DRAGGED, newTouchPoint);
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


    private void initDirTouchDots(int count) {
        dirTouchDots = new Array<>();
        for (int i = 0; i < count; i++) {
            SpriteGameObject sgoDot =
                    new SpriteGameObject(game.getAssetManager().get("img/circle_8px.png",
                                                                    Texture.class), -10, -10)
                            .setSpriteBatch(game.getSpriteBatch());

            sgoDot.setSize(toWorldSize(sgoDot.getSize()));
            sgoDot.setSpriteOriginCenter();
            sgoDot.setColor(new Color(0x00ffffff));
            sgoDot.setVisible(false);
            dirTouchDots.add(sgoDot);
            renderables.add(sgoDot);
        }
    }

    private void initDirDots(int count) {
        dirDots = new Array<>();
        for (int i = 0; i < count; i++) {
            SpriteGameObject sgoDot =
                    new SpriteGameObject(game.getAssetManager().get("img/circle_8px.png",
                                                                    Texture.class), -10, -10)
                            .setSpriteBatch(game.getSpriteBatch());

            sgoDot.setSize(toWorldSize(sgoDot.getSize()));
            sgoDot.setSpriteOriginCenter();
            sgoDot.setColor(new Color(0xffcc00ff));
            sgoDot.setVisible(false);
            dirDots.add(sgoDot);
            renderables.add(sgoDot);
        }
    }

    private void fireBullet() {
        float forceScalar = 5f;
        final float forceX = forceScalar * (float) Math.cos(getShootingAngle());
        final float forceY = forceScalar * (float) Math.sin(getShootingAngle());
        Gdx.app.log(tag, "Fire ball : fx=" + forceX + ",fy=" + forceY);
        int i = 0;

        for (final Body bullet : bullets) {

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    Gdx.app.log(tag, "shooting");
                    bullet.applyLinearImpulse(new Vector2(forceX, forceY),
                                              new Vector2(bullet.getPosition().x,
                                                          bullet.getPosition().y),
                                              true);
                }
            }, 0.1f * i);
            Gdx.app.log(tag, "shooting : " + i);
            i++;
        }
    }

    private void hidedirTouchDots() {
        for (final SpriteGameObject sgoDot : dirTouchDots) {
            if (sgoDot.isVisible()) {
                sgoDot.move(dirTouchDots.get(dirTouchDots.size - 1).getLocationX(),
                            dirTouchDots.get(dirTouchDots.size - 1).getLocationY(), 0.2f);
                sgoDot.setEventListenerMoveX(new VariationPerTime.EventListener() {
                    @Override
                    public void onUpdate(float v, float v1) {

                    }

                    @Override
                    public void onStart(float v) {

                    }

                    @Override
                    public void onFinish(float v, float v1) {
                        sgoDot.setVisible(false);
                    }
                });
            }
        }
        for (SpriteGameObject sgoDot : dirDots) {
            sgoDot.setVisible(false);
        }
    }

    private void arrangeDirTouchDots(Vector2 touchPoint) {
        float len = touchPoint.dst(initPointToShoot);
        float lenGap = 0.017f * len + toWorldDimension(40);
        double ang = Math.atan2(
                (touchPoint.y - initPointToShoot.y), (touchPoint.x - initPointToShoot.x));
        setShootingAngle(ang);

        SpriteGameObject sgoDirDot;
        float lenToDot;
        float cosAng = (float) Math.cos(ang);
        float sinAng = (float) Math.sin(ang);
        int lastShowIndex = 0;

        for (int i = 0; i < dirTouchDots.size; i++) {
            sgoDirDot = dirTouchDots.get(i);
            lenToDot = lenGap * i;

            if (lenToDot < len) {
                sgoDirDot.setCenterLocation(initPointToShoot.x + lenToDot * cosAng,
                                            initPointToShoot.y + lenToDot * sinAng);
                sgoDirDot.setVisible(true);
                lastShowIndex = i;
            } else {

                sgoDirDot.setCenterLocation(dirTouchDots.get(lastShowIndex).getLocationX(),
                                            dirTouchDots.get(lastShowIndex).getLocationY());
                sgoDirDot.setVisible(false);
            }
        }

        arrangeDirDots(lenGap, cosAng, sinAng);
    }

    private void arrangeDirDots(float lenGap, float cosAng, float sinAng) {
        SpriteGameObject sgoDirDot;
        float lenToDot;
        float dotLocX, dotLocY;

        for (int i = 0; i < dirDots.size; i++) {
            lenToDot = lenGap * i;
            sgoDirDot = dirDots.get(i);

            dotLocX = bullets.get(0).getPosition().x + lenToDot * cosAng;
            dotLocY = bullets.get(0).getPosition().y + lenToDot * sinAng;

            if (dotLocY < upperWalls.get(0).getLocationY()) {
                sgoDirDot.setCenterLocation(dotLocX, dotLocY);
                sgoDirDot.setVisible(true);
            } else {
                sgoDirDot.setVisible(false);
            }

        }
    }

    private boolean isEditMode() {
        return editRunButton.getBtnToggleState() == SpriteSimpleToggleButton.ButtonState.DEFAULT;
    }

    private boolean isOnGameArea(Vector2 touchPoint) {
        float topY = upperWalls.get(0).getLocationY();
        float bottomY = bottomWalls.get(0).getLocationY() + bottomWalls.get(0).getHeight();

        return touchPoint.y < topY && touchPoint.y > bottomY;
    }

    private void checkToShoot(TOUCH_EVENT touchEvent, Vector2 touchPoint) {
        if (isEditMode()) {
            return;
        }//return
        Gdx.app.log(tag, "Touch test" + touchEvent.name() + " : x=" + touchPoint.x + ", y=" +
                         touchPoint.y);
        switch (touchEvent) {
            case DOWN://init
                if (isOnGameArea(touchPoint)) {
                    onShootingMode = true;
                    initPointToShoot = touchPoint;
                    Gdx.app.log(tag,
                                "toShoot : InitPoint(" + initPointToShoot.x + "," +
                                initPointToShoot.y +
                                ")");
                }

                break;
            case DRAGGED://calculate
                if (onShootingMode) {
                    arrangeDirTouchDots(touchPoint);
                }

                break;
            case UP://shooting

                if (onShootingMode) {
                    onShootingMode = false;
                    hidedirTouchDots();
                    fireBullet();
                }
                break;
        }
    }

    public double getShootingAngle() {
        return shootingAngle;
    }

    public void setShootingAngle(double shootingAngle) {
        this.shootingAngle = shootingAngle;
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

    private enum BRICK {
        BOX, CIRCLE, TRIANGLE1, TRIANGLE2, TRIANGLE3, TRIANGLE4;
        public static BRICK list[] = BRICK.values();

        public static BRICK getValue(int index, BRICK defBrick) {
            BRICK ret;
            if ((index < 0) || (index > list.length - 1)) {//out of index error --> return default
                Gdx.app.log(tag, "out of index, return default");
                ret = defBrick;
            } else {
                ret = list[index];
            }
            return ret;
        }

        public static BRICK getValue(int index) {
            return getValue(index, list[0]);
        }

    }

    private enum TOUCH_EVENT {
        DOWN, UP, DRAGGED;
    }
}
