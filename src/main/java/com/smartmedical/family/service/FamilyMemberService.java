package com.smartmedical.family.service;

import com.smartmedical.family.dto.MemberRequest;
import com.smartmedical.family.entity.FamilyMember;

import java.util.List;

public interface FamilyMemberService {

    /** 获取当前用户的家庭成员列表 */
    List<FamilyMember> getList(Long userId);

    /** 添加家庭成员 */
    FamilyMember add(Long userId, MemberRequest request);

    /** 删除家庭成员 */
    void delete(Long userId, Long memberId);

    /** 成员详情 */
    FamilyMember getDetail(Long userId, Long memberId);
}
