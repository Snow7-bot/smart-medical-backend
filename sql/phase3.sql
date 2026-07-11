-- ============================================
-- 板块三：个人及家庭健康管理 - 数据库初始化
-- ============================================

USE smart_medical;

-- 健康指标记录表
CREATE TABLE IF NOT EXISTS `health_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `member_id`   BIGINT       NOT NULL COMMENT '家庭成员ID',
    `type`        VARCHAR(30)  NOT NULL COMMENT '指标类型：blood_pressure/glucose/heart_rate/SpO2/temperature/weight',
    `value`       JSON         NOT NULL COMMENT '指标值（复合指标如血压用JSON）',
    `unit`        VARCHAR(10)  DEFAULT NULL COMMENT '单位',
    `recorded_at` DATETIME     NOT NULL COMMENT '记录时间（可手动指定）',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_member` (`user_id`, `member_id`),
    INDEX `idx_type_time` (`type`, `recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标记录表';
