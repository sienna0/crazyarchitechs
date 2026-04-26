package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.utils.Array;

public class LevelProgress {

    public class LevelData {
        public boolean complete;

        public int stars;
        public int minPhotosUsed;

        LevelData (){
            this.complete = false;
            this.stars = 0;
            minPhotosUsed = -1;
        }

    }

    public Array<LevelData> levels;

    public void beatLevel(int level, int photosUsed) {
        LevelData currLevel = levels.get(level - 1);
        currLevel.complete = true;
        if ((currLevel.minPhotosUsed == -1) || photosUsed < currLevel.minPhotosUsed) {currLevel.minPhotosUsed = photosUsed;}
    }

    public boolean isBeaten(int level) {return levels.get(level - 1).complete;}

    public LevelData getLevelData(int level){return levels.get(level - 1);}

    public int getLevelScore(int level){return levels.get(level - 1).minPhotosUsed;}

    public int getNumber(int level) { return levels.get(level - 1).minPhotosUsed; }
    public void setNumber(int level, int n) { levels.get(level - 1).minPhotosUsed = n; }

    LevelProgress (int totalNumLevels){
        levels = new Array<LevelData>(totalNumLevels);
        for (int i = 0; i < totalNumLevels; i++) {
            levels.add(new LevelData());
        }
    }


}
