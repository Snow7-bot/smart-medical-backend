-- ============================================
-- 板块二：AI 智能问诊平台 - 数据库初始化
-- ============================================

USE smart_medical;

-- 对话表
CREATE TABLE IF NOT EXISTS `conversation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `title`       VARCHAR(100) DEFAULT NULL COMMENT '对话标题（首条症状摘要）',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active/ended',
    `group_id`    BIGINT       DEFAULT NULL COMMENT '分组ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊对话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT       NOT NULL,
    `role`            VARCHAR(20)  NOT NULL COMMENT 'user/ai',
    `content`         TEXT         NOT NULL,
    `metadata`        JSON         DEFAULT NULL COMMENT '紧急标记等',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_conv_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊消息表';

-- 对话分组表
CREATE TABLE IF NOT EXISTS `conversation_group` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `name`        VARCHAR(50)  NOT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话分组表';

-- 附件表
CREATE TABLE IF NOT EXISTS `attachment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `file_name`   VARCHAR(255) NOT NULL,
    `file_url`    VARCHAR(500) NOT NULL,
    `file_size`   BIGINT       DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';
