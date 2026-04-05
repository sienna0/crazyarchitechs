package edu.cornell.cis3152.physics.screen;
import edu.cornell.gdiac.util.RandomGenerator;

public class FrogPuns {
    private static final String[] WIN_PUNS = {
            "I'm him. 🐸",
            "HOPPED on them.",
            "toad-ally dominated.",
            "light work, lily pad edition.",
            "pond diff.",
            "too hoppy for y'all.",
            "croak king \uD83D\uDC51",
            "never in frog doubt.",
            "easy hops.",
            "ribbit and win.",
            "I run this pond.",
            "apex amphibian.",
            "y'all just live here, I own it.",
            "I'm the prince AND the frog.",
            "born to hop, forced to carry.",
            "they can't handle my leap mechanics.",
            "top of the food chain.",
            "graduated from tadpole academy.",
            "I evolved mid-game.",
            "flies were light work.",
            "they thought I was pond NPC.",
            "main character frog fr.",
            "built different… amphibiously.",
            "sent them back to the swamp.",
            "they got croaked.",
            "frog diff was CRAZY.",
            "I farmed them for flies.",
            "out-hopped and outplayed.",
            "they wasn't ready for this ecosystem.",
            "Zuko wins. Always.",
            "camera tech diff.",
            "I snapped and they flopped.",
            "polaroid to victory.",
            "thermal vision, zero competition."
    };

    private static final String[] LOSE_PUNS = {
            "croaked.",
            "toad-ally cooked.",
            "not very hoppy rn.",
            "ribbit in peace.",
            "I flopped… amphibiously.",
            "lily pad diff.",
            "pond gap.",
            "frog skill issue.",
            "just hopped into an L.",
            "green but not go.",
            "slimy performance ngl.",
            "tongue diff.",
            "I got FROGGED.",
            "they turned me into frog legs.",
            "I got sautéed.",
            "I'm the side quest frog fr.",
            "spawned just to croak.",
            "they sent me back to the pond.",
            "I'm uninstalling from the swamp.",
            "I didn't lose, I evolved backwards.",
            "tutorial frog activities.",
            "I got bullied in 4K lily pad HD.",
            "they farming me like flies.",
            "tell my tadpoles I tried.",
            "this is a frog tragedy.",
            "my villain origin story starts here.",
            "I leave this pond in shame.",
            "I was not built for this ecosystem.",
            "Zuko could never.",
            "the camera betrayed me.",
            "I photogged myself into a loss.",
            "lens diff.",
            "I peaked in the tutorial.",
            "the physics said no."
    };

    public static String randomWin() {
        return WIN_PUNS[RandomGenerator.getInt(0, WIN_PUNS.length-1)];
    }

    public static String randomLose() {
        return LOSE_PUNS[RandomGenerator.getInt(0, LOSE_PUNS.length-1)];
    }
}
