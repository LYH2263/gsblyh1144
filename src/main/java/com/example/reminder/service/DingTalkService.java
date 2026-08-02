package com.example.reminder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 通过钉钉机器人Webhook发送消息
     * 仅当响应 JSON 中 errcode == 0 时视为发送成功，否则返回失败并携带 errmsg
     */
    public DingTalkSendResult sendMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("钉钉Webhook地址为空，跳过发送");
            return DingTalkSendResult.fail("钉钉Webhook地址为空");
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
            log.info("钉钉消息发送响应: {}", response);

            if (response == null || response.trim().isEmpty()) {
                return DingTalkSendResult.fail("钉钉接口无响应");
            }

            JsonNode node = objectMapper.readTree(response);
            int errcode = node.path("errcode").asInt(-1);
            if (errcode == 0) {
                return DingTalkSendResult.success();
            }
            String errmsg = node.path("errmsg").asText("钉钉接口返回失败");
            log.warn("钉钉消息发送失败, errcode={}, errmsg={}", errcode, errmsg);
            return DingTalkSendResult.fail("[" + errcode + "] " + errmsg);
        } catch (Exception e) {
            log.error("钉钉消息发送异常: {}", e.getMessage(), e);
            return DingTalkSendResult.fail(e.getMessage());
        }
    }
}
