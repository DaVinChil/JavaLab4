package ru.ns;

public class Main {
    private static final Configuration configuration = Configuration.getConfiguration();

    public static void main(String[] args) throws InterruptedException {
        printConfiguration();

        ElevatorsOperator elevatorsOperator = elevatorsOperator();
        elevatorsOperator.launch();

        ElevatorCaller elevatorCaller = elevatorCaller(elevatorsOperator);
        elevatorCaller.launch();
    }

    public static void printConfiguration() {
        System.out.printf("""
                Building has %d floors and %d elevators.
                Elevator moves by 1 floor in %d milliseconds.
                %d milliseconds maximal wait for call.
                %n""",
                configuration.floorCount(), configuration.elevatorsCount(),
                configuration.elevatorSpeed(), configuration.maxCallAwait()
        );
    }

    public static ElevatorsOperator elevatorsOperator() {
        return new ElevatorsOperator(
                configuration.floorCount(),
                configuration.elevatorsCount(),
                configuration.elevatorSpeed()
        );
    }

    public static ElevatorCaller elevatorCaller(ElevatorsOperator elevatorsOperator) {
        return new ElevatorCaller(
                elevatorsOperator,
                configuration.maxCallAwait()
        );
    }
}