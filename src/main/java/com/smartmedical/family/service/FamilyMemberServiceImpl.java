package com.smartmedical.family.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.family.dto.MemberRequest;
import com.smartmedical.family.entity.FamilyMember;
import com.smartmedical.family.mapper.FamilyMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyMemberServiceImpl implements FamilyMemberService {

    private final FamilyMemberMapper memberMapper;
    private static final int MAX_MEMBERS = 10;

    @Override
    public List<FamilyMember> getList(Long userId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId));
    }

    @Override
    @Transactional
    public FamilyMember add(Long userId, MemberRequest request) {
        long count = memberMapper.selectCount(
                new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId));
        if (count >= MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.MEMBER_LIMIT_EXCEEDED);
        }

        FamilyMember member = new FamilyMember();
        member.setUserId(userId);
        member.setName(request.getName());
        member.setRelation(request.getRelation());
        member.setGender(request.getGender());
        member.setBirthDate(request.getBirthDate());
        member.setAvatarUrl(request.getAvatarUrl());
        memberMapper.insert(member);
        return member;
    }

    @Override
    public void delete(Long userId, Long memberId) {
        FamilyMember member = memberMapper.selectById(memberId);
        if (member == null || !member.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        memberMapper.deleteById(memberId);
    }

    @Override
    public FamilyMember getDetail(Long userId, Long memberId) {
        FamilyMember member = memberMapper.selectById(memberId);
        if (member == null || !member.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
