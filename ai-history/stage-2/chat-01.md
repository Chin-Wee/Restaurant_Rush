# AI chat history: Stage 2, chat 01

Author: Bhavya (SC2002 Group 5, Restaurant Rush). Stage 2 (VIP and Critic customers).
Messages are in the original order. Only the speaker labels (Me / Claude) and separators were added; the wording is unchanged.

---

**Me:**

SC2002_Restaurant_Rush_Group_Assignment 1.docx
DOCX

What are the deliverables for Stage 2 and what structure should my part have

---

**Claude:**

Stage 2 adds VIP and Critic customers on top of the Stage 1 game, and it has to run on its own. The brief submits all three stages together, so the diagrams, report and video are group-level. Your part is the Stage 2 code plus the evidence that shows how Stage 2 grew out of Stage 1. The group submission is due tomorrow, 15 Nov, 11:59pm.

Stage 2 deliverables

Code (tag stage-2)

Add VIPCustomer and CriticCustomer alongside Regular customers.
Regular: loses 8 satisfaction per waiting turn and pays full price.
VIP: loses 5 per waiting turn and gets 10% off the bill.
Critic: loses 12 per waiting turn and pays full price. It also needs a special behaviour that you define and document, such as an order preference or a reaction to service.
All customer types use the same order and service flow.
Differences must be dispatched through the customer objects (polymorphism). The turn controller must not contain a chain of type checks.
Stage 2 must be playable independently and use the same win/lose criteria as Stage 1.
The tag must contain no Stage 3 features: no Host, ComboMeal or Happy Hour.
A reproducible base-setting run must include at least one VIP and one Critic. You can swap two of the regular arrivals, but there must still be six arrivals.
Bills stay in integer cents. The VIP 10% discount is where rounding could appear, so decide and document how it rounds.

Evidence for the Stage 1 → Stage 2 transition

In the README, list the new files, and the existing files changed or deleted with reasons. Add the commit or compare link.
Explain why any earlier class had to change. Small registration or setup changes are expected.
If you broadly rewrote stable Stage 1 rules, explain why and show that the earlier behaviour still works.
Include test notes for the Stage 2 run, with a seed or fixed arrival schedule so a TA can reproduce it.

Items that touch your part

Class diagram: it must match the Stage 3 code, so your customer hierarchy has to appear correctly in it.
Sequence diagram: it must include one Stage 2 or Stage 3 behaviour, such as VIP pricing.
Video: segment 4 is a VIP or Critic behaviour.
Declarations: each member submits their own GAI declaration.
Structure for your part
Design
Make Customer abstract, or give it a clear base, with the variation points as overridable methods. These are the satisfaction loss per waiting turn, the bill calculation, and your Critic hook.
Add RegularCustomer, VIPCustomer and CriticCustomer as subclasses.
Keep the turn controller, Waiter and Cashier calling only the base type.
Put the VIP discount in the customer’s bill method, so that Stage 3’s Happy Hour can stack on it later. The brief says to apply the VIP discount first, then Happy Hour.
Critic behaviour
Define one concrete rule, for example a fixed preferred dish or an extra penalty when its order isn’t served within N turns.
It must be deterministic or seeded, and documented.
Arrival schedule
Fix which of the six arrivals are VIP and Critic, for example turns 4 and 10, so the run is reproducible.
Git
Commit in small steps, then tag stage-2 on a runnable commit.
Report section
Cover what changed from Stage 1 and why.
Name the polymorphism and OCP justification: adding a customer type needed no edits to the controller.
Describe the trade-off and the alternative you considered, such as a type enum with switch statements, and why you rejected it.
Test notes
Give the Stage 2 run steps and the expected result for the base setting.

The brief also warns against creating interfaces just to satisfy a checklist, so only add an interface if it does real work.

If you want, send me your Stage 2 code or the repo and I’ll check it against these points, especially the “no type-check chain in the controller” rule.

---

**Me:**

