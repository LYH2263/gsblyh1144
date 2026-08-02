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
     * 钉钉发送结果：仅当响应 errcode=0 时 success 为 true；
     * errmsg 携带钉钉返回的错误描述（或异常信息），供调用方写入日志。
     */
    public static class DingTalkResult {
        private final boolean success;
        private final String errmsg;

        public DingTalkResult(boolean success, String errmsg) {
            this.success = success;
            this.errmsg = errmsg;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrmsg() {
            return errmsg;
        }
    }

    /**
     * 通过钉钉机器人Webhook发送消息。
     * 钉钉即使 webhook 失效/被限流也会返回 HTTP 200，必须解析响应体的 errcode：
     * 仅 errcode=0 视为成功，其余一律失败并把 errmsg 回传。
     */
    public DingTalkResult sendMessage(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("钉钉Webhook地址为空，跳过发送");
            return new DingTalkResult(false, "钉钉Webhook地址为空");
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

            // 解析响应 JSON，仅 errcode=0 视为成功
            if (response == null || response.trim().isEmpty()) {
                log.error("钉钉消息发送失败: 响应为空");
                return new DingTalkResult(false, "钉钉响应为空");
            }

            JsonNode node = objectMapper.readTree(response);
            JsonNode errcodeNode = node.get("errcode");
            String errmsg = node.hasNonNull("errmsg") ? node.get("errmsg").asText() : "";

            if (errcodeNode != null && errcodeNode.asInt(-1) == 0) {
                log.info("钉钉消息发送成功, errmsg: {}", errmsg);
                return new DingTalkResult(true, errmsg);
            }

            int errcode = errcodeNode != null ? errcodeNode.asInt(-1) : -1;
            log.error("钉钉消息发送失败, errcode: {}, errmsg: {}", errcode, errmsg);
            return new DingTalkResult(false, "钉钉返回errcode=" + errcode + ", errmsg=" + errmsg);
        } catch (Exception e) {
            log.error("钉钉消息发送失败: {}", e.getMessage(), e);
            return new DingTalkResult(false, "钉钉发送异常: " + e.getMessage());
        }
    }
}
