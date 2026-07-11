-- ============================================
-- 板块四：智能用药安全体系 - 数据库初始化
-- ============================================

USE smart_medical;

CREATE TABLE IF NOT EXISTS `drug` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户',
    `member_id`   BIGINT       DEFAULT NULL COMMENT '服用成员ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '药品名称',
    `spec`        VARCHAR(50)  DEFAULT NULL COMMENT '规格',
    `dose`        VARCHAR(30)  DEFAULT NULL COMMENT '剂量',
    `freq`        VARCHAR(30)  DEFAULT NULL COMMENT '频率',
    `time_of_day` VARCHAR(30)  DEFAULT NULL COMMENT '服用时间',
    `warning`     TINYINT(1)   DEFAULT 0 COMMENT '是否警告',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品表';
