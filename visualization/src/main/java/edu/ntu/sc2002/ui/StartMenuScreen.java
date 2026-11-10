package edu.ntu.sc2002.ui;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/** Animated start menu for choosing difficulty, starting the game and managing menu audio. */
public final class StartMenuScreen {
    private static final Logger LOG = Logger.getLogger(StartMenuScreen.class.getName());
    private static final double IMAGE_WIDTH = 576;
    private static final double IMAGE_HEIGHT = 324;
    private static final double MUSIC_START_SCENE_SECONDS = 5.0;
    private static final double CITY_SLOWDOWN_DURATION_SECONDS = 7.0;
    private static final double WIND_FADE_SECONDS = 1.0;
    private static final double MENU_TRACK_START_POSITION_SECONDS = 19.8;
    private static final double TITLE_TRACK_CUE_SECONDS = 21.5;
    private static final double INITIAL_SCROLL_SPEED = 5.0;
    private static final double FINAL_SCROLL_SPEED = 0.1;
    private static final double[] DEPTH_SPEED = {0.08, 0.2, 0.36, 0.55, 0.76, 1.0};
    private static final Color HARD_COLOR = Color.web("#C77B82");
    private static final Color MEDIUM_COLOR = Color.web("#E6B450");
    private static final Color EASY_COLOR = Color.web("#A4C98A");

    private final Pane root = new Pane();
    private final ImageView[][] cityTiles = new ImageView[6][2];
    private final double[] cityOffsets = new double[6];
    private final Text credits = new Text("Group 5 presents");
    private final Text title = new Text("Restaurant Rush");
    private final Button startButton = new Button("Start Game");
    private final Button menuButton = new Button("Menu");
    private final Button hideButton = new Button("Hide");
    private final Button hardButton = new Button("Hard");
    private final Button mediumButton = new Button("Medium");
    private final Button easyButton = new Button("Easy like OOP");
    private final Button[] buttons = {startButton, menuButton, hideButton,
            hardButton, mediumButton, easyButton};
    private final Button[] difficultyButtons = {easyButton, mediumButton, hardButton};
    private Button selectedDifficulty;
    private String selectedDifficultyName = "easy";
    private final java.util.function.Consumer<String> onStart;
    private final MediaPlayer wind;
    private final MediaPlayer menuTrack;
    private final MediaPlayer muffledTrack;
    private final AnimationTimer animation;

    private long launchNanos;
    private long previousNanos;
    private boolean creditsFadingIn;
    private boolean creditsFadingOut;
    private boolean windStopped;
    private boolean menuTrackStarted;
    private boolean titleStarted;
    private boolean activated;
    private boolean muffledStarted;
    private double buttonFontSize = 64;

