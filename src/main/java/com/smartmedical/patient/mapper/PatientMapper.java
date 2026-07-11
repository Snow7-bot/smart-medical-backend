package com.smartmedical.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmedical.patient.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {}
