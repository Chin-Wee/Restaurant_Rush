package edu.ntu.sc2002.ui.scene;

import java.net.URL;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import edu.ntu.sc2002.application.contract.GameSnapshot;
import edu.ntu.sc2002.application.contract.HostTaskType;
import edu.ntu.sc2002.application.contract.WaiterTaskType;
import edu.ntu.sc2002.application.contract.ChefTaskType;
import edu.ntu.sc2002.domain.OrderStatus;
import edu.ntu.sc2002.domain.customer.CustomerState;
import edu.ntu.sc2002.domain.customer.CustomerType;
import edu.ntu.sc2002.presentation.RestaurantUiState.ResolvedTurn;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import edu.ntu.sc2002.ui.UiEffects;
import javafx.scene.text.TextAlignment;


/** Stardew Valley room layers, sprites and visual tile routes. No gameplay state. */
public final class RestaurantScene {
    private static final int TILE_SIZE = 64;
    private static final int COLUMNS = 1280 / TILE_SIZE;
    private static final int ROWS = 720 / TILE_SIZE;
    private static final int FLOOR_TOP_ROW = 2;
    private static final int PATCH_COLUMNS = 6;
    private static final int PATCH_ROWS = 3;
    private static final int DINING_ROW = 7;
    private static final int[] DINING_TABLE_COLUMNS = {4, 9};
    private static final int KITCHEN_LEFT_COLUMN = COLUMNS - PATCH_COLUMNS;
    private static final int KITCHEN_BOTTOM_ROW = FLOOR_TOP_ROW + PATCH_ROWS;
    public static final int ROOM_WIDTH = COLUMNS * TILE_SIZE;
    public static final int ROOM_HEIGHT = ROWS * TILE_SIZE;
    private static final int KITCHEN_SOURCE_ROW = 10;
    private static final Set<String> CUSTOMER_PEOPLE = Set.of("adam", "amelia", "bob");
    private static final double WALK_TILES_PER_SECOND = 2.8;
    private static final Font ACTOR_TAG_FONT = UiEffects.font(16);
    private static final long WALK_FRAME_NANOS = 120_000_000L;
    private static final long IDLE_FRAME_NANOS = 400_000_000L;
    private static final Tile CUSTOMER_SPAWN = new Tile(0, 5);
    private static final Tile WAITER_STATION = new Tile(12, 7);
    private static final int HOST_STATION_COLUMN = 5;
    private static final Tile HOST_STATION = new Tile(HOST_STATION_COLUMN, 4);
    private static final int MAX_VISIBLE_QUEUE = 3;
    private static final int QUEUE_FRONT_COLUMN = 4;
    private static final int QUEUE_SLOT_GAP = 2;
    private static final Set<String> STAFF = Set.of("chef", "waiter", "cashier", "host");
    private static final Tile STOVE_APPROACH = new Tile(16, 3);
    private static final Tile[] DISH_SPOTS = {
            new Tile(15, 4), new Tile(16, 4), new Tile(17, 4), new Tile(18, 4)
    };
    private static final String TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Room_Builder_free_16x16/";
    private static final String INTERIORS_TILE_PATH = "/Modern tiles_Free/Interiors_free/16x16/split-16x16/Interiors_free_16x16/";
    private static final String KITCHEN_TILE_PATH = "/Kitchen and more tileset [16x16]/split-16x16/tileset/";

