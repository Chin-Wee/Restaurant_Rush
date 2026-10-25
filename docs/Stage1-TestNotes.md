# Stage 1 reproducible checks

Build and run the acceptance checks with `./mvnw -q clean package`. The suite covers the exact base setting, Pasta trace, role limits, invalid retries, all victory thresholds, turn sequencing, payment timing/priority, abandonment, terminal-state guards, immutable views, input handling and the actual CLI.

## Actual CLI replays

```sh
java -cp target/classes edu.ntu.sc2002.App 2026 < src/test/resources/stage1/victory-seed-2026.txt
java -cp target/classes edu.ntu.sc2002.App 2026 < src/test/resources/stage1/defeat-seed-2026.txt
```

| Run | Turns | Served | Paid | Departures | Revenue | Average paid satisfaction | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Victory input, seed 2026 | 18 | 5 | 4 | 0 | $51.00 | 78.0 | VICTORY |
| Both roles WAIT, seed 2026 | 18 | 0 | 0 | 2 | $0.00 | N/A | DEFEAT |

These are actual stdin command files checked by `ConsoleUiTest`, including parsing, display and ending. No automatic player is part of the game. Input uses Waiter 1=Seat, 2=Take order, 3=Serve, 4=Wait; Chef 1=Advance, 2=Wait, followed by customer IDs when prompted.

The Victory script uses this deterministic policy: serve the oldest already-ready order; otherwise take the oldest seated customer's order; otherwise seat the oldest queued customer if a table is free; otherwise Wait. Chef advances the oldest unfinished order or waits. The recorded choices are:

| Turn | Waiter | Chef |
| --- | --- | --- |
| 1 | SEAT C1 | WAIT |
| 2 | TAKE_ORDER C1 | ADVANCE C1 |
| 3 | SERVE C1 | WAIT |
| 4 | SEAT C2 | WAIT |
| 5 | TAKE_ORDER C2 | ADVANCE C2 |
| 6 | WAIT | ADVANCE C2 |
| 7 | SERVE C2 | WAIT |
| 8 | SEAT C3 | WAIT |
| 9 | TAKE_ORDER C3 | ADVANCE C3 |
| 10 | SERVE C3 | WAIT |
| 11 | SEAT C4 | WAIT |
| 12 | TAKE_ORDER C4 | ADVANCE C4 |
| 13 | SERVE C4 | WAIT |
| 14 | SEAT C5 | WAIT |
| 15 | TAKE_ORDER C5 | ADVANCE C5 |
| 16 | SEAT C6 | ADVANCE C5 |
| 17 | SERVE C5 | WAIT |
| 18 | TAKE_ORDER C6 | ADVANCE C6 |

## Assignment Pasta example

`StageOneGameTest.reproducesPastaExampleAndDefersPayment` uses game seed 2, giving C1 choice seed 3 and Pasta. Turn 1: seat C1, satisfaction 92. Turn 2: take order plus first Chef unit, 84. Turn 3: second unit becomes READY, 76. Turn 4: serve, served count increments, no loss. Turn 5: READY_TO_PAY, still no payment. Turn 6: $18.00 payment and satisfaction 76 recorded, table released; snapshot locations are derived solely from Table.occupant.

Food choice for every customer uses Java Random with seed = game seed + arrival ordinal, selecting uniformly by index from `[Salad, Burger, Pasta]`. This formula and menu order are part of the reproducibility contract. No food is chosen by manager input.

## UML audit

After compiling, run `python3 scripts/audit_stage1_api.py --check`. It confirms the [API inventory](Stage1-Api-Differences.md) is current against the retained Stage 3 diagram and compiled Stage 1 classes. It does not claim zero UML differences. See [the full trace and findings](Stage1-Parity-Audit.md) for their disposition and the GitHub tag publication verification.

## Cashier ordering and Stage 1 purity

The adopted meaning of oldest is earliest arrival ordinal among eligible customers, even if a later arrival became READY_TO_PAY earlier. The assignment does not define that term more precisely. StageOneParityTest.cashierUsesArrivalPriorityEvenWhenLaterArrivalWasReadyEarlier constructs both eligible customers to distinguish these policies; the existing same-readiness test verifies the tie case. These are isolated eligibility fixtures; normal controller runs still pay one customer at most per turn.

Stage 1 has no Customer.recover or applyCustomerDiscount hook. Full-price billing uses item.priceCents directly. Customer has no table ID; Table.occupant owns seating, and the snapshot queries Restaurant.tableFor. The suite contains 23 tests after this focused cleanup.
