package ru.ns;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public record Configuration(
        int floorCount,
        long maxCallAwait,
        int elevatorsCount,
        long elevatorSpeed
) {
    public static Configuration getConfiguration() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new File("src\\main\\resources\\configuration.json"), Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
