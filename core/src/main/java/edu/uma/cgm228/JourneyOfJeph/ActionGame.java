package edu.uma.cgm228.JourneyOfJeph;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ActionGame extends ApplicationAdapter {
    private enum State { START, PLAYING, GAMEOVER, CREDITS }
    private State gameState = State.START;

    private SpriteBatch batch;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    private Texture heroSheet, hareSheet, npcTexture, bushTexture, rockTexture, arrowTexture, smokeSheet, solidPixel, objectTiles;
    private TextureRegion chestRegion, heartRegion, missileRegion;
    private BitmapFont uiFont, titleFont;

    private Sound chompSound, explosionSound, spinSound;
    private long spinSoundId = -1;

    private final Vector2 heroPos = new Vector2();
    private final Rectangle heroBounds = new Rectangle(0, 0, 24, 24);
    private Animation<TextureRegion> walkDown, walkUp, walkLeft, walkRight;
    private float stateTime;
    private int direction = 2;
    private boolean isMoving = false;

    private float chargeTimer = 0;
    private boolean isCharging = false;
    private boolean isSpinLunging = false;
    private float spinLungeTimer = 0;
    private Vector2 spinLungeVelocity = new Vector2();

    // Upgrade System Stats
    private int chompDamage = 1;
    private int missileDamage = 2;
    private int baseSpinDamage = 1;
    private int currentLungeDamage = 1;

    private Array<Hare> hares;
    private Animation<TextureRegion> hareAnimation;

    private Array<NPC> npcs;
    private Array<ShopItem> shopItems;
    private String activeDialogue = null;
    private boolean nearShopkeeper = false;
    private boolean nearUpgrader = false;
    private final Vector2 chestPos = new Vector2();
    private boolean chestOpen = false;
    private boolean transitioningToLevel2 = false;

    private Array<Missile> activeMissiles;
    private Array<Explosion> activeExplosions;
    private Animation<TextureRegion> explosionAnim;

    private Array<Rectangle> solids;
    private final Rectangle safeAreaRect = new Rectangle();
    private final Array<Vector2> baseHareSpawns = new Array<>();

    private int currentLevel = 1;
    private int haresSlain = 0;
    private int coins = 0;
    private int hearts = 1;
    private final int MAX_HEARTS = 3;
    private int missiles = 5;
    private String message = "";

    // Stats for Game Over Screen
    private int lifetimeHaresDefeated = 0;
    private int lifetimeCoinsCollected = 0;

    private static final float HERO_SPEED = 180f;
    private static final float HARE_SPEED = 75f;
    private static final float MISSILE_SPEED = 400f;
    private static final float SPIN_LUNGE_SPEED = 550f;
    private static final float ANIMATION_SPEED = 0.15f;
    private static final float INTERACT_RANGE = 80f;
    private static final float DETECT_RANGE = 96f; // 3 Tiles

    private final Matrix4 uiMatrix = new Matrix4();

    static class Hare {
        private enum Behavior { RANDOM, CHASE, FLEE }
        Vector2 pos;
        Rectangle bounds;
        float moveTimer;
        Vector2 velocity;
        int maxHealth, currentHealth;
        boolean isBoss = false;

        Hare(float x, float y, int hp, boolean isBoss) {
            pos = new Vector2(x, y);
            bounds = new Rectangle(x, y, isBoss ? 72 : 24, isBoss ? 72 : 24);
            velocity = new Vector2();
            this.maxHealth = this.currentHealth = hp;
            this.isBoss = isBoss;
        }

        void update(float delta, Array<Rectangle> solids, Rectangle safeArea, Vector2 heroPos) {
            float dist = pos.dst(heroPos);
            float speed = isBoss ? HARE_SPEED * 0.6f : HARE_SPEED;
            Behavior state = Behavior.RANDOM;

            // FSM Logic
            if (dist < DETECT_RANGE) {
                if (currentHealth < maxHealth / 2.0f && !isBoss) {
                    state = Behavior.FLEE;
                } else {
                    state = Behavior.CHASE;
                }
            }

            if (state == Behavior.CHASE) {
                velocity.set(heroPos).sub(pos).nor().scl(speed);
            } else if (state == Behavior.FLEE) {
                velocity.set(pos).sub(heroPos).nor().scl(speed * 1.2f);
            } else {
                moveTimer -= delta;
                if (moveTimer <= 0) {
                    float angle = MathUtils.random(0, 360) * MathUtils.degreesToRadians;
                    velocity.set(MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed);
                    moveTimer = MathUtils.random(1f, 3f);
                }
            }

            float oldX = pos.x, oldY = pos.y;
            pos.x += velocity.x * delta;
            bounds.setPosition(pos.x + 4, pos.y + 4);
            boolean hit = false;
            for (Rectangle s : solids) if (bounds.overlaps(s)) { hit = true; break; }
            if (!hit && bounds.overlaps(safeArea)) hit = true;
            if (hit) { pos.x = oldX; velocity.x *= -1; }

            pos.y += velocity.y * delta;
            bounds.setPosition(pos.x + 4, pos.y + 4);
            hit = false;
            for (Rectangle s : solids) if (bounds.overlaps(s)) { hit = true; break; }
            if (!hit && bounds.overlaps(safeArea)) hit = true;
            if (hit) { pos.y = oldY; velocity.y *= -1; }

            float limit = isBoss ? 1280 - 100 : 1216;
            if (pos.x < 32 || pos.x > limit) velocity.x *= -1;
            if (pos.y < 32 || pos.y > limit) velocity.y *= -1;
        }
    }

    static class NPC {
        Vector2 pos; String name, text; boolean active = true;
        NPC(float x, float y, String name, String text) { this.pos = new Vector2(x, y); this.name = name; this.text = text; }
    }

    static class ShopItem {
        Vector2 pos; int gid;
        ShopItem(float x, float y, int gid) { this.pos = new Vector2(x, y); this.gid = gid; }
    }

    static class Missile {
        Vector2 pos, velocity; float rotation; boolean alive = true;
        Missile(float x, float y, int direction) {
            pos = new Vector2(x, y); velocity = new Vector2();
            if (direction == 0) { velocity.set(0, MISSILE_SPEED); rotation = 90; }
            else if (direction == 1) { velocity.set(-MISSILE_SPEED, 0); rotation = 180; }
            else if (direction == 3) { velocity.set(MISSILE_SPEED, 0); rotation = 0; }
            else { velocity.set(0, -MISSILE_SPEED); rotation = 270; }
        }
        void update(float delta) { pos.add(velocity.x * delta, velocity.y * delta); if (pos.x < 0 || pos.x > 1280 || pos.y < 0 || pos.y > 1280) alive = false; }
    }

    static class Explosion { Vector2 pos; float stateTime = 0; Explosion(float x, float y) { pos = new Vector2(x, y); } }

    @Override
    public void create() {
        batch = new SpriteBatch();
        uiFont = new BitmapFont(); uiFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        uiFont.getData().setScale(1.8f);
        titleFont = new BitmapFont(); titleFont.getData().setScale(4.5f);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888); pm.setColor(Color.WHITE); pm.fill();
        solidPixel = new Texture(pm); pm.dispose();

        heroSheet = new Texture("hero.png");
        int fh = heroSheet.getHeight() / 4;
        TextureRegion[][] hTmp = TextureRegion.split(heroSheet, fh, fh);
        walkUp = createAnimation(hTmp, 0); walkLeft = createAnimation(hTmp, 1); walkDown = createAnimation(hTmp, 2); walkRight = createAnimation(hTmp, 3);

        hareSheet = new Texture("hare_hop_spritesheet.png");
        TextureRegion[][] fTmp = TextureRegion.split(hareSheet, hareSheet.getHeight(), hareSheet.getHeight());
        hareAnimation = createAnimation(fTmp, 0);

        npcTexture = new Texture("npc-1.png"); bushTexture = new Texture("bush.png"); rockTexture = new Texture("rock.png");
        arrowTexture = new Texture("arrow.png"); smokeSheet = new Texture("smoke.png");
        objectTiles = new Texture("object-tiles.png");
        chestRegion = new TextureRegion(objectTiles, 96, 0, 32, 32);
        heartRegion = new TextureRegion(objectTiles, 192, 0, 32, 32);
        missileRegion = new TextureRegion(objectTiles, 224, 0, 32, 32);

        TextureRegion[][] sTmp = TextureRegion.split(smokeSheet, smokeSheet.getWidth()/4, smokeSheet.getHeight()/2);
        TextureRegion[] eF = new TextureRegion[8]; int idx = 0;
        for (int i=0; i<2; i++) for (int j=0; j<4; j++) eF[idx++] = sTmp[i][j];
        explosionAnim = new Animation<>(0.08f, eF);

        chompSound = Gdx.audio.newSound(Gdx.files.internal("SFX/chomp.mp3"));
        explosionSound = Gdx.audio.newSound(Gdx.files.internal("SFX/explosion.mp3"));
        spinSound = Gdx.audio.newSound(Gdx.files.internal("SFX/spin_move.mp3"));

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);

        // Pre-parse original hares
        TiledMap base = new TmxMapLoader().load("map.tmx");
        MapLayer objectLayer = base.getLayers().get("Object Layer 1");
        for (MapObject obj : objectLayer.getObjects()) {
            if (obj.getProperties().containsKey("gid") && (Integer)obj.getProperties().get("gid") == 45) {
                baseHareSpawns.add(new Vector2(obj.getProperties().get("x", Float.class), obj.getProperties().get("y", Float.class)));
            }
        }
        base.dispose();

        resetLevel();
    }

    private void resetLevel() {
        if (map != null) map.dispose();
        String mapFile = (currentLevel == 1) ? "map.tmx" : "procedural.tmx";
        map = new TmxMapLoader().load(mapFile);
        mapRenderer = new OrthogonalTiledMapRenderer(map, batch);

        stateTime = 0; activeDialogue = null; nearShopkeeper = false; nearUpgrader = false;
        chestOpen = false; isCharging = false; isSpinLunging = false; transitioningToLevel2 = false;

        if (gameState == State.GAMEOVER) {
            chompDamage = 1; missileDamage = 2; baseSpinDamage = 1; hearts = 1; coins = 0; missiles = 5;
            lifetimeHaresDefeated = 0; lifetimeCoinsCollected = 0;
        }

        solids = new Array<>(); hares = new Array<>(); npcs = new Array<>();
        activeMissiles = new Array<>(); activeExplosions = new Array<>();
        shopItems = new Array<>();

        float startX = 640, startY = 192;
        MapLayer objectLayer = map.getLayers().get("Object Layer 1");

        if (objectLayer != null) {
            for (MapObject obj : objectLayer.getObjects()) {
                if ("start".equals(obj.getName())) {
                    startX = obj.getProperties().get("x", Float.class);
                    startY = obj.getProperties().get("y", Float.class);
                    heroPos.set(startX, startY);
                    heroBounds.setPosition(startX + 4, startY + 4);
                    break;
                }
            }

            safeAreaRect.set(startX - 48, startY - 48, 128, 128);

            for (MapObject obj : objectLayer.getObjects()) {
                float ox = 0, oy = 0;
                if (obj instanceof TiledMapTileMapObject) {
                    TiledMapTileMapObject t = (TiledMapTileMapObject) obj;
                    ox = t.getX(); oy = t.getY();
                } else if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    ox = r.x; oy = r.y;
                }

                if (obj instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) obj).getRectangle();
                    if ("Solid".equals(obj.getName()) || "Solid".equals(obj.getProperties().get("type", String.class))) {
                        solids.add(new Rectangle(r));
                    }
                }

                if (obj.getProperties().containsKey("gid")) {
                    int gid = ((Number) obj.getProperties().get("gid")).intValue();
                    if (gid == 46) npcs.add(new NPC(ox, oy, obj.getName(), obj.getProperties().get("text", String.class)));
                    else if (gid == 44) chestPos.set(ox, oy);
                    else if (gid == 47 || gid == 48) shopItems.add(new ShopItem(ox, oy, gid));
                    else if (gid == 42) solids.add(new Rectangle(ox, oy, 32, 32));
                    else if (gid == 41) {
                         boolean isSideWall = (Math.abs(ox - startX) > 10 && Math.abs(ox - startX) < 100);
                         boolean inRow = (Math.abs(oy - startY) < 128);
                         boolean inGap = (ox > 450 && ox < 850) && (oy > startY + 16);
                         if ((isSideWall && inRow) || !inGap) {
                             solids.add(new Rectangle(ox, oy, 32, 32));
                         }
                    }
                }
            }
        }

        // Difficulty Tuning
        int strongestHP = currentLevel + 1;

        // Use exact base spawns for consistency and safety
        for (int i=0; i < baseHareSpawns.size; i++) {
            Vector2 spawn = baseHareSpawns.get(i);
            int hp = currentLevel + (i % 3 == 0 ? 1 : 0);
            hares.add(new Hare(spawn.x, spawn.y, hp, false));
        }

        // Every 5 levels: Add a BOSS HARE at the very top middle
        if (currentLevel % 5 == 0) {
            int bossHP = strongestHP * 5;
            hares.add(new Hare(640 - 48, 1150, bossHP, true));
        }

        haresSlain = 0;
        message = "Slay " + hares.size + " hares!";

        solids.add(new Rectangle(0, 0, 1280, 32)); solids.add(new Rectangle(0, 0, 32, 1280));
        solids.add(new Rectangle(1248, 0, 32, 1280)); solids.add(new Rectangle(0, 1248, 1280, 32));
    }

    private Animation<TextureRegion> createAnimation(TextureRegion[][] tmp, int row) {
        int r = (row < tmp.length) ? row : 0; int c = tmp[r].length;
        if (c >= 3) return new Animation<>(ANIMATION_SPEED, tmp[r][0], tmp[r][1], tmp[r][2], tmp[r][1]);
        return new Animation<>(ANIMATION_SPEED, tmp[r][0], tmp[r][Math.min(1, c-1)]);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (gameState == State.START) {
            ScreenUtils.clear(0, 0.1f, 0.2f, 1);
            batch.setProjectionMatrix(camera.combined.cpy().setToOrtho2D(0, 0, 800, 480));
            batch.begin();
            titleFont.setColor(Color.GOLD); titleFont.draw(batch, "Journey of Jeph", 100, 320);
            uiFont.setColor(Color.WHITE); uiFont.draw(batch, "Press SPACE to Start", 260, 200);
            uiFont.setColor(Color.LIGHT_GRAY); uiFont.draw(batch, "Press C for Credits", 285, 150);
            batch.end();
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) gameState = State.PLAYING;
            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) gameState = State.CREDITS;
            return;
        }

        if (gameState == State.CREDITS) {
            ScreenUtils.clear(0, 0, 0.1f, 1);
            batch.setProjectionMatrix(camera.combined.cpy().setToOrtho2D(0, 0, 800, 480));
            batch.begin();
            titleFont.setColor(Color.GOLD);
            titleFont.getData().setScale(3.0f);
            titleFont.draw(batch, "CREDITS", 250, 420);
            titleFont.getData().setScale(4.5f);

            uiFont.setColor(Color.YELLOW);
            uiFont.draw(batch, "Lead Developer, Tester & Prompter", 180, 350);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, "Gabriel Withee", 300, 310);

            uiFont.setColor(Color.CYAN);
            uiFont.draw(batch, "Tools Used", 330, 250);
            uiFont.setColor(Color.WHITE);
            uiFont.getData().setScale(1.2f);
            uiFont.draw(batch, "libGDX, Android Studio, Gemini (AI), Tiled Map Editor", 200, 210);

            uiFont.setColor(Color.ORANGE);
            uiFont.getData().setScale(1.8f);
            uiFont.draw(batch, "Assets", 350, 150);
            uiFont.setColor(Color.WHITE);
            uiFont.getData().setScale(1.2f);
            uiFont.draw(batch, "Hero & Enemy Sprites: Claude Code (Anthropic)", 220, 110);

            uiFont.getData().setScale(1.5f);
            uiFont.setColor(Color.YELLOW);
            uiFont.draw(batch, "Press C to Return", 310, 50);
            uiFont.getData().setScale(1.8f);
            batch.end();

            if (Gdx.input.isKeyJustPressed(Input.Keys.C)) gameState = State.START;
            return;
        }

        if (gameState == State.PLAYING) update(delta);

        ScreenUtils.clear(0, 0, 0, 1);
        camera.position.set(heroPos.x + 16, heroPos.y + 16, 0); camera.update();
        mapRenderer.setView(camera); mapRenderer.render();

        batch.setProjectionMatrix(camera.combined); batch.begin();

        MapLayer objectLayer = map.getLayers().get("Object Layer 1");
        if (objectLayer != null) {
            for (MapObject obj : objectLayer.getObjects()) {
                if (obj.getProperties().containsKey("gid")) {
                    int gid = ((Number) obj.getProperties().get("gid")).intValue();
                    float ox = 0, oy = 0;
                    if (obj instanceof TiledMapTileMapObject) {
                        TiledMapTileMapObject t = (TiledMapTileMapObject) obj;
                        ox = t.getX(); oy = t.getY();
                    } else if (obj instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) obj).getRectangle();
                        ox = r.x; oy = r.y;
                    }
                    if (gid == 41) batch.draw(bushTexture, ox, oy, 32, 32);
                    else if (gid == 42) batch.draw(rockTexture, ox, oy, 32, 32);
                    else if (gid == 44) batch.draw(chestRegion, ox, oy, 32, 32);
                    else if (gid == 47) batch.draw(heartRegion, ox, oy, 32, 32);
                    else if (gid == 48) batch.draw(missileRegion, ox, oy, 32, 32);
                }
            }
        }

        for (NPC n : npcs) if (n.active) batch.draw(npcTexture, n.pos.x, n.pos.y, 32, 32);
        for (Hare h : hares) {
            float size = h.isBoss ? 96 : 32;
            batch.draw(hareAnimation.getKeyFrame(stateTime, true), h.pos.x, h.pos.y, size, size);
            batch.setColor(Color.RED); batch.draw(solidPixel, h.pos.x, h.pos.y + size + 4, size, 4);
            batch.setColor(Color.GREEN); batch.draw(solidPixel, h.pos.x, h.pos.y + size + 4, size * ((float)h.currentHealth / h.maxHealth), 4);
            batch.setColor(Color.WHITE); uiFont.getData().setScale(0.7f);
            uiFont.draw(batch, h.currentHealth + "/" + h.maxHealth, h.pos.x, h.pos.y + size + 20);
            uiFont.getData().setScale(1.8f);
        }
        for (Missile m : activeMissiles) batch.draw(arrowTexture, m.pos.x, m.pos.y, 16, 16, 32, 32, 1, 1, m.rotation, 0, 0, 32, 32, false, false);
        for (Explosion e : activeExplosions) batch.draw(explosionAnim.getKeyFrame(e.stateTime), e.pos.x - 16, e.pos.y - 16, 64, 64);

        Animation<TextureRegion> anim = walkDown;
        if (direction == 0) anim = walkUp; else if (direction == 1) anim = walkLeft; else if (direction == 3) anim = walkRight;
        float rot = (isCharging || isSpinLunging) ? stateTime * (isSpinLunging ? 1000 : 360 * chargeTimer) : 0;
        batch.draw(anim.getKeyFrame(isMoving || isSpinLunging ? stateTime : 0, true), heroPos.x, heroPos.y, 16, 16, 32, 32, 1, 1, rot);
        if (isCharging) { batch.setColor(Color.YELLOW); batch.draw(solidPixel, heroPos.x, heroPos.y + 36, 32 * (Math.min(chargeTimer, 2) / 2f), 4); batch.setColor(Color.WHITE); }
        batch.end();

        uiMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(uiMatrix); batch.begin();
        uiFont.setColor(Color.YELLOW);
        uiFont.draw(batch, "Level: " + currentLevel + " | Slain: " + haresSlain, 20, Gdx.graphics.getHeight() - 20);
        uiFont.draw(batch, "Coins: " + coins + " | Hearts: " + hearts + " | Missiles: " + missiles, Gdx.graphics.getWidth() - 600, Gdx.graphics.getHeight() - 20);

        // Move Level Counters
        uiFont.getData().setScale(1.2f);
        uiFont.setColor(Color.LIME);
        uiFont.draw(batch, "Chomp Dmg: " + chompDamage, 20, Gdx.graphics.getHeight() - 60);
        uiFont.draw(batch, "Missile Dmg: " + missileDamage, 20, Gdx.graphics.getHeight() - 90);
        uiFont.draw(batch, "Spin Dmg: " + baseSpinDamage + " (Charge x2)", 20, Gdx.graphics.getHeight() - 120);
        uiFont.getData().setScale(1.8f);

        uiFont.setColor(Color.CYAN); uiFont.draw(batch, message, 300, Gdx.graphics.getHeight() - 60);

        if (activeDialogue != null) {
            batch.setColor(0, 0, 0, 0.9f); batch.draw(solidPixel, 0, 0, Gdx.graphics.getWidth(), 160); batch.setColor(Color.WHITE);
            uiFont.draw(batch, activeDialogue, 40, 110);
        }

        if (gameState == State.GAMEOVER) {
            batch.setColor(0, 0, 0, 0.8f); batch.draw(solidPixel, 0, 0, 800, 480); batch.setColor(Color.WHITE);
            titleFont.setColor(Color.RED); titleFont.draw(batch, "GAME OVER", 180, 400);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, "Total Hares Defeated: " + lifetimeHaresDefeated, 250, 300);
            uiFont.draw(batch, "Total Coins Collected: " + lifetimeCoinsCollected, 250, 260);
            uiFont.draw(batch, "Chomp Level: " + chompDamage + " | Missile Level: " + missileDamage, 180, 200);
            uiFont.draw(batch, "Spin Level: " + baseSpinDamage, 320, 160);
            uiFont.setColor(Color.YELLOW); uiFont.draw(batch, "Press R to Return to Start", 230, 80);
        }

        if (transitioningToLevel2) {
            titleFont.setColor(Color.GOLD);
            titleFont.draw(batch, "LEVEL " + (currentLevel+1), Gdx.graphics.getWidth()/2f - 200, Gdx.graphics.getHeight()/2f + 100);
            uiFont.setColor(Color.WHITE);
            uiFont.draw(batch, "Press SPACE to proceed...", Gdx.graphics.getWidth()/2f - 200, Gdx.graphics.getHeight()/2f - 20);
        }
        batch.end();

        if (gameState == State.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) { gameState = State.START; currentLevel = 1; resetLevel(); }
    }

    private void update(float delta) {
        stateTime += delta;
        if (isSpinLunging) {
            float ox = heroPos.x, oy = heroPos.y;
            heroPos.add(spinLungeVelocity.x * delta, spinLungeVelocity.y * delta);
            heroBounds.setPosition(heroPos.x + 4, heroPos.y + 4);
            boolean hit = false; for (Rectangle s : solids) if (heroBounds.overlaps(s)) { hit = true; break; }
            if (hit) { heroPos.set(ox, oy); isSpinLunging = false; }
            spinLungeTimer -= delta; if (spinLungeTimer <= 0) isSpinLunging = false;
            if (checkMeleeHit(currentLungeDamage) == 2) { isSpinLunging = false; heroPos.add(spinLungeVelocity.x * -0.2f, spinLungeVelocity.y * -0.2f); }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
                isCharging = true; chargeTimer += delta;
                if (spinSoundId == -1) spinSoundId = spinSound.loop();
            }
            else if (isCharging) {
                isSpinLunging = true; spinLungeTimer = 0.35f;
                currentLungeDamage = (chargeTimer >= 2f) ? (baseSpinDamage * 2) : baseSpinDamage;
                float lx=0, ly=0; if(direction==0) ly=550; else if(direction==1) lx=-550; else if(direction==2) ly=-550; else lx=550;
                spinLungeVelocity.set(lx, ly); isCharging = false; chargeTimer = 0;
                if (spinSoundId != -1) { spinSound.stop(spinSoundId); spinSoundId = -1; }
            } else if (!isCharging) {
                float mx=0, my=0; isMoving = false;
                if(Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) { my+=180*delta; direction=0; isMoving=true; }
                else if(Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) { my-=180*delta; direction=2; isMoving=true; }
                if(Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) { mx-=180*delta; direction=1; isMoving=true; }
                else if(Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { mx+=180*delta; direction=3; isMoving=true; }
                float ox=heroPos.x, oy=heroPos.y;
                heroPos.x += mx; heroBounds.setPosition(heroPos.x+4, heroPos.y+4);
                for(Rectangle s : solids) if(heroBounds.overlaps(s)) { heroPos.x=ox; break; }
                heroPos.y += my; heroBounds.setPosition(heroPos.x+4, heroPos.y+4);
                for(Rectangle s : solids) if(heroBounds.overlaps(s)) { heroPos.y=oy; break; }
            }
        }

        for (int i = 0; i < hares.size; i++) {
            Hare h = hares.get(i);
            h.update(delta, solids, safeAreaRect, heroPos);
            if (heroBounds.overlaps(h.bounds)) {
                hearts--;
                if (hearts <= 0) { gameState = State.GAMEOVER; message = "Defeat!"; }
                else {
                    Vector2 kbDir = new Vector2(heroPos.x - h.pos.x, heroPos.y - h.pos.y).nor();
                    heroPos.add(kbDir.x * 50, kbDir.y * 50);
                    message = "Hit! " + hearts + " Hearts left!";
                }
            }
        }

        for (int i=activeMissiles.size-1; i>=0; i--) {
            Missile m = activeMissiles.get(i); m.update(delta); boolean hit=false;
            for (int j=hares.size-1; j>=0; j--) {
                Hare h = hares.get(j);
                float size = h.isBoss ? 96 : 32;
                if (new Rectangle(m.pos.x, m.pos.y, 24, 24).overlaps(new Rectangle(h.pos.x, h.pos.y, size, size))) {
                    h.currentHealth -= missileDamage;
                    explosionSound.play();
                    if (h.currentHealth <= 0) {
                        activeExplosions.add(new Explosion(h.pos.x+(size/2), h.pos.y+(size/2)));
                        hares.removeIndex(j);
                        haresSlain++;
                        lifetimeHaresDefeated++;
                        int reward = MathUtils.random(1, 3) * (h.isBoss ? 10 : 1);
                        coins += reward;
                        lifetimeCoinsCollected += reward;
                    }
                    hit = true; break;
                }
            }
            if (hit || !m.alive) activeMissiles.removeIndex(i);
        }
        for (int i=activeExplosions.size-1; i>=0; i--) { Explosion e = activeExplosions.get(i); e.stateTime+=delta; if (explosionAnim.isAnimationFinished(e.stateTime)) activeExplosions.removeIndex(i); }

        int currentTargetCount = hares.size + haresSlain;
        if (haresSlain >= currentTargetCount && !chestOpen) message = "The chest is open!";

        activeDialogue = null; nearShopkeeper = false; nearUpgrader = false;
        Vector2 hc = new Vector2(heroPos.x+16, heroPos.y+16);
        for (NPC n : npcs) if (n.active && hc.dst(n.pos.x+16, n.pos.y+16) < 80) {
            activeDialogue = n.name + ": " + n.text;
            if ("Shopkeeper".equals(n.name)) nearShopkeeper = true;
            if ("Upgrader".equals(n.name)) nearUpgrader = true;
        }

        if (hc.dst(chestPos.x+16, chestPos.y+16) < 48 && haresSlain >= currentTargetCount) {
            if (!chestOpen) { chestOpen = true; transitioningToLevel2 = true; for (NPC n : npcs) if ("Gatekeeper".equals(n.name)) n.active = false; }
        }

        if (transitioningToLevel2 && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { currentLevel++; resetLevel(); }
        if (nearShopkeeper) {
            if ((Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) && hearts < MAX_HEARTS) { if(coins>=3){coins-=3; hearts++; message="Purchased Heart";} }
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { if(coins>=4){coins-=4; missiles+=3; message="Purchased Missiles";} }
        } else if (nearUpgrader) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && coins >= 5) { coins-=5; chompDamage++; message="Chomp Upgraded!"; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && coins >= 5) { coins-=5; missileDamage++; message="Missile Upgraded!"; }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) && coins >= 5) { coins-=5; baseSpinDamage++; message="Spin Upgraded!"; }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            chompSound.play();
            checkMeleeHit(chompDamage);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && missiles > 0 && !isCharging && !isSpinLunging) { activeMissiles.add(new Missile(heroPos.x, heroPos.y, direction)); missiles--; }
    }

    private int checkMeleeHit(int damage) {
        Vector2 hc = new Vector2(heroPos.x+16, heroPos.y+16);
        for (int i=hares.size-1; i>=0; i--) {
            Hare h = hares.get(i);
            float size = h.isBoss ? 96 : 32;
            if (hc.dst(h.pos.x+(size/2), h.pos.y+(size/2)) < (size + 16)) {
                h.currentHealth -= damage;
                if (h.currentHealth <= 0) {
                    hares.removeIndex(i);
                    haresSlain++;
                    lifetimeHaresDefeated++;
                    int reward = MathUtils.random(1, 3) * (h.isBoss ? 10 : 1);
                    coins += reward;
                    lifetimeCoinsCollected += reward;
                    message = "Eliminated!";
                    return 1;
                }
                return 2;
            }
        }
        return 0;
    }

    @Override public void resize(int w, int h) { viewport.update(w, h); }
    @Override public void dispose() {
        batch.dispose(); if(map!=null)map.dispose(); mapRenderer.dispose(); uiFont.dispose(); titleFont.dispose(); heroSheet.dispose(); hareSheet.dispose(); npcTexture.dispose(); bushTexture.dispose(); rockTexture.dispose(); arrowTexture.dispose(); smokeSheet.dispose(); solidPixel.dispose(); objectTiles.dispose();
        chompSound.dispose(); explosionSound.dispose(); spinSound.dispose();
    }
}
