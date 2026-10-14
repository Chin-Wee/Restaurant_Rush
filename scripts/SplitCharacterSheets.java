import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public final class SplitCharacterSheets {
    private static final Path SOURCE = Path.of("Assets/Modern tiles_Free/Characters_free");
    private static final Path OUTPUT = Path.of("src/main/resources/characters");
    private static final List<String> PEOPLE = List.of("Adam", "Alex", "Amelia", "Bob");
    private static final List<String> CUSTOMERS = List.of("Adam", "Amelia", "Bob");
    private static final List<String> DIRECTIONS = List.of("right", "up", "left", "down");
    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 32;
    private static final int EXPECTED_OUTPUTS = 228;

    private SplitCharacterSheets() {}

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT);
        int written = 0;
        for (String person : PEOPLE) {
            written += split(person, "idle_anim", "idle", 6);
            written += split(person, "run", "run", 6);
            if (CUSTOMERS.contains(person)) {
                written += split(person, "sit3", "sit3", 3);
            }
        }
        if (written != EXPECTED_OUTPUTS) {
            throw new IOException("Expected to write " + EXPECTED_OUTPUTS + " frames, wrote " + written);
        }
        Files.copy(Path.of("Assets/Modern tiles_Free/LICENSE.txt"), OUTPUT.resolve("LICENSE.txt"),
                StandardCopyOption.REPLACE_EXISTING);
        verifyOutput();
        System.out.println("Wrote and verified " + EXPECTED_OUTPUTS + " 16x32 frames in " + OUTPUT);
    }

    private static int split(String person, String sourceAction, String outputAction, int framesPerDirection)
            throws IOException {
        Path source = SOURCE.resolve(person + "_" + sourceAction + "_16x16.png");
        BufferedImage sheet = ImageIO.read(source.toFile());
        int frameCount = DIRECTIONS.size() * framesPerDirection;
        if (sheet == null || sheet.getWidth() != frameCount * FRAME_WIDTH || sheet.getHeight() != FRAME_HEIGHT) {
            throw new IOException("Unexpected sheet dimensions/frame count for " + source + ": expected "
                    + (frameCount * FRAME_WIDTH) + "x" + FRAME_HEIGHT + " (" + frameCount + " frames)");
        }

        for (int direction = 0; direction < DIRECTIONS.size(); direction++) {
            Path directory = OUTPUT.resolve(person.toLowerCase()).resolve(outputAction).resolve(DIRECTIONS.get(direction));
            Files.createDirectories(directory);
            for (int frame = 0; frame < framesPerDirection; frame++) {
                int sourceFrame = direction * framesPerDirection + frame;
                Path output = directory.resolve(String.format("frame-%02d.png", frame));
                if (!ImageIO.write(sheet.getSubimage(sourceFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT),
                        "png", output.toFile())) {
                    throw new IOException("PNG writer unavailable for " + output);
                }
            }
        }
        return frameCount;
    }

    private static void verifyOutput() throws IOException {
        int count = 0;
        try (Stream<Path> paths = Files.walk(OUTPUT)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".png")).toList()) {
                BufferedImage frame = ImageIO.read(path.toFile());
                if (frame == null || frame.getWidth() != FRAME_WIDTH || frame.getHeight() != FRAME_HEIGHT) {
                    throw new IOException("Invalid output frame: " + path);
                }
                count++;
            }
        }
        if (count != EXPECTED_OUTPUTS) {
            throw new IOException("Expected " + EXPECTED_OUTPUTS + " output frames, found " + count);
        }
    }
}
