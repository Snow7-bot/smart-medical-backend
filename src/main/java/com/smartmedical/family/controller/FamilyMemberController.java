package com.smartmedical.family.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.family.dto.MemberRequest;
import com.smartmedical.family.entity.FamilyMember;
import com.smartmedical.family.service.FamilyMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService memberService;

    /** 家庭成员列表 */
    @GetMapping("/list")
    public ApiResponse<List<FamilyMember>> getList(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(memberService.getList(userId));
    }

    /** 添加成员 */
    @PostMapping("/add")
    public ApiResponse<FamilyMember> add(@AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody MemberRequest request) {
        return ApiResponse.ok(memberService.add(userId, request));
    }

    /** 删除成员 */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable Long id) {
        memberService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    /** 成员详情 */
    @GetMapping("/detail/{id}")
    public ApiResponse<FamilyMember> getDetail(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id) {
        return ApiResponse.ok(memberService.getDetail(userId, id));
    }
}
