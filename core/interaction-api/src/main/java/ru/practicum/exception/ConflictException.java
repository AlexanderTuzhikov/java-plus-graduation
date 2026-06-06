package ru.practicum.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {
    private Object existingData;

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Object existingData) {
        super("Entity already exists");
        this.existingData = existingData;
    }
}