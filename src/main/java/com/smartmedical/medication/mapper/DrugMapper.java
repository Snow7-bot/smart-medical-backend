package com.smartmedical.medication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmedical.medication.entity.Drug;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DrugMapper extends BaseMapper<Drug> {}
