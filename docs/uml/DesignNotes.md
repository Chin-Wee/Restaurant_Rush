# Restaurant Rush — UML diagrams (Stage 3 design)

> Historical design notes for the final stage. They are not the Stage 1 behavioral specification. Some names below (SimulationConfig, ManagerChoices, ResultEvaluator and HappyHourPolicy) predate the supplied class diagram and do not name Stage 1 classes. The older shared-Random selection description also differs from the documented per-arrival seed used in Stage 1. See [the parity audit](../Stage1-Parity-Audit.md), [exact API differences](../Stage1-Api-Differences.md), and [Stage 1 test notes](../Stage1-TestNotes.md).

Two draw.io files:

| File | Content |
|---|---|
| `RestaurantRush_ClassDiagram.drawio` | Final (Stage 3) class diagram: boundary, control, entity classes, enums, associations with multiplicities and the numbered annotations ①–⑦ |
| `RestaurantRush_SequenceDiagram.drawio` | One complete scenario (turns 1–6 + the result check after turn 18) with the manager's choice that changes the outcome, the VIP/Happy Hour behaviour, guards and frames |

Open with draw.io (desktop or app.diagrams.net → File → Open) and export as PNG/PDF for the report.
For a very large figure, draw.io's PDF export supports "Multiple Pages" so the tall sequence
diagram can be split across A4/A3 pages.

## Documented rules the diagrams assume

These are the free choices the brief leaves open; they are applied consistently in both figures.

* **Customer food choice** — `RegularCustomer.chooseItem()` and `VIPCustomer.chooseItem()` pick uniformly
  at random from all menu items (dishes **and** combos) using the single `Random(randomSeed)` created by
  `SimulationConfig` (base run: seed 2026). `CriticCustomer.chooseItem()` is fully deterministic: the most
  expensive item on the menu (ties broken alphabetically) — no randomness at all.
* **Critic special behaviour** — order preference as above, plus a service reaction in `onServed()`:
  +5 satisfaction (capped at 100) if the critic is served on the **first turn after its dish became READY**.
* **ComboMeal** — two *different* existing menu items; price = 90% of the component prices rounded to the
  nearest cent; preparation units = the sum. Salad + Pasta → $24.30, 3 units.
* **Price pipeline** (one place, `BillCalculator.quote()`): item price → customer discount (VIP −10%) →
  active `HappyHourPolicy` (−20%), one `Math.round` per step, then frozen into `Order.priceCents`.
* **Happy Hour** — once per game, switched on at the start of a turn, lasts that turn and the next;
  remaining duration is displayed; every WAITING/SEATED customer recovers 15 at the start of each active turn.
* **Host policy** — seats one queued customer per turn (FIFO), in addition to the Waiter's task.
* **Cashier** — automatic: at most one READY_TO_PAY customer per turn, oldest first, and a customer who
  became READY_TO_PAY in the same step pays next turn at the earliest.

## Connector convention (class diagram)

Every relationship is a **real, attached connector**, not a floating line:

* `source` / `target` point at the two class cells, so if you drag a class in draw.io the connector
  and its arrow head follow it and stay on the border.
* `exitX/exitY` + `entryX/entryY` with `exitPerimeter=0;entryPerimeter=0;exitDx=0;exitDy=0;entryDx=0;entryDy=0`
  pin the connection points to exact points on the box borders.
* `edgeStyle=orthogonalEdgeStyle` plus explicit waypoints (stored in `<Array as="points">`) gives
  right-angle routing that follows the planned lanes in the gutters between columns.
* Arrow heads: hollow triangle at the *superclass/interface* end (generalization/realization), open arrow
  at the *target* end (association/dependency), filled diamond at the owner and hollow diamond at the
  referenced end (composition/aggregation).
* **Every connector leaves and enters perpendicular to the border it touches** (horizontal for left/right
  borders, vertical for top/bottom borders) — the router builds the path from both ports, so an arrow head
  always points straight at the class. Mixed cases (e.g. `Restaurant ◆— Staff`, which leaves the left border
  and must enter the top border) are routed as "out horizontally, in vertically".

### Spacing and "no crossing" rules used by the layout

* Columns are 340 px wide with **140 px gutters**; two classes in the same column are **80 px** apart;
  parallel lanes inside a gutter are **30–40 px** apart (the two long associations at the very top are
  25 px apart).
* Lanes inside one gutter are ordered as **nested intervals**: the connector with the longest vertical
  span gets the lane closest to its source column, and the deeper its target the earlier its exit point.
  With that ordering no connector can cross another one.
* Long associations (`Restaurant → Customer`, `WaitingQueue → Customer`) run in the empty band **above**
  the boxes; the `CustomerFactory «create»` dependency runs below everything and comes back up the far-right
  gutter of the `Customer` column.
* Verified against draw.io's own renderer: no diagonal segment, no connector through a class box or a note,
  **0 connector/connector crossings**, every arrow perpendicular to the border it hits, every end on the
  border of the class it connects, and every decoration at the correct end.

### Multiplicities