    private final Pane root = new StackPane();
    private final Canvas floorLayer = new Canvas();
    private final Canvas actorLayer = new Canvas();
    private final Canvas furnitureLayer = new Canvas();
    private final Canvas uiLayer = new Canvas();
    private final Image wood = loadTile("c04_r13.png");
    private final Image trim = loadTile("c00_r12.png");
    private final Image gray = loadTile("c05_r17.png");
    private final Image kitchenLeftEdge = loadInteriorsTile("c00_r07.png");
    private final Image kitchenBottomLeftCorner = loadInteriorsTile("c00_r08.png");
    private final Image kitchenBottomEdge = loadInteriorsTile("c01_r08.png");
    private final Image kitchenBottomInsideTile = loadInteriorsTile("c03_r09.png");
    private final Image kitchenAboveBottomInsideTile = loadInteriorsTile("c03_r08.png");
    private final Image tileRightOfComputer = loadInteriorsTile("c03_r05.png");
    private final Image diningTable = loadInteriorsTile("c06_r13.png");
    private final Image diningChairTop = loadInteriorsTile("c06_r21.png");
    private final Image diningChairBottom = loadInteriorsTile("c06_r22.png");
    private final Image[][] kitchenChunk = loadKitchenChunk();
    private static final boolean[][] MOVEMENT_AREA = createMovementArea();
    private final Actor chef = new Actor(new Tile(17, 4), loadDirectionalFrames("adam", "idle", 6),
            loadDirectionalFrames("adam", "run", 6), null, Direction.UP);
    private final Actor waiter = new Actor(WAITER_STATION, loadDirectionalFrames("alex", "idle", 6),
            loadDirectionalFrames("alex", "run", 6), null, Direction.LEFT);
    private final Actor cashier = new Actor(new Tile(14, 4), loadSingleDirection("amelia", "idle", "down", 6),
            null, null, Direction.DOWN);
    /** Stage 3 host who escorts newly seated guests, then returns to the stand. */
    private final Actor host = new Actor(HOST_STATION, loadDirectionalFrames("bob", "idle", 6),
            loadDirectionalFrames("bob", "run", 6), null, Direction.DOWN);
    private final Map<String, Actor> actors = new LinkedHashMap<>();
    private Map<String, Placement> customerPlacements = Map.of();
    private Map<String, CustomerType> customerTypes = Map.of();
    private int visibleQueueCount;
    private int hiddenQueueCount;
    private final Set<String> exiting = new HashSet<>();
    private final Map<String, Dish> dishes = new LinkedHashMap<>();
    private long previousAnimationNanos;
    private final AnimationTimer actorAnimation = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double delta = previousAnimationNanos == 0 ? 0
                    : Math.min((now - previousAnimationNanos) / 1_000_000_000.0, 0.1);
            previousAnimationNanos = now;
            advanceActors(delta);
            drawActors(now);
            drawUi();
        }
    };

    /** Creates the room layers, loads visual assets and starts the actor animation when attached. */
    public RestaurantScene() {
        actors.put("chef", chef);
        actors.put("waiter", waiter);
        actors.put("cashier", cashier);
        actors.put("host", host);
        root.setMinSize(0, 0);
        root.setStyle("-fx-background-color: #252B3D;");
        floorLayer.setId("floor-layer");
        actorLayer.setId("actor-layer");
        furnitureLayer.setId("furniture-layer");
        floorLayer.setMouseTransparent(true);
        actorLayer.setMouseTransparent(true);
        furnitureLayer.setMouseTransparent(true);
        floorLayer.widthProperty().bind(root.widthProperty());
        floorLayer.heightProperty().bind(root.heightProperty());
        actorLayer.widthProperty().bind(root.widthProperty());
        actorLayer.heightProperty().bind(root.heightProperty());
        furnitureLayer.widthProperty().bind(root.widthProperty());
        furnitureLayer.heightProperty().bind(root.heightProperty());
        uiLayer.widthProperty().bind(root.widthProperty());
        uiLayer.heightProperty().bind(root.heightProperty());
        uiLayer.setMouseTransparent(true);
        root.getChildren().addAll(floorLayer, actorLayer, furnitureLayer, uiLayer);

        root.widthProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.heightProperty().addListener((observable, oldValue, newValue) -> drawLayers());
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                actorAnimation.stop();
                previousAnimationNanos = 0;
            } else {
                actorAnimation.start();
            }
        });
    }

    /** @return the room's root pane */
    public Pane getRoot() { return root; }

    /** Stops the actor animation and clears its frame clock. */
    public void dispose() { actorAnimation.stop(); previousAnimationNanos = 0; }

    /** Reconcile visual targets only; no animation callback can change an engine snapshot. */
    public void showSnapshot(GameSnapshot snapshot) {
        showSnapshot(snapshot, Map.of());
    }

    private void showSnapshot(GameSnapshot snapshot, Map<String, Actor> seaters) {
        Map<String, Placement> targets = customerTargets(snapshot);
        Set<String> activeIds = new HashSet<>();
        int visible = 0;
        int hidden = 0;
        for (GameSnapshot.CustomerView customer : snapshot.customers()) {
            activeIds.add(customer.id());
            if (customer.state() == CustomerState.WAITING && customer.queuePosition() != null) {
                if (customer.queuePosition() <= MAX_VISIBLE_QUEUE) visible++;
                else hidden++;
            }
        }
        visibleQueueCount = visible;
        hiddenQueueCount = hidden;
        Iterator<Map.Entry<String, Actor>> iterator = actors.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String id = entry.getKey();
            if (STAFF.contains(id) || targets.containsKey(id)) continue;
            if (activeIds.contains(id)) {
                iterator.remove();
                continue;
            }
            if (exiting.add(id)) {
                Actor guest = entry.getValue();
                List<Tile> exitPath = new ArrayList<>(requirePath(guest.tile(), CUSTOMER_SPAWN));
                exitPath.add(new Tile(-1, CUSTOMER_SPAWN.row));
                guest.follow(exitPath, false);
            }
        }
        for (var entry : targets.entrySet()) {
            String id = entry.getKey();
            Placement placement = entry.getValue();
            Actor guest = actors.computeIfAbsent(id, key -> customer(placement.person(), CUSTOMER_SPAWN));
            if (placement.seated()) {
                if (!placement.equals(customerPlacements.get(id))) {
                    // The staff member who seated the guest escorts them, then returns to their stand.
                    Actor seater = seaters.getOrDefault(id, host);
                    Tile station = seater == waiter ? WAITER_STATION : HOST_STATION;
                    Runnable rest = seater == waiter ? waiter::faceLeft : host::faceDown;
                    guest.requestDestination(guest.tile(), false);
                    seater.walkTo(hostApproach(guest.tile(), targets), () -> {
                        if (placement.equals(customerPlacements.get(id))) {
                            guest.requestDestination(placement.tile(), true);
                            guest.walkDelay = 1.0;
                        }
                    });
                    seater.walkTo(new Tile(placement.tile().column - 1, placement.tile().row),
                            () -> seater.walkTo(station, rest));
                }
            } else {
                guest.requestQueueDestination(placement.tile());
            }
        }
        customerPlacements = targets;
        Map<String, CustomerType> types = new HashMap<>();
        snapshot.customers().forEach(c -> types.put(c.id(), c.type()));
        customerTypes = Map.copyOf(types);
        Set<String> activeOrders = new HashSet<>();
        snapshot.orders().stream().filter(o -> o.status() == OrderStatus.READY || o.status() == OrderStatus.SERVED)
                .forEach(order -> {
                    activeOrders.add(order.customerId());
                    Integer tableId = tableFor(snapshot, order.customerId());
                    if (tableId != null && order.status() == OrderStatus.READY)
                        dishes.computeIfAbsent(order.customerId(), id -> new Dish(tableId));
                });
        dishes.keySet().retainAll(activeOrders);
    }

    /** Called once per completed turn by the screen. No engine callbacks or revision counters. */
    public void playTurn(ResolvedTurn turn, GameSnapshot nextSnapshot) {
        Map<String, Actor> seaters = new HashMap<>();
        if (turn.host() != null && turn.host().taskType() == HostTaskType.SEAT) {
            seaters.put(turn.host().targetCustomerId(), host);
        }
        if (turn.waiter().decision().taskType() == WaiterTaskType.SEAT) {
            seaters.put(turn.waiter().decision().targetCustomerId(), waiter);
        }
        showSnapshot(nextSnapshot, seaters);
        Integer waiterTable = tableFor(turn.before(), turn.waiter().customerId());
        if (waiterTable != null && turn.waiter().decision().taskType() != WaiterTaskType.WAIT
                && turn.waiter().decision().taskType() != WaiterTaskType.SEAT) {
            Tile chair = chairFor(waiterTable);
            if (turn.waiter().decision().taskType() == WaiterTaskType.SERVE) {
                String customerId = turn.waiter().customerId();
                Dish dish = dishes.computeIfAbsent(customerId, id -> new Dish(waiterTable));
                waiter.walkTo(counterApproach(chair), () -> {
                    if (dishes.get(customerId) == dish) dish.place = DishPlace.CARRIED;
                });
                waiter.walkTo(new Tile(chair.column - 1, chair.row), () -> {
                    if (dishes.get(customerId) == dish) dish.place = DishPlace.TABLE;
                    waiter.walkTo(WAITER_STATION, waiter::faceLeft);
                });
            } else {
                waiter.walkTo(new Tile(chair.column - 1, chair.row),
                        () -> waiter.walkTo(WAITER_STATION, waiter::faceLeft));
            }
        }
        Integer chefTable = tableFor(turn.before(), turn.chefCustomerId());
        if (chefTable != null && turn.chef().taskType() == ChefTaskType.ADVANCE) {
            chef.walkTo(STOVE_APPROACH, null);
            chef.walkTo(DISH_SPOTS[chefTable - 1], null);
            chef.walkTo(STOVE_APPROACH, null);
        }
    }

    private static Integer tableFor(GameSnapshot snapshot, String id) {
        return snapshot.customers().stream().filter(c -> c.id().equals(id)).findFirst()
                .map(GameSnapshot.CustomerView::tableId).orElse(null);
    }

    private static Tile counterApproach(Tile chair) {
        int index = chair.column == DINING_TABLE_COLUMNS[0] - 1 ? 0 : 1;
        return new Tile(DISH_SPOTS[index].column, KITCHEN_BOTTOM_ROW + 2);
    }

    /** Picks a free neighbouring tile for the Host so sprites never stack on the guest's tile. */
    private static Tile hostApproach(Tile guest, Map<String, Placement> targets) {
        Set<Tile> occupied = new HashSet<>();
        targets.values().forEach(placement -> occupied.add(placement.tile()));
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            Tile candidate = new Tile(guest.column + offset[0], guest.row + offset[1]);
            if (inside(candidate) && MOVEMENT_AREA[candidate.row][candidate.column]
                    && !occupied.contains(candidate)) {
                return candidate;
            }
        }
        return guest;
    }

    /** Visual time only. Departure removes a sprite after it has crossed the room edge. */
    private void advanceActors(double delta) {
        for (Actor actor : actors.values()) actor.advance(delta);
        actors.entrySet().removeIf(entry -> {
            if (!exiting.contains(entry.getKey()) || entry.getValue().isMoving()) return false;
            exiting.remove(entry.getKey());
            return true;
        });
    }

    private static Map<String, Placement> customerTargets(GameSnapshot snapshot) {
        Map<String, Placement> targets = new LinkedHashMap<>();
        String[] people = {"bob", "adam", "amelia"};
        for (GameSnapshot.CustomerView customer : snapshot.customers()) {
            if (customer.state() == CustomerState.PAID || customer.state() == CustomerState.LEFT) continue;
            if (customer.tableId() == null && customer.queuePosition() != null
                    && customer.queuePosition() > MAX_VISIBLE_QUEUE) continue;
            Tile target;
            if (customer.tableId() != null) {
                target = chairFor(customer.tableId());
            } else {
                int queuePosition = Objects.requireNonNull(customer.queuePosition());
                target = new Tile(QUEUE_FRONT_COLUMN - QUEUE_SLOT_GAP * (queuePosition - 1), CUSTOMER_SPAWN.row);
            }
            targets.put(customer.id(), new Placement(people[Math.floorMod(customer.arrivalOrdinal() - 1, people.length)],
                    target, customer.tableId() != null));
        }
        return Map.copyOf(targets);
    }

    private static Tile chairFor(int tableId) {
        if (tableId < 1 || tableId > DINING_TABLE_COLUMNS.length) throw new IllegalArgumentException("Unknown visual table: " + tableId);
        return new Tile(DINING_TABLE_COLUMNS[tableId - 1] - 1, DINING_ROW);
    }

    private record Placement(String person, Tile tile, boolean seated) {}

    private static Image loadTile(String filename) {
        return new Image(Objects.requireNonNull(RestaurantScene.class.getResource(TILE_PATH + filename),
                "Missing tile: " + filename).toExternalForm(), false);
    }

    private static Image loadInteriorsTile(String filename) {
        return new Image(Objects.requireNonNull(RestaurantScene.class.getResource(INTERIORS_TILE_PATH + filename),
                "Missing tile: " + filename).toExternalForm(), false);
    }

    private static Image[][] loadKitchenChunk() {
        Image[][] tiles = new Image[PATCH_ROWS][PATCH_COLUMNS];
        for (int row = 0; row < PATCH_ROWS; row++) {
            for (int column = 0; column < PATCH_COLUMNS; column++) {
                String filename = String.format("c%02d_r%02d.png", column, KITCHEN_SOURCE_ROW + row);
                URL resource = RestaurantScene.class.getResource(KITCHEN_TILE_PATH + filename);
                if (resource != null) {
                    tiles[row][column] = new Image(resource.toExternalForm(), false);
                }
            }
        }
        return tiles;
    }

    private void drawLayers() {
        drawFloor();
        long now = System.nanoTime();
        drawActors(now);
        drawFurniture();
        drawUi();
    }

    private static Image[] loadFrames(String person, String action, String direction, int count) {
        Image[] frames = new Image[count];
        String path = "/characters/" + person + "/" + action + "/" + direction + "/frame-%02d.png";
        for (int frame = 0; frame < count; frame++) {
            URL resource = RestaurantScene.class.getResource(String.format(path, frame));
            frames[frame] = new Image(Objects.requireNonNull(resource, "Missing character frame: " + path)
                    .toExternalForm(), false);
        }
        return frames;
    }

    private static Image[][] loadSingleDirection(String person, String action, String direction, int count) {
        Image[][] frames = new Image[Direction.values().length][];
        Direction facing = Direction.fromName(direction);
        frames[facing.ordinal()] = loadFrames(person, action, direction, count);
        return frames;
    }

    private static Image[][] loadDirectionalFrames(String person, String action, int count) {
        Image[][] frames = new Image[Direction.values().length][];
        for (Direction direction : Direction.values()) {
            frames[direction.ordinal()] = loadFrames(person, action, direction.path, count);
        }
        return frames;
    }

    private static Actor customer(String person, Tile spawn) {
        if (!CUSTOMER_PEOPLE.contains(person)) {
            throw new IllegalArgumentException("Not an allowed customer: " + person);
        }
        Actor actor = new Actor(spawn, loadDirectionalFrames(person, "idle", 6),
                loadDirectionalFrames(person, "run", 6), loadFrames(person, "sit3", "right", 3), Direction.RIGHT);
        return actor;
    }

    private static boolean[][] createMovementArea() {
        boolean[][] movement = new boolean[ROWS][COLUMNS];
        for (int row = FLOOR_TOP_ROW; row < ROWS; row++) {
            Arrays.fill(movement[row], true);
        }
        for (int column = 0; column < KITCHEN_LEFT_COLUMN; column++) {
            block(movement, column, FLOOR_TOP_ROW);
        }
        for (int row = 0; row < PATCH_ROWS; row++) {
            for (int column = KITCHEN_LEFT_COLUMN; column < COLUMNS; column++) {
                block(movement, column, row);
            }
        }
        for (int row = FLOOR_TOP_ROW + 1; row <= KITCHEN_BOTTOM_ROW; row++) {
            block(movement, KITCHEN_LEFT_COLUMN - 1, row);
        }
        block(movement, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW - 1);
        for (int column = KITCHEN_LEFT_COLUMN; column < COLUMNS; column++) {
            block(movement, column, KITCHEN_BOTTOM_ROW);
        }
        for (int tableColumn : DINING_TABLE_COLUMNS) {
            int chairColumn = tableColumn - 1;
            block(movement, chairColumn, DINING_ROW - 1);
            block(movement, chairColumn, DINING_ROW);
            block(movement, tableColumn, DINING_ROW);
        }
        return movement;
    }

    private static void block(boolean[][] movement, int column, int row) {
        movement[row][column] = false;
    }

    private static List<Tile> requirePath(Tile start, Tile destination) {
        List<Tile> path = findPath(start, destination);
        if (path.isEmpty()) {
            throw new IllegalStateException("No movement path from " + start + " to " + destination);
        }
        return path;
    }

    // Stardew Valley's PathFindController uses four-way A* with a Manhattan heuristic for tile routes.
    private static List<Tile> findPath(Tile start, Tile destination) {
        if (!inside(start) || !inside(destination) || (!MOVEMENT_AREA[start.row][start.column]
                && Arrays.stream(DINING_TABLE_COLUMNS).noneMatch(c -> start.column == c - 1 && start.row == DINING_ROW))) {
            return List.of();
        }
        Comparator<SearchNode> priority = Comparator.comparingInt(SearchNode::score)
                .thenComparingLong(SearchNode::order);
        PriorityQueue<SearchNode> open = new PriorityQueue<>(priority);
        Map<Tile, Integer> costs = new HashMap<>();
        Map<Tile, Tile> previous = new HashMap<>();
        Set<Tile> closed = new HashSet<>();
        long order = 0;
        costs.put(start, 0);
        open.add(new SearchNode(start, 0, manhattan(start, destination), order++));

        while (!open.isEmpty()) {
            SearchNode current = open.remove();
            if (current.cost != costs.getOrDefault(current.tile, Integer.MAX_VALUE) || !closed.add(current.tile)) {
                continue;
            }
            if (current.tile.equals(destination)) {
                return reconstructPath(start, destination, previous);
            }
            for (Direction direction : Direction.values()) {
                Tile next = new Tile(current.tile.column + direction.dx, current.tile.row + direction.dy);
                if (!inside(next) || (!MOVEMENT_AREA[next.row][next.column] && !next.equals(destination))
                        || closed.contains(next)) {
                    continue;
                }
                int nextCost = current.cost + 1;
                if (nextCost < costs.getOrDefault(next, Integer.MAX_VALUE)) {
                    costs.put(next, nextCost);
                    previous.put(next, current.tile);
                    open.add(new SearchNode(next, nextCost, nextCost + manhattan(next, destination), order++));
                }
            }
        }
        return List.of();
    }

    private static List<Tile> reconstructPath(Tile start, Tile destination, Map<Tile, Tile> previous) {
        List<Tile> path = new ArrayList<>();
        for (Tile tile = destination; tile != null; tile = previous.get(tile)) {
            path.add(tile);
            if (tile.equals(start)) {
                Collections.reverse(path);
                return path;
            }
        }
        return List.of();
    }

    private static boolean inside(Tile tile) {
        return tile.column >= 0 && tile.column < COLUMNS && tile.row >= 0 && tile.row < ROWS;
    }

    private static int manhattan(Tile left, Tile right) {
        return Math.abs(left.column - right.column) + Math.abs(left.row - right.row);
    }

    private void drawActors(long now) {
        drawLayer(actorLayer, graphics -> {
            for (Actor actor : actors.values()) {
                graphics.drawImage(actor.frame(now), actor.column * TILE_SIZE,
                        (actor.row - 1) * TILE_SIZE, TILE_SIZE, TILE_SIZE * 2);
            }
        });
    }

    private void drawUi() {
        drawLayer(uiLayer, graphics -> {
            drawDishes(graphics);
            graphics.setFont(ACTOR_TAG_FONT);
            graphics.setTextAlign(TextAlignment.CENTER);
            drawActorTag(graphics, chef, "Adam");
            drawActorTag(graphics, waiter, "Alex");
            drawActorTag(graphics, cashier, "Amelia");
            drawActorTag(graphics, host, "Host");
            actors.forEach((id, actor) -> {
                if (actor != chef && actor != waiter && actor != cashier && actor != host) {
                    drawCustomerTag(graphics, actor, id);
                }
            });
            if (hiddenQueueCount > 0) {
                drawText(graphics, "+" + hiddenQueueCount + " waiting",
                        0.5 * TILE_SIZE,
                        (CUSTOMER_SPAWN.row - 1) * TILE_SIZE - 40, Color.web("#FFD54A"));
            }
        });
    }

    private static void drawWhiteText(GraphicsContext graphics, String label, double x, double y) {
        drawText(graphics, label, x, y, Color.WHITE);
    }

    private static void drawText(GraphicsContext graphics, String label, double x, double y, Color color) {
        graphics.setFill(Color.web("#252B3D"));
        graphics.fillText(label, x + 1, y + 1);
        graphics.setFill(color);
        graphics.fillText(label, x, y);
    }

    /** Regular guests show their ID; VIPs are tagged gold and Critics red so the player can spot them in the queue. */
    private void drawCustomerTag(GraphicsContext graphics, Actor actor, String id) {
        CustomerType type = customerTypes.getOrDefault(id, CustomerType.REGULAR);
        String label = type == CustomerType.REGULAR ? id : id + " " + type;
        Color color = switch (type) {
            case VIP -> Color.web("#FFD54A");
            case CRITIC -> Color.web("#FF6B6B");
            case REGULAR -> Color.WHITE;
        };
        drawText(graphics, label, actor.column * TILE_SIZE + TILE_SIZE / 2, (actor.row - 1) * TILE_SIZE - 8, color);
    }

    private static void drawActorTag(GraphicsContext graphics, Actor actor, String label) {
        drawWhiteText(graphics, label, actor.column * TILE_SIZE + TILE_SIZE / 2,
                (actor.row - 1) * TILE_SIZE - 8);
    }

    private void drawFloor() {
        drawLayer(floorLayer, graphics -> {
            for (int row = FLOOR_TOP_ROW; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    Image tile = row < FLOOR_TOP_ROW + PATCH_ROWS
                            && column >= KITCHEN_LEFT_COLUMN
                            && column < KITCHEN_LEFT_COLUMN + PATCH_COLUMNS ? gray : wood;
                    drawTile(graphics, tile, column, row);
                }
            }
            for (int tableColumn : DINING_TABLE_COLUMNS) {
                int chairColumn = tableColumn - 1;
                drawTile(graphics, diningChairTop, chairColumn, DINING_ROW - 1);
                drawTile(graphics, diningChairBottom, chairColumn, DINING_ROW);
            }
        });
    }

    private void drawFurniture() {
        drawLayer(furnitureLayer, graphics -> {
            for (int column = 0; column < KITCHEN_LEFT_COLUMN; column++) {
                drawTile(graphics, trim, column, FLOOR_TOP_ROW);
            }
            int borderColumn = KITCHEN_LEFT_COLUMN - 1;
            for (int row = FLOOR_TOP_ROW + 1; row < KITCHEN_BOTTOM_ROW; row++) {
                drawTile(graphics, kitchenLeftEdge, borderColumn, row);
            }
            drawTile(graphics, kitchenBottomLeftCorner, borderColumn, KITCHEN_BOTTOM_ROW);
            drawTile(graphics, kitchenAboveBottomInsideTile, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW - 1);
            drawTile(graphics, kitchenBottomInsideTile, KITCHEN_LEFT_COLUMN, KITCHEN_BOTTOM_ROW);
            for (int column = KITCHEN_LEFT_COLUMN + 1; column < COLUMNS - 1; column++) {
                drawTile(graphics, tileRightOfComputer, column, KITCHEN_BOTTOM_ROW);
            }
            drawTile(graphics, kitchenBottomEdge, COLUMNS - 1, KITCHEN_BOTTOM_ROW);
            for (int row = 0; row < PATCH_ROWS; row++) {
                int destinationRow = FLOOR_TOP_ROW + row - PATCH_ROWS + 1;
                for (int column = 0; column < PATCH_COLUMNS; column++) {
                    Image tile = kitchenChunk[row][column];
                    if (tile != null) {
                        int destinationColumn = KITCHEN_LEFT_COLUMN + column;
                        drawTile(graphics, tile, destinationColumn, destinationRow);
                    }
                }
            }
            for (int tableColumn : DINING_TABLE_COLUMNS) {
                drawTile(graphics, diningTable, tableColumn, DINING_ROW);
            }
        });
    }

    private void drawDishes(GraphicsContext graphics) {
        for (Dish dish : dishes.values()) {
            Tile counter = DISH_SPOTS[dish.tableId - 1];
            double x = switch (dish.place) {
                case COUNTER -> counter.column * TILE_SIZE;
                case CARRIED -> waiter.column * TILE_SIZE;
                case TABLE -> DINING_TABLE_COLUMNS[dish.tableId - 1] * TILE_SIZE;
            };
            double y = switch (dish.place) {
                case COUNTER -> (counter.row + 1) * TILE_SIZE;
                case CARRIED -> waiter.row * TILE_SIZE - 4;
                case TABLE -> DINING_ROW * TILE_SIZE;
            };
            graphics.setFill(Color.web("#F3EFDE"));
            graphics.fillOval(x + 18, y + 18, 28, 23);
            graphics.setStroke(Color.web("#737985"));
            graphics.setLineWidth(2);
            graphics.strokeOval(x + 18, y + 18, 28, 23);
            graphics.setFill(Color.web("#D98749"));
            graphics.fillOval(x + 26, y + 24, 12, 10);
        }
    }

    private void drawLayer(Canvas layer, Consumer<GraphicsContext> drawTiles) {
        double width = layer.getWidth();
        double height = layer.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double mapWidth = COLUMNS * TILE_SIZE;
        double mapHeight = ROWS * TILE_SIZE;
        double scale = Math.min(width / mapWidth, height / mapHeight);
        GraphicsContext graphics = layer.getGraphicsContext2D();
        graphics.clearRect(0, 0, width, height);
        graphics.save();
        graphics.setImageSmoothing(false);
        graphics.translate((width - mapWidth * scale) / 2, (height - mapHeight * scale) / 2);
        graphics.scale(scale, scale);
        drawTiles.accept(graphics);
        graphics.restore();
    }

    private static void drawTile(GraphicsContext graphics, Image tile, int column, int row) {
        graphics.drawImage(tile, column * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    private enum DishPlace { COUNTER, CARRIED, TABLE }
    private static final class Dish {
        private final int tableId;
        private DishPlace place = DishPlace.COUNTER;
        private Dish(int tableId) { this.tableId = tableId; }
    }

    private static final class Actor {
        private final Image[][] idleFrames;
        private final Image[][] runFrames;
        private final Image[] sittingFrames;
        private double column;
        private double row;
        private Direction direction;
        private List<Tile> path = List.of();
        private int nextPathIndex;
        private double progress;
        private boolean sitOnArrival;
        private boolean seated;
        private Tile destination;
        private boolean requestedSit;
        private final ArrayDeque<Walk> walks = new ArrayDeque<>();
        private Runnable onArrival;
        private double walkDelay;
        private boolean faceRightWhenIdle;

        private Actor(Tile start, Image[][] idleFrames, Image[][] runFrames, Image[] sittingFrames,
                Direction direction) {
            this.idleFrames = idleFrames;
            this.runFrames = runFrames;
            this.sittingFrames = sittingFrames;
            this.direction = direction;
            column = start.column;
            row = start.row;
        }

        private Tile tile() {
            return new Tile((int) Math.round(column), (int) Math.round(row));
        }

        private void requestDestination(Tile target, boolean sit) {
            if (target.equals(destination) && sit == requestedSit) return;
            follow(requirePath(tile(), target), sit);
        }

        /** Queue guests always look towards the counter once they stop moving. */
        private void requestQueueDestination(Tile target) {
            requestDestination(target, false);
            faceRightWhenIdle = true;
        }

        private void faceDown() {
            direction = Direction.DOWN;
        }

        private void faceLeft() {
            direction = Direction.LEFT;
        }

        private void walkTo(Tile target, Runnable arrived) {
            walks.addLast(new Walk(target, arrived));
            if (!isMoving() && onArrival == null) startNextWalk();
        }

        private void startNextWalk() {
            if (walks.isEmpty()) return;
            Walk walk = walks.removeFirst();
            follow(requirePath(tile(), walk.target()), false);
            onArrival = walk.arrived();
        }

        private void follow(List<Tile> path, boolean sitOnArrival) {
            walkDelay = 0;
            faceRightWhenIdle = false;
            destination = path.get(path.size() - 1);
            requestedSit = sitOnArrival;
            column = path.get(0).column;
            row = path.get(0).row;
            this.path = path;
            nextPathIndex = path.size() > 1 ? 1 : path.size();
            progress = 0;
            this.sitOnArrival = sitOnArrival;
            seated = path.size() == 1 && sitOnArrival;
            if (seated) {
                direction = Direction.RIGHT;
            }
        }

        private boolean isMoving() {
            return nextPathIndex < path.size();
        }

        private boolean advance(double delta) {
            if (walkDelay > 0) {
                walkDelay = Math.max(0, walkDelay - delta);
                if (walkDelay > 1e-9) return false;
            }
            if (!isMoving()) {
                if (onArrival != null) {
                    Runnable arrived = onArrival;
                    onArrival = null;
                    arrived.run();
                }
                startNextWalk();
            }
            if (!isMoving() || delta <= 0) {
                if (faceRightWhenIdle) direction = Direction.RIGHT;
                return false;
            }
            Tile from = path.get(nextPathIndex - 1);
            Tile to = path.get(nextPathIndex);
            direction = Direction.between(from, to);
            progress = Math.min(1, progress + delta * WALK_TILES_PER_SECOND);
            column = from.column + (to.column - from.column) * progress;
            row = from.row + (to.row - from.row) * progress;
            if (progress < 1) {
                return false;
            }
            nextPathIndex++;
            progress = 0;
            if (isMoving()) {
                return false;
            }
            path = List.of();
            nextPathIndex = 0;
            if (sitOnArrival) {
                seated = true;
                direction = Direction.RIGHT;
            } else if (faceRightWhenIdle) {
                direction = Direction.RIGHT;
            }
            return true;
        }

        private Image frame(long now) {
            Image[] frames;
            long frameDuration;
            if (seated) {
                frames = sittingFrames;
                frameDuration = IDLE_FRAME_NANOS;
            } else if (isMoving() && walkDelay <= 1e-9 && runFrames != null) {
                frames = runFrames[direction.ordinal()];
                frameDuration = WALK_FRAME_NANOS;
            } else {
                frames = idleFrames[direction.ordinal()];
                if (frames == null) {
                    frames = Arrays.stream(idleFrames).filter(Objects::nonNull).findFirst().orElseThrow();
                }
                frameDuration = IDLE_FRAME_NANOS;
            }
            return frames[(int) ((now / frameDuration) % frames.length)];
        }
    }

    private record Walk(Tile target, Runnable arrived) {}

    private record Tile(int column, int row) {}

    private record SearchNode(Tile tile, int cost, int score, long order) {}

    private enum Direction {
        RIGHT(1, 0, "right"),
        UP(0, -1, "up"),
        LEFT(-1, 0, "left"),
        DOWN(0, 1, "down");

        private final int dx;
        private final int dy;
        private final String path;

        Direction(int dx, int dy, String path) {
            this.dx = dx;
            this.dy = dy;
            this.path = path;
        }

        private static Direction fromName(String name) {
            return Arrays.stream(values()).filter(direction -> direction.path.equals(name)).findFirst().orElseThrow();
        }

        private static Direction between(Tile from, Tile to) {
            int dx = to.column - from.column;
            int dy = to.row - from.row;
            return Arrays.stream(values()).filter(direction -> direction.dx == dx && direction.dy == dy)
                    .findFirst().orElseThrow();
        }
    }
}
