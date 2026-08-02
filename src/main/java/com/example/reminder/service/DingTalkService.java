package com.example.reminder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 通过钉钉机器人Webhook发送消息
     */
    public boolean sendMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("钉钉Webhook地址为空，跳过发送");
            return false;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");

            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(webhookUrl, request, String.class);
            log.info("钉钉消息发送成功, 响应: {}", response);
            return true;
        } catch (Exception e) {
            log.error("钉钉消息发送失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
