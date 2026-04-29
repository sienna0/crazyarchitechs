package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.FlyCollectible;
import edu.cornell.cis3152.physics.world.GameObject;
import edu.cornell.cis3152.physics.world.Obj;
import edu.cornell.cis3152.physics.world.Picture;
import edu.cornell.cis3152.physics.world.Zuko;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.Obstacle;
import java.util.ArrayList;

/**
 * Responsible for all non-physics rendering: HUD inventory bar, stuck-picture overlays (surface-with-photo combo art),
 * highlight outlines, range indicators, pause icon, and level tile backgrounds.
 * <p>
 * Uses screen-space coordinates for HUD elements.
 */
class LevelRenderer {
    private static final float UI = CanvasRender.layoutScale();

    /**
     * Stuck sticker span relative to the block's drawable mesh (min of width/height in world pixels).
     * Keeps the icon similar to vanilla blocks at any resolution.
     */
    private static final float STUCK_STICKER_BLOCK_FRACTION = 0.40f;

    /** Legacy Polaroid reference inner width (world px at old tuning); borders scale with this. */
    private static final float STUCK_PICTURE_REF_INNER = 22.0f;
    private static final float STUCK_PICTURE_BORDER = 2.0f;
    private static final float STUCK_PICTURE_INNER_PADDING = 4.0f;
    private static final float INVENTORY_BAR_HEIGHT = 56.0f * UI;
    private static final float INVENTORY_PADDING = 8.0f * UI;
    /** Slot size is capped so the HUD stays reasonable with few inventory slots. */
    private static final float MAX_SLOT_SIZE = 40.0f * UI;
    /** Flat strip behind the inventory row (not gray — avoids a second “border”). */
    private static final Color INVENTORY_BAR_BACK = new Color(0.1f, 0.08f, 0.07f, 0.92f);
    private final WorldState worldState;
    /** Empty slots, and frame overlay on filled slots ({@code shared/inventory.png}). */
    private final Texture inventoryTexture;
    private final Texture settingsIconTexture;
    private final Texture pauseIconTexture;
    private final Texture markerPixel;
    /** [target surface][photographed subject] — matches {@code picture_<surface>_with_<photo>.png}. */
    private final Texture[][] stuckPictureTextures;
    private final float stickDistance;
    private final float takeDistance;
    private final Affine2 highlightTransform = new Affine2();
    private ArrayList<FlyCollectible> inRangeFlies = new ArrayList<>();

    void setInRangeFlies(ArrayList<FlyCollectible> flies) {
        this.inRangeFlies = flies != null ? flies : new ArrayList<>();
    }

    LevelRenderer(WorldState worldState,
                  Texture inventoryTexture,
                  Texture settingsIconTexture,
                  Texture pauseIconTexture,
                  Texture markerPixel,
                  Texture[][] stuckPictureTextures,
                  float stickDistance,
                  float takeDistance) {
        this.worldState = worldState;
        this.inventoryTexture = inventoryTexture;
        this.settingsIconTexture = settingsIconTexture;
        this.pauseIconTexture = pauseIconTexture;
        this.markerPixel = markerPixel;
        this.stuckPictureTextures = stuckPictureTextures;
        this.stickDistance = stickDistance;
        this.takeDistance = takeDistance;
    }