can u break down the points of this class diagram, access my repo? what exactly does it have so what my code must include u can acess it?

---

**Claude:**

I can’t open your repo from here. GitHub isn’t linked to this session, so Chin-Wee/Restaurant_Rush is blocked. To fix that, link GitHub under claude.ai Settings → Connectors. You can also zip the code and attach it, or paste the Stage 2 files. Until then I can only read the diagram, so the breakdown below is what your code must contain to match it.

What the diagram requires

Boundary and control

ConsoleUI (boundary) only collects the manager’s choices and displays state. It has run(), promptManagerChoices(), displayTurnStart(GameSnapshot), displayTurnSummary(TurnResolution), displayEndGameResult(GameResult) and displayError(String). It holds no game rules.
TurnController runs the turn sequence. It has beginTurn(), processArrival(), submitWaiterDecision(WaiterChoiceDTO), submitChefDecision(ChefChoiceDTO), snapshot(), lastResolution(), phase(), currentTurn() and result(). It also holds GameConfig, Restaurant and the pending acceptedWaiter choice.
The TurnPhase enum has START_OF_TURN, MANAGER_CHOICES, RESOLVING and GAME_OVER.
The visualization (GUI) package is collapsed and owns no game rules. It is optional, and the brief gives no extra marks for it.

Data transfer objects and records

TurnChoicesDTO holds a WaiterChoiceDTO and a ChefChoiceDTO.
WaiterChoiceDTO is (WaiterTaskType, targetId).
ChefChoiceDTO is (ChefTaskType, targetOrderId).
GameSnapshot and TurnResolution are immutable records. GameSnapshot carries the turn, phase, revenue, average satisfaction, queue, tables, customers, orders and available staff actions. TurnResolution carries events, new arrival, departing customers, payment record, isSuccessful, errorMessage and result.

Entities

Restaurant is the aggregate root. It owns two Tables, the WaitingQueue, the Menu, active Orders and the staff list. It also tracks customers, revenue in cents, paid and served counts, unhappy departures, and evaluateResult(). It has completePayment() and abandon().
Table has assignCustomer(), releaseTable() and isOccupied().
WaitingQueue is a FIFO with addCustomer, pollCustomer, peekCustomer and remove.
Order links a customer and a MenuItem. It has remainingPrepUnits, advancePreparation(), isReady() and a status.
MenuItem has name, priceCents and prepUnitsNeeded. Menu holds the list of items.

Staff

Staff is abstract, with hasActedThisTurn, resetTurn(), waitTurn() and protected requireAvailable() and completeAction(). This enforces “one task per turn”.
Waiter has seatCustomer(), takeOrder() and serveOrder().
Chef has prepareOrder().
Cashier has processPayment().

Stage 2 core (your part)

Customer is abstract. It has id, satisfaction (starting at 100), state, arrivalOrdinal and order. It has three abstract methods:
applyTurnEndDecay()
calculateBill(basePriceCents)
chooseMenuItem(availableItems)
RegularCustomer, VIPCustomer and CriticCustomer override those three methods. CriticCustomer also has preferredItemName, which is your documented Critic behaviour.
CustomerFactory has createCustomer(CustomerType, id). The CustomerType enum is REGULAR, VIP, CRITIC.
Enums: CustomerState (QUEUED, SEATED, EATING, READY_TO_PAY, PAID, ABANDONED), OrderStatus (PENDING, IN_PREPARATION, READY, SERVED, PAID, CANCELLED), GameResult, WaiterTaskType and ChefTaskType.
Things to check against your code
Polymorphism: TurnController and Restaurant must call customer.applyTurnEndDecay() and customer.calculateBill() with no instanceof or switch on CustomerType. The only place that switches on type should be CustomerFactory.
Arrival schedule: the diagram doesn’t show where the schedule lives. It is probably in GameConfig. Make sure it puts at least one VIP and one Critic among the six arrivals.
Match the code: the brief says the class diagram must match the Stage 3 code and that marks include “match to code”. Compare every method signature against your source.
Stage 3 additions: Host and ComboMeal don’t appear in this Phase 2 diagram, so someone will need to extend it for Stage 3. That is not your part, but the final diagram needs them.
Annotations: the brief asks for annotated examples of encapsulation, polymorphism and composition. The diagram has none yet, and the notes box only describes the input path.
Notation: a few multiplicity labels sit awkwardly on the lines, for example around Table, Customer and Order. Tidy them before submission.
Possible mismatch: Cashier has no direct association to Restaurant drawn, but processPayment takes a Restaurant parameter. Make sure the code does the same.