    /**
     * Builds the animated menu.
     *
     * @param onStart callback receiving the selected difficulty when the player starts the game
     */
    public StartMenuScreen(java.util.function.Consumer<String> onStart) {
        this.onStart = onStart;
        wind = createPlayer("/menu-wind.wav");
        menuTrack = createPlayer("/Potsu - sunny.mp3");
        muffledTrack = createPlayer("/Potsu - sunny (muffled).mp3");
        if (menuTrack != null) {
            menuTrack.setVolume(0.5);
        }

        root.setStyle("-fx-background-color: black;");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        for (int layer = 0; layer < cityTiles.length; layer++) {
            URL imageUrl = resource("/craftpix-net-322807-free-city-backgrounds-pixel-art/city 2/" + (layer + 1) + ".png");
            if (imageUrl == null) {
                LOG.warning("Missing city layer " + (layer + 1));
                continue;
            }
            Image image = new Image(imageUrl.toExternalForm(), false);
            if (image.isError()) {
                LOG.log(Level.WARNING, "Could not load city layer " + (layer + 1), image.getException());
                continue;
            }
            for (int tile = 0; tile < 2; tile++) {
                ImageView view = new ImageView(image);
                view.setSmooth(false);
                cityTiles[layer][tile] = view;
                root.getChildren().add(view);
            }
        }

        credits.setFill(Color.WHITE);
        credits.setTextAlignment(TextAlignment.CENTER);
        credits.setOpacity(0);
        credits.setMouseTransparent(true);
        title.setFill(Color.WHITE);
        title.setOpacity(0);
        title.setMouseTransparent(true);
        root.getChildren().addAll(credits, title, startButton, menuButton,
                hideButton, hardButton, mediumButton, easyButton);

        configureButton(startButton, this::startGame);
        configureButton(menuButton, () -> transitionDifficultyMenu(true));
        configureButton(hideButton, () -> transitionDifficultyMenu(false));
        configureButton(hardButton, () -> selectDifficulty(hardButton, HARD_COLOR, "hard"));
        configureButton(mediumButton, () -> selectDifficulty(mediumButton, MEDIUM_COLOR, "medium"));
        configureButton(easyButton, () -> selectDifficulty(easyButton, EASY_COLOR, "easy"));
        for (Button button : buttons) {
            button.setOpacity(0);
            button.setDisable(true);
        }
        selectDifficulty(easyButton, EASY_COLOR, "easy");

        root.widthProperty().addListener((observable, oldValue, newValue) -> layoutScene());
        root.heightProperty().addListener((observable, oldValue, newValue) -> layoutScene());
        animation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsed = (now - launchNanos) / 1_000_000_000.0;
                double delta = Math.min((now - previousNanos) / 1_000_000_000.0, 0.1);
                previousNanos = now;
                updateCity(elapsed, delta);
                updateCues(elapsed);
            }
        };
    }

    /** @return the menu's root pane */
    public Pane getRoot() {
        return root;
    }

    /** Starts the cinematic animation and menu audio. */
    public void start() {
        launchNanos = System.nanoTime();
        previousNanos = launchNanos;
        if (wind != null) {
            wind.setVolume(1);
            wind.play();
        }
        layoutScene();
        animation.start();
    }

    /** Stops the menu animation and releases its media players. */
    public void dispose() {
        animation.stop();
        for (MediaPlayer player : new MediaPlayer[]{wind, menuTrack, muffledTrack}) {
            if (player != null) player.dispose();
        }
    }

    private void updateCity(double elapsed, double delta) {
        double progress = Math.min(Math.max(elapsed - MUSIC_START_SCENE_SECONDS, 0)
                / CITY_SLOWDOWN_DURATION_SECONDS, 1.0);
        double smooth = progress * progress * (3 - 2 * progress);
        double speed = INITIAL_SCROLL_SPEED
                + (FINAL_SCROLL_SPEED - INITIAL_SCROLL_SPEED) * smooth;
        double width = root.getWidth();
        if (width > 0) {
            for (int layer = 0; layer < cityOffsets.length; layer++) {
                cityOffsets[layer] += speed * DEPTH_SPEED[layer] * delta;
            }
        }
        layoutScene();
    }

    private void updateCues(double elapsed) {
        if (!creditsFadingIn && elapsed >= 2) {
            creditsFadingIn = true;
            fade(credits, 1, 0.5).play();
        }
        if (!creditsFadingOut && elapsed >= 4.5) {
            creditsFadingOut = true;
            fade(credits, 0, 0.5).play();
        }
        if (!menuTrackStarted && elapsed >= MUSIC_START_SCENE_SECONDS) {
            menuTrackStarted = true;
            if (menuTrack != null) {
                menuTrack.setCycleCount(MediaPlayer.INDEFINITE);
                menuTrack.seek(Duration.seconds(MENU_TRACK_START_POSITION_SECONDS));
                menuTrack.play();
            }
        }
        if (!windStopped && menuTrackStarted) {
            double fadeProgress = Math.min((elapsed - MUSIC_START_SCENE_SECONDS) / WIND_FADE_SECONDS, 1.0);
            if (wind != null) {
                wind.setVolume(1 - fadeProgress);
            }
            if (fadeProgress >= 1.0) {
                windStopped = true;
                stop(wind);
            }
        }
        boolean songCueReached = menuTrackStarted && menuTrack != null
                && menuTrack.getCurrentTime().toSeconds() >= TITLE_TRACK_CUE_SECONDS;
        double titleSceneFallback = MUSIC_START_SCENE_SECONDS + TITLE_TRACK_CUE_SECONDS
                - MENU_TRACK_START_POSITION_SECONDS;
        if (!titleStarted && (songCueReached || elapsed >= titleSceneFallback)) {
            dropTitle();
        }
    }

    private void layoutScene() {
        double width = root.getWidth();
        double height = root.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double tileWidth = Math.max(width, height * IMAGE_WIDTH / IMAGE_HEIGHT);
        double tileHeight = tileWidth * IMAGE_HEIGHT / IMAGE_WIDTH;
        double tileRatio = tileWidth / width;
        double y = (height - tileHeight) / 2;
        for (int layer = 0; layer < cityTiles.length; layer++) {
            cityOffsets[layer] %= tileRatio;
            double x = -cityOffsets[layer] * width;
            for (int tile = 0; tile < 2; tile++) {
                ImageView view = cityTiles[layer][tile];
                if (view != null) {
                    view.setFitWidth(tileWidth);
                    view.setFitHeight(tileHeight);
                    view.setX(x + tile * tileWidth);
                    view.setY(y);
                }
            }
        }
        credits.setFont(font(Math.min(height * 0.16, width * 0.085)));
        title.setFont(font(Math.min(height * 0.17, width * 0.09)));
        centerText(credits, (height - credits.getLayoutBounds().getHeight()) / 2);
        centerText(title, height * 0.28 - title.getLayoutBounds().getHeight() / 2);
        buttonFontSize = Math.min(height * 0.09, width * 0.07);
        for (Button button : buttons) {
            button.setFont(font(baseButtonFontSize(button) + (button.isHover() ? 10 : 0)));
            if (root.getScene() != null) {
                button.applyCss();
            }
            button.autosize();
            button.layout();
        }
        positionButton(startButton, (width - startButton.prefWidth(-1)) / 2,
                height * 0.9 - startButton.prefHeight(-1) / 2);
        double menuX = width * 0.75;
        double menuCenterY = height * 0.9 - menuButton.prefHeight(-1) / 2;
        double menuTop = height * 0.9 - menuButton.prefHeight(-1);
        double menuGap = baseButtonFontSize(hardButton) * 0.1;
        double hardTop = menuTop - menuGap - hardButton.prefHeight(-1);
        Bounds hardText = buttonTextBounds(hardButton);
        Bounds mediumText = buttonTextBounds(mediumButton);
        Bounds easyText = buttonTextBounds(easyButton);
        double mediumTop = hardTop + hardText.getMinY() - mediumText.getMaxY();
        double easyTop = mediumTop + mediumText.getMinY() - easyText.getMaxY();
        positionButton(menuButton, menuX, menuCenterY);
        positionButton(hideButton, menuX, menuCenterY);
        positionButton(hardButton, menuX, hardTop + hardButton.prefHeight(-1) / 2);
        positionButton(mediumButton, menuX, mediumTop + mediumButton.prefHeight(-1) / 2);
        positionButton(easyButton, menuX, easyTop + easyButton.prefHeight(-1) / 2);
    }

    private void centerText(Text text, double top) {
        Bounds bounds = text.getLayoutBounds();
        text.relocate((root.getWidth() - bounds.getWidth()) / 2, top);
    }

    private Bounds buttonTextBounds(Button button) {
        Node text = button.lookup(".text");
        return text == null || button.getScene() == null ? button.getLayoutBounds()
                : button.sceneToLocal(text.localToScene(text.getBoundsInLocal()));
    }

    private void dropTitle() {
        titleStarted = true;
        layoutScene();
        title.setOpacity(1);
        title.setTranslateY(-root.getHeight() - title.getLayoutBounds().getHeight());
        TranslateTransition drop = new TranslateTransition(Duration.seconds(1), title);
        drop.setToY(0);
        drop.setInterpolator(Interpolator.EASE_OUT);
        drop.setOnFinished(event -> {
            if (!activated) {
                startButton.setDisable(false);
                menuButton.setDisable(false);
                new ParallelTransition(fade(startButton, 1, 0.5),
                        fade(menuButton, 1, 0.5)).play();
            }
        });
        drop.play();
    }

    private void configureButton(Button button, Runnable action) {
        UiEffects.install(button);
        button.setTextFill(Color.WHITE);
        button.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; "
                + "-fx-border-color: transparent; -fx-padding: 0;");
        button.setOnMouseEntered(event -> {
            button.setFont(font(baseButtonFontSize(button) + 2));
        });
        button.setOnMouseExited(event -> button.setFont(font(baseButtonFontSize(button))));
        button.setOnAction(event -> {
            action.run();
        });
    }

    private void positionButton(Button button, double left, double centerY) {
        button.relocate(left, centerY - button.prefHeight(-1) / 2);
    }

    private double baseButtonFontSize(Button button) {
        double size = button == startButton ? buttonFontSize : Math.max(1, buttonFontSize - 5);
        return button == startButton || button == menuButton || button == hideButton
                ? size : size * 0.7;
    }

    private void transitionDifficultyMenu(boolean show) {
        menuButton.setDisable(true);
        hideButton.setDisable(true);
        for (Button button : difficultyButtons) {
            button.setDisable(true);
        }

        SequentialTransition transition = new SequentialTransition();
        if (show) {
            transition.getChildren().addAll(
                    fade(menuButton, 0, 0.2),
                    new PauseTransition(Duration.seconds(0.3)), fade(hideButton, 1, 0.2));
            transition.getChildren().addAll(fade(hardButton, 1, 0.18),
                    fade(mediumButton, 1, 0.18), fade(easyButton, 1, 0.18));
        } else {
            transition.getChildren().addAll(fade(easyButton, 0, 0.18),
                    fade(mediumButton, 0, 0.18), fade(hardButton, 0, 0.18));
            transition.getChildren().addAll(fade(hideButton, 0, 0.2),
                    new PauseTransition(Duration.seconds(0.3)),
                    fade(menuButton, 1, 0.2));
        }
        transition.setOnFinished(event -> {
            menuButton.setDisable(show);
            hideButton.setDisable(!show);
            for (Button button : difficultyButtons) {
                button.setDisable(!show);
            }
        });
        transition.play();
    }

    private void selectDifficulty(Button selected, Color color, String difficulty) {
        if (selectedDifficulty != null) {
            selectedDifficulty.setTextFill(Color.WHITE);
        }
        selectedDifficulty = selected;
        selectedDifficultyName = difficulty;
        selectedDifficulty.setTextFill(color);
    }

    private void startGame() {
        if (activated) {
            return;
        }
        activated = true;
        animation.stop();
        stop(wind);
        double menuPosition = menuTrack == null ? 0 : menuTrack.getCurrentTime().toSeconds();
        if (!Double.isFinite(menuPosition)) {
            menuPosition = 0;
        }
        stop(menuTrack);
        playMuffledFrom(Math.max(0, menuPosition - 2));
        onStart.accept(selectedDifficultyName);
    }

    private void playMuffledFrom(double seconds) {
        if (muffledTrack == null) {
            return;
        }
        muffledTrack.setCycleCount(1);
        Runnable playFromCue = () -> {
            if (!activated || muffledStarted) {
                return;
            }
            muffledStarted = true;
            muffledTrack.seek(Duration.seconds(seconds));
            muffledTrack.play();
        };
        muffledTrack.setOnReady(playFromCue);
        if (muffledTrack.getStatus() == MediaPlayer.Status.READY) {
            playFromCue.run();
        }
    }

    private FadeTransition fade(javafx.scene.Node node, double opacity, double seconds) {
        FadeTransition transition = new FadeTransition(Duration.seconds(seconds), node);
        transition.setToValue(opacity);
        return transition;
    }

    private Font font(double size) { return UiEffects.font(size); }

    private MediaPlayer createPlayer(String path) {
        URL url = resource(path);
        if (url == null) {
            LOG.warning("Missing audio resource " + path);
            return null;
        }
        try {
            Media media = new Media(url.toExternalForm());
            media.setOnError(() -> LOG.log(Level.WARNING, "Could not load audio " + path, media.getError()));
            MediaPlayer player = new MediaPlayer(media);
            player.setOnError(() -> LOG.log(Level.WARNING, "Audio playback failed for " + path, player.getError()));
            return player;
        } catch (RuntimeException error) {
            LOG.log(Level.WARNING, "Could not create audio player for " + path, error);
            return null;
        }
    }

    private URL resource(String path) {
        return StartMenuScreen.class.getResource(path);
    }

    private void stop(MediaPlayer player) {
        if (player != null) {
            player.stop();
        }
    }
}