    /**
     * Main draw entry — renders highlights, range, placed pictures, inventory bar, and pause icon.
     */
    void draw(SpriteBatch batch,
              CanvasRender viewport,
              com.badlogic.gdx.graphics.OrthographicCamera camera,
              com.badlogic.gdx.graphics.OrthographicCamera uiCamera,
              Zuko avatar) {
        if (avatar == null) {
            return;
        }

        viewport.apply();
        batch.begin(camera);
        batch.setColor(worldState.getActivePicture() != null ? Color.LIME : Color.CORAL);

        for (GameObject go : worldState.getHighlighted()) {
            drawHighlight(batch, go);
        }

        batch.setColor(Color.GOLD);
        for (FlyCollectible fly : inRangeFlies) {
            drawFlyHighlight(batch, fly);
        }

        if (worldState.isShowRange()) {
            drawRanges(batch, avatar);
        }

        drawPlacedPictures(batch);
        batch.end();
        viewport.reset();

        viewport.apply();
        batch.begin(uiCamera);
        int invSize = avatar.getPictureInventory().getSize();
        float slotSz = Math.min(MAX_SLOT_SIZE, INVENTORY_BAR_HEIGHT - 2 * INVENTORY_PADDING);
        float dynBarWidth = invSize * slotSz + (invSize + 1) * INVENTORY_PADDING;
        float barX = viewport.getWidth() / 2 - dynBarWidth / 2.0f;
        batch.setColor(INVENTORY_BAR_BACK);
        batch.draw(markerPixel, barX, 0, dynBarWidth, INVENTORY_BAR_HEIGHT);
        drawInventory(batch, viewport, avatar);
        batch.setColor(Color.WHITE);

        float baseSize = LevelHud.baseIconSize();
        float pauseRight = viewport.getWidth() - LevelHud.margin();
        float pauseTop = viewport.getHeight() - LevelHud.margin();

        float pIcon = worldState.isPauseIconHovered() ? LevelHud.hoverIconSize() : baseSize;
        float pauseDraw = pIcon * LevelHud.iconDrawScale();
        float pauseX = pauseRight - pauseDraw;
        float pauseY = pauseTop - pauseDraw;

        if (settingsIconTexture != null) {
            float sIcon = worldState.isSettingsIconHovered() ? LevelHud.hoverIconSize() : baseSize;
            float settingsDraw = sIcon * LevelHud.iconDrawScale();
            float settingsLeft = pauseX - LevelHud.iconGap() - settingsDraw;
            float iconYSettings = pauseTop - settingsDraw;
            batch.draw(settingsIconTexture, settingsLeft, iconYSettings, settingsDraw, settingsDraw);
        }

        if (pauseIconTexture != null) {
            batch.draw(pauseIconTexture, pauseX, pauseY, pauseDraw, pauseDraw);
        }

        batch.end();
        viewport.reset();
    }

