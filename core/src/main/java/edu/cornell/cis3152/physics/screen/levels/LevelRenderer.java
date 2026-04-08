package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.GameObject;
import edu.cornell.cis3152.physics.world.Picture;
import edu.cornell.cis3152.physics.world.Zuko;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.Obstacle;

/**
 * Responsible for all non-physics rendering: HUD inventory bar, placed-picture Polaroid overlays,
 * highlight outlines, range indicators, pause icon, and level tile backgrounds.
 * <p>
 * Uses screen-space coordinates for HUD elements.
 */
class LevelRenderer {
    private static final float STUCK_PICTURE_SIZE = 22.0f;
    private static final float STUCK_PICTURE_BORDER = 2.0f;
    private static final float STUCK_PICTURE_INNER_PADDING = 4.0f;
    private static final float INVENTORY_BAR_HEIGHT = 56.0f;
    private static final float INVENTORY_PADDING = 8.0f;
    /** Slot size is capped so the HUD stays reasonable with few inventory slots. */
    private static final float MAX_SLOT_SIZE = 40.0f;
    private static final float PAUSE_ICON_SIZE = 56.0f;
    private static final float PAUSE_ICON_HOVER_SIZE = 64.0f;

    private final WorldState worldState;
    private final Texture slotTexture;
    private final Texture pauseIconTexture;
    private final Texture markerPixel;
    private final float stickDistance;
    private final float takeDistance;
    private final Affine2 highlightTransform = new Affine2();

    LevelRenderer(WorldState worldState,
                  Texture slotTexture,
                  Texture pauseIconTexture,
                  Texture markerPixel,
                  float stickDistance,
                  float takeDistance) {
        this.worldState = worldState;
        this.slotTexture = slotTexture;
        this.pauseIconTexture = pauseIconTexture;
        this.markerPixel = markerPixel;
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
        batch.setColor(new Color(0.3f, 0.3f, 0.3f, 0.8f));
        batch.draw(slotTexture, viewport.getWidth() / 2 - dynBarWidth / 2.0f, 0, dynBarWidth, INVENTORY_BAR_HEIGHT);
        drawInventory(batch, viewport, avatar);
        batch.setColor(Color.WHITE);

        if (pauseIconTexture != null) {
            float baseSize = PAUSE_ICON_SIZE;
            float iconSize = worldState.isPauseIconHovered() ? PAUSE_ICON_HOVER_SIZE : baseSize;
            float baseX = viewport.getWidth() - baseSize - 15f;
            float baseY = viewport.getHeight() - baseSize - 15f;
            float iconX = baseX - (iconSize - baseSize) / 2f;
            float iconY = baseY - (iconSize - baseSize) / 2f;
            batch.draw(pauseIconTexture, iconX, iconY, iconSize, iconSize);
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
     * Renders Polaroid-style overlays on objects that have pictures stuck to them.
     */
    private void drawPlacedPictures(SpriteBatch batch) {
        for (Picture picture : worldState.getPictures()) {
            if (picture.getTarget() == null) {
                continue;
            }

            GameObject target = picture.getTarget();
            Obstacle obstacle = target.getObstacle();
            float units = obstacle.getPhysicsUnits();
            float centerX = obstacle.getX() * units;
            float centerY = obstacle.getY() * units;
            float rotation = obstacle.getAngle() * MathUtils.radiansToDegrees;

            drawStuckPictureLayer(batch, centerX, centerY, STUCK_PICTURE_SIZE + (STUCK_PICTURE_BORDER * 2.0f), Color.BLACK, rotation);
            drawStuckPictureLayer(batch, centerX, centerY, STUCK_PICTURE_SIZE, new Color(0.96f, 0.95f, 0.91f, 1.0f), rotation);
            drawStuckPictureLayer(batch, centerX, centerY, STUCK_PICTURE_SIZE - (STUCK_PICTURE_INNER_PADDING * 2.0f), picture.getColor(), rotation);

            Texture subjectTexture = picture.getTexture();
            if (subjectTexture != null) {
                float textureSize = STUCK_PICTURE_SIZE - (STUCK_PICTURE_INNER_PADDING * 2.0f) - 4.0f;
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
        float selectedRaise = 8f;

        for (int ii = 0; ii < size; ii++) {
            float slotX = startX + ii * (slotSize + padding);
            float slotY = ii == worldState.getSelectedSlotIndex() ? startY + selectedRaise : startY;
            Picture picture = avatar.getPictureInventory().getPicture(ii);

            batch.setColor(Color.GRAY);
            batch.draw(slotTexture, slotX, slotY, slotSize, slotSize);
            if (picture != null && picture.hasSubject()) {
                batch.setColor(picture.getColor());
                batch.draw(slotTexture, slotX, slotY, slotSize, slotSize);
                batch.setColor(Color.WHITE);
                batch.draw(picture.getSubject().getTexture(), slotX + 4f, slotY + 4f, slotSize - 8f, slotSize - 8f);
            }
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
