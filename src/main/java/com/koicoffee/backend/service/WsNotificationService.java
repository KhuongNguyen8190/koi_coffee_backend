package com.koicoffee.backend.service;

import com.koicoffee.backend.dto.WsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WsNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastOrder(String action, Long orderId, String message) {
        WsEvent event = WsEvent.order("ORDER_" + action.toUpperCase(), action, orderId, message);
        try {
            messagingTemplate.convertAndSend("/topic/orders", event);
            // Backward-compatible signal for legacy listeners
            messagingTemplate.convertAndSend("/topic/public", "ORDER_CHANGED");
        } catch (Exception e) {
            log.error("Failed to broadcast order event", e);
        }
    }

    public void broadcastMenu(String action, Long id, String message) {
        WsEvent event = WsEvent.menu("MENU_" + action.toUpperCase(), action, id, message);
        try {
            messagingTemplate.convertAndSend("/topic/menu", event);
            // Backward-compatible signals
            if (message != null && !message.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/public", "MENU_NOTIFY:" + message);
            }
            messagingTemplate.convertAndSend("/topic/public", "DATA_CHANGED");
        } catch (Exception e) {
            log.error("Failed to broadcast menu event", e);
        }
    }

    public void broadcastShift(String action, String message) {
        String eventType = "OPEN".equalsIgnoreCase(action) ? "SHIFT_OPENED" : "SHIFT_CLOSED";
        WsEvent event = WsEvent.shift(eventType, action, null, message);
        try {
            messagingTemplate.convertAndSend("/topic/shifts", event);
            // Backward-compatible signals
            messagingTemplate.convertAndSend("/topic/public", eventType);
        } catch (Exception e) {
            log.error("Failed to broadcast shift event", e);
        }
    }

    public void broadcastUser(String action, String username, String message) {
        WsEvent event = WsEvent.user("USER_" + action.toUpperCase(), action, null, message);
        try {
            messagingTemplate.convertAndSend("/topic/users", event);
            // Backward-compatible signals
            if ("LOCK".equalsIgnoreCase(action) && username != null) {
                messagingTemplate.convertAndSend("/topic/public", "USER_LOCKED:" + username);
            }
            messagingTemplate.convertAndSend("/topic/public", "USER_LIST_CHANGED");
        } catch (Exception e) {
            log.error("Failed to broadcast user event", e);
        }
    }

    public void kickoutUser(Long userId, String sessionId) {
        try {
            messagingTemplate.convertAndSend("/topic/kickout/" + userId, sessionId);
        } catch (Exception e) {
            log.error("Failed to send kickout event for user {}", userId, e);
        }
    }
}
