package com.smartmedical.patient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartmedical.common.BusinessException;
import com.smartmedical.common.ErrorCode;
import com.smartmedical.patient.entity.Patient;
import com.smartmedical.patient.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientMapper mapper;

    public Patient create(Long userId, Map<String, Object> body) {
        Patient p = new Patient();
        p.setUserId(userId);
        p.setName(body.get("name").toString());
        if (body.get("gender") != null) p.setGender(Integer.valueOf(body.get("gender").toString()));
        if (body.get("birthDate") != null) p.setBirthDate(LocalDate.parse(body.get("birthDate").toString()));
        if (body.get("phone") != null) p.setPhone(body.get("phone").toString());
        if (body.get("medicalHistory") != null) p.setMedicalHistory(body.get("medicalHistory").toString());
        mapper.insert(p);
        return p;
    }

    public List<Patient> list(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, userId));
    }

    public Patient detail(Long userId, Long id) {
        Patient p = mapper.selectById(id);
        if (p == null || !p.getUserId().equals(userId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        return p;
    }

    public void update(Long userId, Long id, Map<String, Object> body) {
        Patient p = mapper.selectById(id);
        if (p == null || !p.getUserId().equals(userId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (body.get("name") != null) p.setName(body.get("name").toString());
        if (body.get("gender") != null) p.setGender(Integer.valueOf(body.get("gender").toString()));
        if (body.get("birthDate") != null) p.setBirthDate(LocalDate.parse(body.get("birthDate").toString()));
        if (body.get("phone") != null) p.setPhone(body.get("phone").toString());
        if (body.get("medicalHistory") != null) p.setMedicalHistory(body.get("medicalHistory").toString());
        mapper.updateById(p);
    }

    public void delete(Long userId, Long id) {
        Patient p = mapper.selectById(id);
        if (p == null || !p.getUserId().equals(userId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        mapper.deleteById(id);
    }
}
