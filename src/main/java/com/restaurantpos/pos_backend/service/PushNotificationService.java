package com.restaurantpos.pos_backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.restaurantpos.pos_backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    // Sends the same notification to every registered device — a small
    // restaurant POS setup typically has just one or two tablets/phones at
    // the counter, so broadcasting to all of them is the simplest correct
    // behavior (whichever device staff are looking at will show the alert).
    public void sendToAllDevices(String title, String body) {
        deviceTokenRepository.findAll().forEach(deviceToken -> {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (Exception e) {
                log.warn("Failed to send push notification to token {}: {}", deviceToken.getToken(), e.getMessage());
            }
        });
    }
}