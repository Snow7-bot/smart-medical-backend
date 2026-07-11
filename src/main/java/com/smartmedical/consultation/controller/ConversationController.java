package com.smartmedical.consultation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.ApiResponse;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.consultation.entity.Conversation;
import com.smartmedical.consultation.entity.Message;
import com.smartmedical.consultation.mapper.ConversationMapper;
import com.smartmedical.consultation.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationMapper convMapper;
    private final MessageMapper msgMapper;

    @PostMapping("/conversations")
    public ApiResponse<Conversation> create(@AuthenticationPrincipal Long userId,
                                            @RequestBody Map<String, Object> body) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(body.get("title") != null ? body.get("title").toString() : "新对话");
        conv.setStatus("active");
        convMapper.insert(conv);
        return ApiResponse.ok(conv);
    }

    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> list(@AuthenticationPrincipal Long userId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        List<Conversation> list = convMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdatedAt)
                        .last("LIMIT " + size + " OFFSET " + (page * size)));
        return ApiResponse.ok(list);
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<Map<String, Object>> detail(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long id) {
        Conversation conv = convMapper.selectById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<Message> messages = msgMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, id)
                        .orderByAsc(Message::getCreatedAt));
        Map<String, Object> result = new HashMap<>();
        result.put("conversation", conv);
        result.put("messages", messages);
        return ApiResponse.ok(result);
    }

    @PutMapping("/conversations/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        Conversation conv = convMapper.selectById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (body.get("title") != null) conv.setTitle(body.get("title").toString());
        conv.setUpdatedAt(LocalDateTime.now());
        convMapper.updateById(conv);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id) {
        Conversation conv = convMapper.selectById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        msgMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, id));
        convMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<Message> sendMessage(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Object> body) {
        Conversation conv = convMapper.selectById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Message msg = new Message();
        msg.setConversationId(id);
        msg.setRole(body.get("role") != null ? body.get("role").toString() : "user");
        msg.setContent(body.get("content").toString());
        msgMapper.insert(msg);
        conv.setUpdatedAt(LocalDateTime.now());
        convMapper.updateById(conv);
        return ApiResponse.ok(msg);
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<Message>> getMessages(@AuthenticationPrincipal Long userId,
                                                   @PathVariable Long id) {
        Conversation conv = convMapper.selectById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return ApiResponse.ok(msgMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, id)
                        .orderByAsc(Message::getCreatedAt)));
    }

    @DeleteMapping("/conversations/{convId}/messages/{msgId}")
    public ApiResponse<Void> deleteMessage(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long convId,
                                            @PathVariable Long msgId) {
        Conversation conv = convMapper.selectById(convId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        msgMapper.deleteById(msgId);
        return ApiResponse.ok(null);
    }
}
