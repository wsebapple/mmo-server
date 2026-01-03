package com.example.mmo.protocol.dto;

public class ErrorPayload {
    public String message;

    public ErrorPayload() {}

    public ErrorPayload(String message) {
        this.message = message;
    }
}
