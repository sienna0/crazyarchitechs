package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class LevelProgress {

    public static class LevelData {
        public boolean complete;

        public int stars;
        public int minPhotosUsed;
        public int flyCount;

         LevelData (){
            this.complete = false;
            this.stars = 0;
            minPhotosUsed = -1;
            flyCount = 0;
        }

    }

    public final int numLevels;

    public Array<LevelData> levels;

    private String saveGameFile;

    public void beatLevel(int level, int photosUsed, int flyCount) {
        LevelData currLevel = levels.get(level - 1);
        currLevel.complete = true;
        if ((currLevel.minPhotosUsed == -1) || photosUsed < currLevel.minPhotosUsed) {currLevel.minPhotosUsed = photosUsed;}
        currLevel.flyCount = flyCount;
        saveGame();
    }

    public boolean isBeaten(int level) {return levels.get(level - 1).complete;}

    public LevelData getLevelData(int level){return levels.get(level - 1);}

    /**
     *
     * @param level is level to query
     * @return 0-3. 0 == not passed, 1 == passed, 2 == 1 & got second star, 3 == 2 & third star
     */
    public int getLevelScore(int level){
        if (!isBeaten(level)){return 0;}
        int score = 1;
        if (getLevelData(level).flyCount > 0) {score++;}
        return score;
    }

    public int getNumber(int level) { return levels.get(level - 1).minPhotosUsed; }
    public void setNumber(int level, int n) { levels.get(level - 1).minPhotosUsed = n; }

    LevelProgress (int totalNumLevels, String saveFile){
        saveGameFile = saveFile;
        numLevels = totalNumLevels;
        levels = new Array<LevelData>(totalNumLevels);
        for (int i = 0; i < totalNumLevels; i++) {
            levels.add(new LevelData());
        }
        loadGame();
    }

    public void saveGame() {
        Json js = new Json();
        js.setOutputType(JsonWriter.OutputType.json);
        try (Writer writer = new FileWriter(saveGameFile)) {
            String pretty = js.prettyPrint(js.toJson(levels, Array.class, LevelData.class));
            writer.write(pretty);
        } catch (Exception e) {
            System.out.println("Error saving game");
            resetSaveGame();
        }
    }

    public void loadGame(){
        Json js = new Json();
        js.setOutputType(JsonWriter.OutputType.json);
        try (Reader reader = new FileReader(saveGameFile)) {
            levels = js.fromJson(Array.class, LevelData.class, reader);
        } catch (Exception e) {
            System.out.println("Error loading game");
            resetSaveGame();
        }
    }

    public void resetSaveGame(){
        levels = new Array<LevelData>(numLevels);
        for (int i = 0; i < numLevels; i++) {
            levels.add(new LevelData());
        }
        saveGame();
    }




}
