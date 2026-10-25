# Stage 1 reproducible checks

Run `./mvnw -q clean test`.

`StageOneGameTest.reproducesPastaExampleAndDefersPayment` uses game seed `2`. The first arrival's choice seed is `3`, so C1 selects Pasta. Actions: turn 1 seat C1; turn 2 take C1's order and advance it; turn 3 advance it; turn 4 serve it; turn 5 wait; turn 6 wait. Expected satisfaction is 92, 84, and 76 at the ends of turns 1–3, then remains 76. Served count increases on turn 4; the $18.00 payment and paid-satisfaction record occur on turn 6, after the full eating turn and one-turn payment delay.

`StageOneRunTest.seed2026WithOldestReadyAndOldestQueuePolicyWins` is the reproducible Victory run (seed `2026`, all 18 turns): each turn, serve the oldest order that was READY before the turn; otherwise take an order for the oldest seated customer without one; otherwise seat the oldest queued customer if a table is free; otherwise WAIT. The Chef advances the oldest order still being prepared, or WAITs if none. The test asserts VICTORY at turn 18.

`StageOneRunTest.seed2026WithWaitEveryTurnLoses` is the reproducible Defeat run (seed `2026`): choose WAIT for both Waiter and Chef for all 18 turns. The test asserts zero payments and DEFEAT at turn 18.

The scenarios call the controller directly to keep exact decisions reproducible. To play interactively, run `java -cp target/classes edu.ntu.sc2002.App 2026` after compiling.
