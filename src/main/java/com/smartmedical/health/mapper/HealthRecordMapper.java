package com.smartmedical.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmedical.health.entity.HealthRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {}
