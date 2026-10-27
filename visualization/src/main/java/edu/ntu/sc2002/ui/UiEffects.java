package edu.ntu.sc2002.ui;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.util.Duration;

/** Shared intro font, card sounds, and tactile button feedback. */
public final class UiEffects {
    private static final Logger LOG = Logger.getLogger(UiEffects.class.getName());
    private static final String FONT_FAMILY = loadFont();
    private UiEffects() {}

    public static Font font(double size) { return Font.font(FONT_FAMILY, size); }
    public static String fontStyle() { return "-fx-font-family: '" + FONT_FAMILY + "';"; }

    public static void install(Button button) {
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            play(Sounds.HOVER);
            button.setScaleX(1.04); button.setScaleY(1.04);
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            button.setScaleX(1); button.setScaleY(1);
        });
        button.pressedProperty().addListener((observable, oldValue, pressed) -> {
            button.setScaleX(pressed ? .94 : 1);
            button.setScaleY(pressed ? .94 : 1);
        });
        button.addEventHandler(ActionEvent.ACTION, event -> {
            play(Sounds.CLICK);
            button.setScaleX(.94); button.setScaleY(.94);
            ScaleTransition bump = new ScaleTransition(Duration.millis(110), button);
            bump.setFromX(.94); bump.setFromY(.94);
            bump.setToX(1); bump.setToY(1);
            bump.play();
        });
    }

    private static String loadFont() {
        URL source = UiEffects.class.getResource("/friendlyscribbles/friendlyscribbles.ttf");
        Font font = source == null ? null : Font.loadFont(source.toExternalForm(), 72);
        if (font == null) LOG.warning("Friendly Scribbles unavailable; using the system font");
        return font == null ? Font.getDefault().getFamily() : font.getFamily();
    }

    private static class Sounds {
        static final AudioClip HOVER = load("card_draw_1.wav");
        static final AudioClip CLICK = load("card_draw_2.wav");
        private static AudioClip load(String name) {
            URL source = UiEffects.class.getResource("/400 Sounds Pack/Card and Board/" + name);
            try {
                if (source != null) return new AudioClip(source.toExternalForm());
            } catch (RuntimeException error) { LOG.log(Level.WARNING, "Could not load button sound " + name, error); }
            return null;
        }
    }

    private static void play(AudioClip clip) {
        if (clip != null) { clip.stop(); clip.play(); }
    }
}
