package ru.ns;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ElevatorsOperator {
    private final List<Deque<Call>> externalCallsByFloor = new ArrayList<>();
    private final List<Elevator> elevators = new ArrayList<>();

    private final int floorsCount;
    private final int elevatorsCount;
    private final long elevatorSpeed;

    private final Thread process;

    private long tick = 0;

    public ElevatorsOperator(int floorsCount, int elevatorsCount, long elevatorSpeed) {
        this.floorsCount = floorsCount;
        this.elevatorsCount = elevatorsCount;
        this.elevatorSpeed = elevatorSpeed;

        initializeExternalCallsByFloor();
        initializeElevators();

        this.process = createProcess();
    }

    private void initializeExternalCallsByFloor() {
        for (int i = 0; i < floorsCount; i++) {
            externalCallsByFloor.add(new ConcurrentLinkedDeque<>());
        }
    }

    private void initializeElevators() {
        for (int i = 0; i < elevatorsCount; i++) {
            elevators.add(new Elevator(i));
        }
    }

    private Thread createProcess() {
        return new Thread(() -> {
            try {
                process();
            } catch (InterruptedException ignored){
            }
        });
    }

    private void process() throws InterruptedException {
        while (true) {
            System.out.printf("%n----------- Tick %d%n", tick++);

            elevators.forEach(Elevator::processCurrentFloor);

            setTargetFloorFreeElevators();
            elevators.forEach(Elevator::proceed);

            Thread.sleep(elevatorSpeed);
        }
    }

    private void setTargetFloorFreeElevators() {
        new CallDistributor().distributeFloorsByElevator();
    }

    public int getFloorsCount() {
        return floorsCount;
    }

    public void launch() {
        process.start();
    }

    private boolean hasExternalCallOnTheFloor(int floor) {
        return !externalCallsByFloor.get(floor).isEmpty();
    }

    public void processCall(Call call) {
        externalCallsByFloor.get(call.from()).add(call);
    }

    private class Elevator {
        final int id;
        final boolean[] hasInternalCallByFloor;
        int currentFloor = 0;
        int targetFloor = 0;

        Elevator(int id) {
            this.id = id;
            this.hasInternalCallByFloor = new boolean[floorsCount];
        }

        void processCurrentFloor() {
            System.out.printf("Elevator@%d now on %d floor.%n", id, currentFloor + 1);

            Deque<Call> calls = externalCallsByFloor.get(currentFloor);
            while (!calls.isEmpty()) {
                int to = calls.pop().to();
                hasInternalCallByFloor[to] = true;

                System.out.printf("Elevator@%d received internal call to %d floor on %d floor.%n", id, to + 1, currentFloor + 1);
            }

            hasInternalCallByFloor[currentFloor] = false;
        }

        void proceed() {
            calculateTargetFloor();
            nextFloor();
        }

        void calculateTargetFloor() {
            boolean hasInternalCall = hasInternalCall();

            if (currentFloor == targetFloor && hasInternalCall) {
                goToFloor(closestFloorCall());
            } else if (!hasInternalCall && !hasExternalCallOnTheFloor(targetFloor)) {
                targetFloor = currentFloor;
                System.out.printf("Elevator@%d is idle.%n", id);
            }
        }

        int closestFloorCall() {
            for (int i = 0; i < floorsCount; i++) {
                if (i <= currentFloor && hasInternalCallByFloor[currentFloor - i]) {
                    return currentFloor - i;
                } else if (currentFloor + i < floorsCount && hasInternalCallByFloor[currentFloor + i]) {
                    return currentFloor + i;
                }
            }

            return currentFloor;
        }

        boolean hasInternalCall() {
            for (boolean hasCall : hasInternalCallByFloor) {
                if (hasCall) return true;
            }

            return false;
        }

        void goToFloor(int floor) {
            System.out.printf("Elevator@%d going to %d from %d floor.%n", id, floor + 1, currentFloor + 1);
            this.targetFloor = floor;
        }

        void nextFloor() {
            if (currentFloor < targetFloor) {
                currentFloor++;
            } else if (currentFloor > targetFloor) {
                currentFloor--;
            }
        }

        int getCurrentFloor() {
            return currentFloor;
        }

        boolean isFree() {
            return currentFloor == targetFloor && !hasInternalCall();
        }
    }

    private class CallDistributor {
        int[] result;
        int sumDistance = Integer.MAX_VALUE;

        List<Elevator> freeElevators;
        List<Integer> floorsCall;

        List<Integer> more;
        List<Integer> less;

        boolean freeElevatorsMoreThanFloors;

        Map<boolean[], Integer> cache = new HashMap<>();

        void distributeFloorsByElevator() {
            freeElevators = getFreeElevators();
            floorsCall = getFloorsCall();

            freeElevatorsMoreThanFloors = freeElevators.size() > floorsCall.size();

            var elevatorsFloor = freeElevators.stream().map(Elevator::getCurrentFloor).toList();
            more = freeElevatorsMoreThanFloors ? elevatorsFloor : floorsCall;
            less = freeElevatorsMoreThanFloors ? floorsCall : elevatorsFloor;

            startDistribution();
        }

        void startDistribution() {
            result = new int[less.size()];

            recursiveDistribution(0, new int[less.size()], new boolean[more.size()], 0, less, more);
            sendByDistribution();
        }

        void sendByDistribution() {
            for (int i = 0; i < less.size(); i++) {
                Elevator elevator;
                int toFloor;

                if (!freeElevatorsMoreThanFloors) {
                    elevator = freeElevators.get(i);
                    toFloor = floorsCall.get(result[i]);
                } else {
                    elevator = freeElevators.get(result[i]);
                    toFloor = floorsCall.get(i);
                }

                elevator.goToFloor(toFloor);
            }

            sendLeftFreeElevators();
        }

        void sendLeftFreeElevators() {
            if (freeElevatorsMoreThanFloors) {
                for (Elevator elevator : freeElevators) {
                    if (elevator.isFree()) {
                        sendToClosest(elevator);
                    }
                }
            }
        }

        void sendToClosest(Elevator elevator) {
            for (int i = 0; i <= floorsCount; i++) {
                int lower = elevator.currentFloor - i;
                int higher = elevator.currentFloor + i;

                if (lower >= 0 && hasExternalCallOnTheFloor(lower)) {
                    elevator.goToFloor(lower);
                    return;
                } else if(higher < floorsCount && hasExternalCallOnTheFloor(higher)) {
                    elevator.goToFloor(higher);
                    return;
                }
            }
        }

        int recursiveDistribution(int curDistance, int[] curDistribution, boolean[] was, int curIndex, List<Integer> less, List<Integer> more) {
            if (curIndex == less.size() || curDistance > sumDistance) {
                if (curDistance < sumDistance) {
                    sumDistance = curDistance;
                    result = Arrays.copyOf(curDistribution, curDistribution.length);
                }

                return 0;
            }

            int to = 0;
            int minDistance = Integer.MAX_VALUE;
            boolean[] adjustedWas = Arrays.copyOf(was, curIndex);

            if (cache.containsKey(adjustedWas)) {
                to = cache.get(adjustedWas);
                int distanceTo = Math.abs(less.get(curIndex) - more.get(to));

                was[to] = true;
                curDistribution[curIndex] = to;
                minDistance = distanceTo + recursiveDistribution(curDistance + distanceTo, curDistribution, was, curIndex + 1, less, more);
                was[to] = false;
            } else {
                for (int i = 0; i < more.size(); i++) {
                    if (!was[i]) {
                        int distanceTo = Math.abs(less.get(curIndex) - more.get(i));

                        was[i] = true;
                        curDistribution[curIndex] = i;
                        int sum = distanceTo + recursiveDistribution(curDistance + distanceTo, curDistribution, was, curIndex + 1, less, more);
                        was[i] = false;

                        if (sum < minDistance) {
                            to = i;
                            minDistance = sum;
                        }
                    }
                }

                cache.put(adjustedWas, to);
            }

            return minDistance;
        }

        private List<Elevator> getFreeElevators() {
            List<Elevator> freeElevators = new ArrayList<>();
            for (Elevator elevator : elevators) {
                if (elevator.isFree()) {
                    freeElevators.add(elevator);
                }
            }

            return freeElevators;
        }

        private List<Integer> getFloorsCall() {
            var floors = new ArrayList<Integer>();
            for (int i = 0; i < floorsCount; i++) {
                if (hasExternalCallOnTheFloor(i)) {
                    floors.add(i);
                }
            }

            return floors;
        }
    }
}
