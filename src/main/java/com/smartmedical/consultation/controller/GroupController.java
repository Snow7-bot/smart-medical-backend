package com.smartmedical.consultation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.ApiResponse;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.consultation.entity.Conversation;
import com.smartmedical.consultation.entity.ConversationGroup;
import com.smartmedical.consultation.mapper.ConversationMapper;
import com.smartmedical.consultation.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupMapper groupMapper;
    private final ConversationMapper convMapper;

    @PostMapping
    public ApiResponse<ConversationGroup> create(@AuthenticationPrincipal Long userId,
                                                  @RequestBody Map<String, Object> body) {
        ConversationGroup g = new ConversationGroup();
        g.setUserId(userId);
        g.setName(body.get("name").toString());
        groupMapper.insert(g);
        return ApiResponse.ok(g);
    }

    @GetMapping
    public ApiResponse<List<ConversationGroup>> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(groupMapper.selectList(
                new LambdaQueryWrapper<ConversationGroup>().eq(ConversationGroup::getUserId, userId)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        ConversationGroup g = groupMapper.selectById(id);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        g.setName(body.get("name").toString());
        groupMapper.updateById(g);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id) {
        ConversationGroup g = groupMapper.selectById(id);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        groupMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{groupId}/conversations/{convId}")
    public ApiResponse<Void> moveIn(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long groupId,
                                     @PathVariable Long convId) {
        ConversationGroup g = groupMapper.selectById(groupId);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Conversation conv = convMapper.selectById(convId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        conv.setGroupId(groupId);
        convMapper.updateById(conv);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{groupId}/conversations/{convId}")
    public ApiResponse<Void> removeFromGroup(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long groupId,
                                              @PathVariable Long convId) {
        ConversationGroup g = groupMapper.selectById(groupId);
        if (g == null || !g.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Conversation conv = convMapper.selectById(convId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        conv.setGroupId(null);
        convMapper.updateById(conv);
        return ApiResponse.ok(null);
    }
}
