package com.smartmedical.consultation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmedical.consultation.entity.ConversationGroup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapper<ConversationGroup> {}