If you attach the Stage 2 source, I can check it against this list.

---

**Me:**

is the switch in CustomerFactory ok? the brief says no type checks in the controller. also i have a switch on customer type in the GUI for the colours, does that break the rule? package edu.ntu.sc2002.domain.customer;

import java.util.Objects;

public final class CustomerFactory {
    public static final String CRITIC_PREFERRED_ITEM = "Pasta";

    private CustomerFactory() {}

    public static Customer createCustomer(CustomerType type, String id, int arrivalOrdinal, long choiceSeed) {
        return switch (Objects.requireNonNull(type)) {
            case REGULAR -> new RegularCustomer(id, arrivalOrdinal, choiceSeed);
            case VIP -> new VIPCustomer(id, arrivalOrdinal, choiceSeed);
            case CRITIC -> new CriticCustomer(id, arrivalOrdinal, CRITIC_PREFERRED_ITEM);
        };
    }
} private void drawCustomerTag(GraphicsContext graphics, Actor actor, String id) {
    CustomerType type = customerTypes.getOrDefault(id, CustomerType.REGULAR);
    String label = type == CustomerType.REGULAR ? id : id + " " + type;
    Color color = switch (type) {
        case VIP -> Color.web("
#FFD54A");
        case CRITIC -> Color.web("
#FF6B6B");
        case REGULAR -> Color.WHITE;
    };
    drawText(graphics, label, actor.column * TILE_SIZE + TILE_SIZE / 2, (actor.row - 1) * TILE_SIZE - 8, color);
} public abstract class Customer {
    public abstract CustomerType getType();
    public abstract void applyTurnEndDecay();
    public abstract int calculateBill(int basePriceCents);
    public abstract MenuItem chooseMenuItem(List<MenuItem> availableItems);

    protected final void loseSatisfactionIfWaiting(int points) {
        if (state == CustomerState.WAITING || state == CustomerState.SEATED) deductSatisfaction(points);
    }

    protected MenuItem seededChoice(List<MenuItem> availableItems) {
        Objects.requireNonNull(availableItems);
        if (availableItems.isEmpty()) throw new IllegalStateException("Menu is empty");
        return availableItems.get(new Random(choiceSeed).nextInt(availableItems.size()));
    }
} public final class RegularCustomer extends Customer {
    public RegularCustomer(String id, int arrivalOrdinal, long choiceSeed) { super(id, arrivalOrdinal, choiceSeed); }

    @Override public CustomerType getType() { return CustomerType.REGULAR; }
    @Override public void applyTurnEndDecay() { loseSatisfactionIfWaiting(8); }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
    @Override public MenuItem chooseMenuItem(List<MenuItem> availableItems) { return seededChoice(availableItems); }
} public final class CriticCustomer extends Customer {
    private final String preferredItemName;

    public CriticCustomer(String id, int arrivalOrdinal, String preferredItemName) {
        super(id, arrivalOrdinal, 0);
        Objects.requireNonNull(preferredItemName);
        if (preferredItemName.isBlank()) throw new IllegalArgumentException("Invalid preferred item");
        this.preferredItemName = preferredItemName;
    }

    @Override public CustomerType getType() { return CustomerType.CRITIC; }
    @Override public void applyTurnEndDecay() { loseSatisfactionIfWaiting(12); }
    @Override public int calculateBill(int basePriceCents) {
        if (basePriceCents < 0) throw new IllegalArgumentException("Price must be nonnegative");
        return basePriceCents;
    }
    @Override public MenuItem chooseMenuItem(List<MenuItem> availableItems) {
        Objects.requireNonNull(availableItems);
        return availableItems.stream().filter(i -> i.getName().equalsIgnoreCase(preferredItemName)).findFirst()
            .or(() -> availableItems.stream().max(Comparator.comparingInt(MenuItem::getPriceCents)))
            .orElseThrow(() -> new IllegalStateException("Menu is empty"));
    }
} public Order takeOrder(Customer customer, Menu menu) {
    var item = customer.chooseMenuItem(menu.getItems());
    return customer.placeOrder(item, customer.calculateBill(item.getPriceCents()));
}

---

**Claude:**

Both are fine, but there are a few other things in your code to fix.

Is the CustomerFactory switch ok?

Yes. The rule in the brief is that different behaviour must be dispatched through the customer objects, “not through a chain of type checks in the turn controller”. The factory switch only decides which class to create. It never branches on behaviour.

After creation, everything goes through the Customer methods. Your takeOrder does this correctly: it calls chooseMenuItem and calculateBill, and no type check sits in the controller or the Waiter.

The cost is that adding a new customer type means editing the factory and the CustomerType enum. That is a small OCP trade-off. State it in your design discussion rather than hiding it.

Does the GUI colour switch break the rule?

No. The rule covers game behaviour in the turn controller, and colour is presentation. The brief also says the UI collects decisions and displays state, and colour is neither a game rule nor a decision.

Two small tidy-ups would make it harder for a marker to question:

Use an EnumMap<CustomerType, Color> instead of a switch. It reads as data rather than branching, and a missing entry is easier to spot.
Keep colours out of the domain package. Don’t add a getColor() to Customer, because the domain shouldn’t know about JavaFX.
Problems I found
State name mismatch. The code uses CustomerState.WAITING, but the diagram’s enum says QUEUED. The brief marks “match to code”, so fix one of them.
Factory signature mismatch. The diagram shows createCustomer(type, id), but the code takes (type, id, arrivalOrdinal, choiceSeed). The class is final, with a private constructor and static methods, so mark the method static in the diagram.
Missing from the diagram. It lacks getType(), choiceSeed and placeOrder(item, bill) on Customer.
Duplicated validation. The check basePriceCents < 0 is repeated in each subclass. Move it into a protected helper in Customer, or use a template method where the base class validates and then calls a protected computeBill. This is optional but cleaner.
Hardcoded critic item. The Critic matches "Pasta" by name, and falls back to the most expensive item if it isn’t found. That works, but document it. Pasta takes 2 prep units and the Critic loses 12 per wait, which is a real trade-off for the player and worth mentioning in the report.
VIP not checked. I haven’t seen VIPCustomer. Check the 10% discount: $15 gives 1350 cents, but $9 gives 810 and $18 gives 1620, which all divide cleanly. Document the rounding rule anyway, since Stage 3 combos will not always divide evenly.
Stage 3 stacking. The brief says to apply the VIP discount first, then Happy Hour, and to lock the price when the order is taken. Your placeOrder(item, bill) already locks it at order time. Keep Happy Hour out of calculateBill, or the stacking order could break.

Send VIPCustomer and GameConfig if you want me to check the arrival schedule and the discount.

---

**Me:**

ok fixed

---

**Claude:**

Good. Before you tag stage-2, here’s what’s left:

Confirm VIPCustomer rounds the discount in integer cents and that the rule is documented.
Check that the arrival schedule has at least one VIP and one Critic among the six arrivals.
Make sure the tag contains no Stage 3 code (no Host, ComboMeal or Happy Hour).
Run the base setting once from the tag, and save the seed and the expected result in your test notes.
Write your README transition entry for 1 → 2: new files, changed files with reasons, and the compare link.

Send me VIPCustomer or GameConfig if you’d like me to check them.
