-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` VARCHAR(128) NOT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '.' COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT '' COMMENT '头像链接',
  `bio` VARCHAR(255) DEFAULT '暂时还没有简介' COMMENT '个人简介(小红书的签名)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱必须唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 帖子/笔记表
CREATE TABLE IF NOT EXISTS `posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
  `title` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题(小红书标题通常较短)',
  `content` TEXT COMMENT '正文内容',
  `images` JSON COMMENT '图片URL集合，JSON数组格式 ["url1", "url2"]',
  `like_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '评论数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) USING BTREE COMMENT '用于查找某人的所有帖子',
  KEY `idx_created_at` (`created_at`) USING BTREE COMMENT '用于首页按时间流推荐'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子/笔记表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
  `content` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '评论内容',
  `like_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '点赞数',
  
  -- 核心：无限级评论的实现字段
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '直接父评论ID (回复了谁)',
  `root_parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '顶级根评论ID (为了快速聚合楼中楼)',
  `reply_to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被回复的用户ID (方便前端显示 "回复 @某某")',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`) USING BTREE COMMENT '查询某帖子的评论',
  KEY `idx_root_parent` (`root_parent_id`) USING BTREE COMMENT '查询某根评论下的所有回复'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 补充评论表字段 (图片支持)
-- 注意：如果表是新创建的，建议直接合并到 CREATE TABLE 中。这里保留 ALTER 语句以兼容旧表结构升级。
-- 检查列是否存在通常需要存储过程，这里直接尝试添加，如果已存在可能会报错，建议在全新环境运行。
ALTER TABLE `comments`
ADD COLUMN `image_url` VARCHAR(512) DEFAULT NULL COMMENT '评论图片URL (若为空则是纯文本)' AFTER `content`,
ADD COLUMN `image_width` INT UNSIGNED DEFAULT 0 COMMENT '图片宽度' AFTER `image_url`,
ADD COLUMN `image_height` INT UNSIGNED DEFAULT 0 COMMENT '图片高度' AFTER `image_width`;

-- 补充帖子表字段 (封面尺寸)
ALTER TABLE `posts` 
ADD COLUMN `img_width` INT UNSIGNED DEFAULT 0 COMMENT '封面图/首图宽度(px)' AFTER `images`,
ADD COLUMN `img_height` INT UNSIGNED DEFAULT 0 COMMENT '封面图/首图高度(px)' AFTER `img_width`;


-- 帖子点赞记录表
CREATE TABLE IF NOT EXISTS `post_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的帖子ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 核心：联合唯一索引。保证一个用户对同一个帖子只能点赞一次
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`), 
  KEY `idx_post_id` (`post_id`) -- 用于查询某帖子被谁点赞过
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞记录表';

-- 评论点赞记录表
CREATE TABLE IF NOT EXISTS `comment_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的评论ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 核心：联合唯一索引。保证一个用户对同一个帖子只能点赞一次
  UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`), 
  KEY `idx_comment_id` (`comment_id`) -- 用于查询某评论被谁点赞过
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录表';
