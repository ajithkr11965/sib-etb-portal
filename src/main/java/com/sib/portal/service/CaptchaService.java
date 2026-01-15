package com.sib.portal.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class CaptchaService {

    public String generateCaptcha() {
        // Simple alphanumeric captcha
        int length = 6;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
