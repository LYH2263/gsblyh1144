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
     * 钉钉消息发送结果：success 仅当响应 errcode=0；errMsg 承载响应中的 errmsg
     */
    public static class SendResult {
        private final boolean success;
        private final String errMsg;

        private SendResult(boolean success, String errMsg) {
            this.success = success;
            this.errMsg = errMsg;
        }

        public static SendResult ok() {
            return new SendResult(true, null);
        }

        public static SendResult fail(String errMsg) {
            return new SendResult(false, errMsg);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrMsg() {
            return errMsg;
        }
    }

    /**
     * 通过钉钉机器人Webhook发送消息
     * 解析响应JSON：仅 errcode=0 视为成功，否则取 errmsg 作为失败原因
     */
    public SendResult sendMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("钉钉Webhook地址为空，跳过发送");
            return SendResult.fail("钉钉Webhook地址为空");
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
            log.info("钉钉接口响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            int errcode = root.has("errcode") ? root.get("errcode").asInt(-1) : -1;
            String errmsg = root.has("errmsg") ? root.get("errmsg").asText() : null;
            if (errcode == 0) {
                return SendResult.ok();
            }
            log.warn("钉钉消息发送失败: errcode={}, errmsg={}", errcode, errmsg);
            return SendResult.fail(errmsg != null ? errmsg : "钉钉返回错误码: " + errcode);
        } catch (Exception e) {
            log.error("钉钉消息发送失败: {}", e.getMessage(), e);
            return SendResult.fail(e.getMessage());
        }
    }
}
