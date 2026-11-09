package com.koicoffee.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsEvent {
    private String type;
    private String entity;
    private String action;
    private Long id;
    private String message;
    private Object payload;
    private long timestamp;

    public static WsEvent of(String type, String entity, String action, Long id, String message, Object payload) {
        return WsEvent.builder()
                .type(type)
                .entity(entity)
                .action(action)
                .id(id)
                .message(message)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static WsEvent order(String type, String action, Long id, String message) {
        return of(type, "ORDER", action, id, message, null);
    }

    public static WsEvent menu(String type, String action, Long id, String message) {
        return of(type, "MENU", action, id, message, null);
    }

    public static WsEvent shift(String type, String action, Long id, String message) {
        return of(type, "SHIFT", action, id, message, null);
    }

    public static WsEvent user(String type, String action, Long id, String message) {
        return of(type, "USER", action, id, message, null);
    }
}