    /**
     * Hit-tests a mouse position against the inventory bar slots; returns slot index or {@code -1}.
     */
    int getClickedSlot(float mouseX, float mouseY, CanvasRender viewport, int inventorySize) {
        float padding = INVENTORY_PADDING;
        float barHeight = INVENTORY_BAR_HEIGHT;
        float slotSize = Math.min(MAX_SLOT_SIZE, barHeight - 2 * padding);
        float barWidth = inventorySize * slotSize + (inventorySize + 1) * padding;
        float barX = viewport.getWidth() / 2 - barWidth / 2;
        float startX = barX + padding;

        for (int ii = 0; ii < inventorySize; ii++) {
            float slotX = startX + ii * (slotSize + padding);
            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= 0f && mouseY <= barHeight) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Draws a thick colored outline around a highlighted object.
     */
    private void drawHighlight(SpriteBatch batch, GameObject go) {
        Obstacle obj = go.getObstacle();
        float units = obj.getPhysicsUnits();
        float angle = obj.getAngle();
        Vector2 position = obj.getPosition();

        highlightTransform.idt();
        highlightTransform.preRotate((float) (angle * 180.0f / Math.PI));
        highlightTransform.preTranslate(position.x * units, position.y * units);
        batch.outline(obj.getOutline(), highlightTransform);

        for (int offset = -2; offset <= 2; offset++) {
            highlightTransform.idt();
            highlightTransform.preRotate((float) (angle * 180.0f / Math.PI));
            highlightTransform.preTranslate(position.x * units + offset, position.y * units);
            batch.outline(obj.getOutline(), highlightTransform);
        }
        for (int offset = -2; offset <= 2; offset++) {
            highlightTransform.idt();
            highlightTransform.preRotate((float) (angle * 180.0f / Math.PI));
            highlightTransform.preTranslate(position.x * units, position.y * units + offset);
            batch.outline(obj.getOutline(), highlightTransform);
        }
    }

    private void drawFlyHighlight(SpriteBatch batch, FlyCollectible fly) {
        Obstacle obj = fly.getObstacle();
        float units = obj.getPhysicsUnits();
        float angle = obj.getAngle();
        Vector2 position = obj.getPosition();
        highlightTransform.idt();
        highlightTransform.preRotate((float) (angle * 180.0f / Math.PI));
        highlightTransform.preTranslate(position.x * units, position.y * units);
        batch.outline(obj.getOutline(), highlightTransform);
    }

    /**
     * Draws dashed circles showing take and stick ranges around Zuko.
     */
    private void drawRanges(SpriteBatch batch, Zuko avatar) {
        Obstacle obj = avatar.getObstacle();
        Vector2 position = obj.getPosition();
        float units = obj.getPhysicsUnits();
        float cx = position.x * units;
        float cy = position.y * units;
        float dashSize = 20f;
        float gapSize = 10f;
        float total = dashSize + gapSize;
        PathFactory factory = new PathFactory();

        highlightTransform.idt();
        batch.setColor(Color.LIME);
        for (float angle = 0; angle < 360; angle += total) {
            batch.outline(factory.makeArc(cx, cy, (stickDistance * units * 2) - 1, angle, dashSize, false), highlightTransform);
            batch.outline(factory.makeArc(cx, cy, (stickDistance * units * 2), angle, dashSize, false), highlightTransform);
            batch.outline(factory.makeArc(cx, cy, (stickDistance * units * 2) - 2, angle, dashSize, false), highlightTransform);
        }
        batch.setColor(Color.CORAL);
        for (float angle = 0; angle < 360; angle += total) {
            batch.outline(factory.makeArc(cx, cy, (takeDistance * units * 2) - 1, angle, dashSize, false), highlightTransform);
            batch.outline(factory.makeArc(cx, cy, (takeDistance * units * 2), angle, dashSize, false), highlightTransform);
            batch.outline(factory.makeArc(cx, cy, (takeDistance * units * 2) - 2, angle, dashSize, false), highlightTransform);
        }
    }

    /**
     * World-space sticker size for a block: fraction of its mesh so it scales with {@code physicsUnits} and art size.
     */
    private float computeStuckStickerSize(GameObject target) {
        Rectangle b = target.getMesh().computeBounds();
        float span = Math.min(Math.abs(b.width), Math.abs(b.height));
        if (span < 1e-3f) {
            span = 28f * UI;
        }
        return span * STUCK_STICKER_BLOCK_FRACTION;
    }

    /**
     * Draws combo art on surfaces that have a stuck picture (e.g. {@code picture_cloud_with_ice.png}).
     */
    private void drawPlacedPictures(SpriteBatch batch) {
        for (Picture picture : worldState.getPictures()) {
            if (picture.getTarget() == null) {
                continue;
            }

            GameObject target = picture.getTarget();
            GameObject subject = picture.getSubject();
            Obstacle obstacle = target.getObstacle();
            float units = obstacle.getPhysicsUnits();
            float centerX = obstacle.getX() * units;
            float centerY = obstacle.getY() * units;
            float rotation = obstacle.getAngle() * MathUtils.radiansToDegrees;

            float stickerInner = computeStuckStickerSize(target);

            Texture combo = resolveStuckComboTexture(subject, target);
            if (combo != null) {
                batch.setColor(Color.WHITE);
                float sz = stickerInner * (34f / STUCK_PICTURE_REF_INNER) * 1.64f;
                batch.draw(combo,
                        centerX - (sz * 0.5f),
                        centerY - (sz * 0.5f),
                        sz * 0.5f,
                        sz * 0.5f,
                        sz,
                        sz,
                        1.0f,
                        1.0f,
                        rotation,
                        0,
                        0,
                        combo.getWidth(),
                        combo.getHeight(),
                        false,
                        false);
                continue;
            }

            float k = stickerInner / STUCK_PICTURE_REF_INNER;
            float outer = stickerInner + (STUCK_PICTURE_BORDER * 2.0f * k);
            float mid = stickerInner;
            float innerTint = Math.max(4f * UI, stickerInner - (STUCK_PICTURE_INNER_PADDING * 2.0f * k));
            drawStuckPictureLayer(batch, centerX, centerY, outer, Color.BLACK, rotation);
            drawStuckPictureLayer(batch, centerX, centerY, mid, new Color(0.96f, 0.95f, 0.91f, 1.0f), rotation);
            drawStuckPictureLayer(batch, centerX, centerY, innerTint, picture.getColor(), rotation);

            Texture subjectTexture = picture.getTexture();
            if (subjectTexture != null) {
                float textureSize = Math.max(2f * UI, innerTint - (4.0f * k));
                batch.setColor(Color.WHITE);
                batch.draw(subjectTexture,
                        centerX - (textureSize * 0.5f),
                        centerY - (textureSize * 0.5f),
                        textureSize * 0.5f,
                        textureSize * 0.5f,
                        textureSize,
                        textureSize,
                        1.0f,
                        1.0f,
                        rotation,
                        0,
                        0,
                        subjectTexture.getWidth(),
                        subjectTexture.getHeight(),
                        false,
                        false);
            }
        }
    }

    /** Assets follow {@code picture_<surface>_with_<photographed type>.png} — index [target][subject]. */
    private Texture resolveStuckComboTexture(GameObject subject, GameObject target) {
        if (subject == null || target == null || stuckPictureTextures == null) {
            return null;
        }
        int si = subject.getObjectType().ordinal();
        int ti = target.getObjectType().ordinal();
        if (ti < 0 || ti >= stuckPictureTextures.length) {
            return null;
        }
        Texture[] row = stuckPictureTextures[ti];
        if (row == null || si < 0 || si >= row.length) {
            return null;
        }
        return row[si];
    }

    /**
     * Helper — draws a single colored square layer for the Polaroid effect.
     */
    private void drawStuckPictureLayer(SpriteBatch batch, float centerX, float centerY, float size, Color color, float rotation) {
        batch.setColor(color);
        batch.draw(markerPixel, centerX - (size * 0.5f), centerY - (size * 0.5f), size * 0.5f, size * 0.5f,
                size, size, 1.0f, 1.0f, rotation, 0, 0, 1, 1,                 false, false);
    }

    /**
     * Draws the bottom HUD bar with picture slots.
     */
    private void drawInventory(SpriteBatch batch, CanvasRender viewport, Zuko avatar) {
        float barHeight = INVENTORY_BAR_HEIGHT;
        float barY = 0f;
        float padding = INVENTORY_PADDING;
        int size = avatar.getPictureInventory().getSize();
        float slotSize = Math.min(MAX_SLOT_SIZE, barHeight - 2 * padding);
        float barWidth = size * slotSize + (size + 1) * padding;
        float barX = viewport.getWidth() / 2 - barWidth / 2;
        float startX = barX + padding;
        float startY = barY + (barHeight - slotSize) / 2f;
        float selectedRaise = 8f * UI;
        // Inner margin for subject art vs. frame hole (inventory.png is 200×200 with ~44px border).
        float frameInnerPad = slotSize * 0.22f;

        for (int ii = 0; ii < size; ii++) {
            float slotX = startX + ii * (slotSize + padding);
            float slotY = ii == worldState.getSelectedSlotIndex() ? startY + selectedRaise : startY;
            Picture picture = avatar.getPictureInventory().getPicture(ii);

            if (picture != null && picture.hasSubject()) {
                batch.setColor(Color.WHITE);
                GameObject inventoryObject = picture.getSubject();
                if (inventoryObject.object == Obj.CLOUD) {
                    batch.setColor(inventoryObject.getCloudColor());
                }
                batch.draw(inventoryObject.getTexture(), slotX + frameInnerPad, slotY + frameInnerPad,
                        slotSize - 2f * frameInnerPad, slotSize - 2f * frameInnerPad);
                batch.setColor(Color.WHITE);
            }
            batch.setColor(Color.WHITE);
            batch.draw(inventoryTexture, slotX, slotY, slotSize, slotSize);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Renders the tilemap background tiles.
     */
    void drawLevelTiles(SpriteBatch batch, LevelPopulation.Result levelData, float tileSize) {
        if (levelData == null || levelData.tileRegions.isEmpty()) {
            return;
        }

        batch.setColor(Color.WHITE);
        for (int ii = 0; ii < levelData.tileRegions.size(); ii++) {
            TextureRegion region = levelData.tileRegions.get(ii);
            float[] position = levelData.tilePositions.get(ii);
            batch.draw(region, position[0], position[1], tileSize, tileSize);
        }
    }
}
