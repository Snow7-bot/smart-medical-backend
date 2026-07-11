-- ============================================
-- 板块一：统一身份与权限中心 - 数据库初始化
-- ============================================

CREATE DATABASE IF NOT EXISTS smart_medical
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_medical;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `name`        VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar_url`  VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender`      TINYINT      DEFAULT 0 COMMENT '性别：0=未知 1=男 2=女',
    `birth_date`  DATE         DEFAULT NULL COMMENT '出生日期',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 2=禁用 3=已注销',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '注销时间（软删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 家庭成员表
CREATE TABLE IF NOT EXISTS `family_member` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `user_id`     BIGINT       NOT NULL COMMENT '所属用户ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '姓名',
    `relation`    VARCHAR(20)  NOT NULL COMMENT '关系：本人/配偶/子女/父母/其他',
    `gender`      TINYINT      DEFAULT 0 COMMENT '性别：0=未知 1=男 2=女',
    `birth_date`  DATE         DEFAULT NULL COMMENT '出生日期',
    `avatar_url`  VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员表';
