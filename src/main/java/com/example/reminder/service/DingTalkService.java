package com.example.reminder.service;

import com.example.reminder.dto.DingTalkSendResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通过钉钉机器人Webhook发送消息。
     * 仅当响应 JSON 中 errcode == 0 时视为成功，否则将 errmsg 作为失败原因返回。
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
            log.info("钉钉接口响应: {}", response);

            if (response == null || response.trim().isEmpty()) {
                return DingTalkSendResult.fail("钉钉接口返回为空");
            }

            JsonNode node = objectMapper.readTree(response);
            JsonNode errcodeNode = node.get("errcode");
            if (errcodeNode == null || !errcodeNode.isNumber()) {
                return DingTalkSendResult.fail("钉钉接口返回缺少errcode: " + response);
            }

            int errcode = errcodeNode.asInt();
            if (errcode == 0) {
                return DingTalkSendResult.ok();
            }

            String errmsg = node.has("errmsg") && !node.get("errmsg").isNull()
                    ? node.get("errmsg").asText()
                    : "钉钉接口返回失败(errcode=" + errcode + ")";
            return DingTalkSendResult.fail(errmsg);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("钉钉消息发送HTTP失败: status={}, body={}", e.getRawStatusCode(), body, e);
            return DingTalkSendResult.fail("钉钉接口HTTP错误(" + e.getRawStatusCode() + "): " + body);
        } catch (Exception e) {
            log.error("钉钉消息发送失败: {}", e.getMessage(), e);
            return DingTalkSendResult.fail(e.getMessage());
        }
    }
}
