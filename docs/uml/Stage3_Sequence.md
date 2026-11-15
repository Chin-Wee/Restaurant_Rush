# Stage 3 sequence diagram

The [six submission diagrams](../../README.md#stage-3-submission-sequence-diagrams) show the VIP burger scenario from the video. The diagram below follows the CLI victory replay.

Follow the [seed 2026 Easy victory](../../README.md#instant-cli-tests).
C2 is the VIP arriving on turn 4 while C1 occupies Table 1. Activating Happy Hour
before C2 orders locks in $12.96 for Pasta ($18.00 × 0.9 × 0.8). With identical
staff choices and Happy Hour declined, C2 pays $16.20 with satisfaction 90
instead of 95.

```mermaid
sequenceDiagram
    actor Manager
    participant UI as ConsoleUI
    participant TC as TurnController
    participant CF as CustomerFactory
    participant R as Restaurant
    participant H as Host
    participant W as Waiter
    participant C as C2 · VIPCustomer
    participant HH as HappyHourPolicy
    participant K as Chef
    participant O as C2's Order
    participant Cash as Cashier
    participant S as ScoreBoard

    loop Until phase is GAME_OVER
        UI->>TC: beginTurn()
        opt Happy Hour already active
            TC->>C: addSatisfaction(15) if waiting or seated
        end
        UI->>TC: processArrival()
        opt Scheduled arrival, C2 on turn 4
            TC->>CF: createCustomer(VIP, C2, 2, 2028)
            CF-->>TC: VIPCustomer
            TC->>R: registerArrival(C2)
            Note right of R: WaitingQueue enqueues C2
        end
        UI->>TC: snapshot()
        UI-->>Manager: Display state and available tasks
        opt Turn 4, manager activates Happy Hour
            Manager->>UI: Activate Happy Hour
            UI->>TC: activateHappyHour()
            TC->>HH: activate(4)
            HH-->>TC: ACTIVE for two turns
        end
        Manager->>UI: Choose Host task
        UI->>TC: submitHostDecision(choice)
        opt Seat C2 on turn 4
            TC->>H: seatCustomer(C2, queue, table2)
            Note over H,C: Host assigns Table 2 and dequeues C2
        end
        loop Until Waiter choice is accepted
            Manager->>UI: Choose Waiter task
            UI->>TC: submitWaiterDecision(choice)
            alt Invalid task
                TC-->>UI: Failed TurnResolution
                UI-->>Manager: Explain and retry
            else Take C2's order on turn 4
                TC->>W: takeOrder(C2, menu, policy)
                W->>C: chooseItem(menu)
                C-->>W: Pasta
                W->>C: calculateBill(1800)
                C-->>W: 1620 cents
                W->>HH: applyHappyHourDiscount(1620)
                HH-->>W: 1296 cents
                W->>C: placeOrder(Pasta, 1296)
                C->>O: Order(Pasta, 1296)
                UI-->>Manager: Show Pasta and $12.96
            else Serve C2 on turn 6, READY since turn 5
                TC->>W: serve(C2, 6)
                W->>C: serveOrder(6)
                C->>O: serve(6)
                Note over C,O: C2 is EATING, order is SERVED
            end
        end
        Manager->>UI: Choose Chef task
        UI->>TC: submitChefDecision(choice)
        opt Advance C2 on turns 4 and 5
            TC->>K: prepareOrder(order, turn)
            K->>O: advance(turn)
            Note right of O: PREPARING (1/2) on turn 4, READY (2/2) on turn 5
        end
        TC->>C: finishEating(turn)
        Note right of C: READY_TO_PAY on turn 7, eligible from turn 8
        opt Eligible customer, C2 on turn 8
            TC->>R: oldestPaymentCandidate(8)
            R-->>TC: C2
            TC->>Cash: processPayment(C2, restaurant, 8)
            Cash->>C: canPay(8)
            C-->>Cash: true
            Cash->>R: completePayment(C2)
            R->>C: paid()
            C->>O: markPaid()
            Note over R,O: Restaurant releases Table 2
            R-->>Cash: PaymentRecord(C2, 1296, 95)
            Cash-->>TC: PaymentRecord
            TC->>S: recordPayment(record)
        end
        opt C2 is still waiting or seated
            TC->>C: applySatisfactionLoss()
            Note right of C: VIP loses 5 at turn end
        end
        TC->>HH: decrementTurn()
        Note right of HH: EXHAUSTED after turn 5
        TC-->>UI: TurnResolution
        Note over UI,TC: CLI displays the completed summary at the next turn or at game end
    end
    UI->>TC: result()
    TC->>S: evaluateResult()
    Note right of S: Paid 5, revenue $91.26, average satisfaction 84.6
    S-->>TC: VICTORY
    TC-->>UI: VICTORY
    UI-->>Manager: Display final result
```
