-- ============================================
-- 板块五：患者病历与档案中心 - 数据库初始化
-- ============================================

USE smart_medical;

CREATE TABLE IF NOT EXISTS `patient` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL COMMENT '所属用户',
    `name`            VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`          TINYINT      DEFAULT 0 COMMENT '0未知 1男 2女',
    `birth_date`      DATE         DEFAULT NULL COMMENT '出生日期',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `medical_history` TEXT         DEFAULT NULL COMMENT '既往病史',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者表';

CREATE TABLE IF NOT EXISTS `medical_record` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `patient_id`   BIGINT       NOT NULL COMMENT '所属患者',
    `file_url`     VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    `file_type`    VARCHAR(20)  DEFAULT NULL COMMENT 'pdf/image',
    `parsed_data`  TEXT         DEFAULT NULL COMMENT '解析结果JSON',
    `parse_status` VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/done/failed',
    `record_date`  DATE         DEFAULT NULL COMMENT '病历日期',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_patient_id` (`patient_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历表';