Multiplicities are written at the association ends (solid associations, compositions and aggregations) with
the role name in the middle where it is not already obvious from the attribute list: `1 … 1`, `1 … 2`
(tables), `1 … 1..4` (staff), `1 … 0..*` (menu items, customers, queued, orders per item) and
`0..1 … 1` (customer ↔ order). Dependencies (dashed arrows) carry **no** multiplicities — UML only allows
them on association ends.

## Key abstractions and responsibilities (for the design-discussion table)

| Class | Layer | Responsibility |
|---|---|---|
| `UserInterface` / `ConsoleUI` | boundary | Collects the manager's decisions, displays state, formats cents as `$x.xx`. No game rules. |
| `GameController` | control | Runs the six steps of every turn in order, in all 18 turns. |
| `ManagerChoices` | control (value) | The manager's decisions for one turn (waiter task + target, chef order, Happy Hour request). |
| `SimulationConfig` | control | Base setting: 18 turns, 2 tables, arrival plan, seeded Random, the three victory targets. |
| `CustomerFactory` | control | Creates the right `Customer` subclass for a `CustomerType` arrival. |
| `BillCalculator` | control | Applies VIP then Happy Hour and locks the bill. |
| `ResultEvaluator` | control | Checks the three targets once, after turn 18. |
| `Restaurant` | entity (aggregate) | Owns the menu, tables, waiting queue, staff and the orders/customers of the run; answers the queries the turn needs. |
| `Table`, `WaitingQueue`, `Menu` | entity | The physical world; multiplicity 2 tables, 1 queue, 0..* menu items. |
| `Order` | entity | One dish or combo: preparation progress, READY/SERVED/PAID transitions, locked price. |
| `MenuItem` / `Dish` / `ComboMeal` | entity | Polymorphic price and preparation units; a combo aggregates two menu items. |
| `Customer` / `Regular` / `VIP` / `Critic` | entity | Satisfaction loss per waiting turn (8/5/12), discount, food choice, service reaction. |
| `Satisfaction` | value object | Owns the 0..100 rule with `lose()` / `recover()`. |
| `ScoreBoard` | entity | Revenue in cents, paid satisfactions, served/abandoned counters. |
| `HappyHourPolicy` | policy | Switch state, remaining duration, −20%, +15 recovery. |
| `Staff` / `Waiter` / `Chef` / `Cashier` / `Host` | entity (roles) | One task per turn each; the Waiter seats/orders/serves, the Chef advances one unit, the Cashier collects, the Host seats one extra. |

## Where the design principles actually show up

* **SRP** — pricing (`BillCalculator`), scoring (`ScoreBoard`), targets (`ResultEvaluator`), configuration
  (`SimulationConfig`) and turn sequencing (`GameController`) are separate.
* **OCP** — Stage 2 added `VIPCustomer`/`CriticCustomer` and Stage 3 added `Host`, `Dish`/`ComboMeal` and
  `HappyHourPolicy`; neither stage needed an edit to the six steps in `playTurn()`.
* **LSP** — every customer subclass is usable anywhere a `Customer` is (queue, table, order, payment,
  satisfaction loss) and only overrides what differs.
* **DIP** — the controller depends on the `UserInterface` boundary abstraction; `ConsoleUI` implements it.
* **Polymorphism over type checks** — the turn controller calls `chooseItem()`, `satisfactionLossPerTurn()`,
  `applyDiscount()`, `onServed()`, `onTurnEnd()`; there is no `instanceof` in the control layer.

## The sequence scenario in numbers

Base setting, seed 2026, arrivals t1 **VIP** (C1), t4 Regular (C2), t7/t10/t16 Regular, t13 **Critic** (5 paid
customers in the full run). Detailed turns in the figure:

| Turn | What the figure shows |
|---|---|
| 1 | C1 arrives (satisfaction 100), Waiter seats it, end-of-turn loss 5 → 95 |
| 2 | Manager switches Happy Hour on (turns 2–3); C1 recovers 15 → 100; Waiter takes the Burger order; bill locked at $15.00 × 0.9 × 0.8 = **$10.80**; Chef advances it to READY |
| 3 | Waiter serves the READY order; C1 eats, no further loss |
| 4 | C2 arrives. **The choice:** the Waiter may serve `o1` *or* seat C2. `alt` branch 1 serves → C1 pays on turn 6 with satisfaction 95; branch 2 seats → C1 pays on turn 7 with 90, C2 still loses the same three waiting turns |
| 5 | C1 finishes eating → READY_TO_PAY; the payment-candidate list is empty that turn (pays next turn at the earliest) |
| 6 | C2's order is taken at full price $9.00; Cashier collects $12.96 from C1, records satisfaction 95, frees T1 |
| 7–18 | Abbreviated: the same six steps; C2's order flows READY → SERVED → eating → paid $9.00 with satisfaction 76 |
| after 18 | `ResultEvaluator.evaluate()` → VICTORY when paid ≥ 4, revenue ≥ $45.00 and average ≥ 60 |

## Keeping the figures in sync with the code

* Class names, attribute/method signatures and multiplicities in the class diagram are the intended Stage 3
  API — if the code renames something, update the matching row rather than redrawing.
* The annotations ①–⑦ (encapsulation, polymorphism, composition/aggregation, money, determinism, stages/OCP)
  are the examples to quote in the report's "Design evidence" section.
