package ru.ns;

import java.util.Random;

public class ElevatorCaller {
    private final Random random = new Random();

    private final ElevatorsOperator elevatorsOperator;

    private final int floorCount;
    private final long maxCallAwait;

    public ElevatorCaller(ElevatorsOperator elevatorsOperator, long maxCallAwait) {
        this.elevatorsOperator = elevatorsOperator;
        this.floorCount = this.elevatorsOperator.getFloorsCount();
        this.maxCallAwait = maxCallAwait;
    }

    public void launch() throws InterruptedException {
        while(true) {
            randomWait();

            Call call = randomCall();
            System.out.printf("Call from %d to %d floor%n", call.from() + 1, call.to() + 1);

            elevatorsOperator.processCall(call);
        }
    }

    private void randomWait() throws InterruptedException {
        long await = random.nextLong(maxCallAwait);
        Thread.sleep(await);
    }

    private Call randomCall() {
        int from = randomFloor();
        int to = randomFloor();
        return new Call(from, to);
    }

    private int randomFloor() {
        return random.nextInt(floorCount);
    }
}
