package com.smartmedical.consultation.controller;

import com.smartmedical.common.ApiResponse;
import com.smartmedical.consultation.entity.Attachment;
import com.smartmedical.consultation.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentMapper attachmentMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping
    public ApiResponse<Map<String, String>> upload(@AuthenticationPrincipal Long userId,
                                                    @RequestParam("file") MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String uuid = UUID.randomUUID().toString();
        String ext = file.getOriginalFilename() != null
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                : "";
        String fileName = uuid + ext;
        file.transferTo(new File(dir, fileName));

        Attachment att = new Attachment();
        att.setUserId(userId);
        att.setFileName(file.getOriginalFilename());
        att.setFileUrl("/uploads/" + fileName);
        att.setFileSize(file.getSize());
        attachmentMapper.insert(att);

        return ApiResponse.ok(Map.of("url", att.getFileUrl(), "id", att.getId().toString()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id) {
        attachmentMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
