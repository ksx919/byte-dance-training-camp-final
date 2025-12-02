# RedNote (小红书仿写项目)

## 项目简介

RedNote 是一个基于 Android (Kotlin) 和 Java (Spring Boot) 开发的全栈仿小红书应用。本项目旨在还原小红书的核心功能，包括双列瀑布流 Feed、图文发布、富文本详情页、评论互动、点赞收藏以及用户系统等。

项目采用经典的前后端分离架构，客户端使用 MVVM 模式，服务端基于 Spring Boot 和 MyBatis-Plus 构建。

## 功能特性

### 客户端 (Android)

- **首页 Feed 流**: 采用 StaggeredGridLayoutManager 实现双列瀑布流，支持下拉刷新、上拉加载更多。
- **沉浸式详情页**: 支持多图轮播、富文本内容展示、滑动查看评论。
- **内容发布**: 支持多图选择、预览、裁剪，实时上传至阿里云 OSS。
- **互动体系**: 支持点赞（帖子/评论）、收藏、二级评论回复。
- **用户系统**: 登录注册（JWT 鉴权）、个人主页、我的草稿箱、我的点赞/收藏列表。
- **性能优化**: 使用 `FeedViewPool` 复用 RecyclerView，移除冗余动画，优化图片加载。App性能优化方式：使用perfetto-trace进行性能测试，并进行优化。

### 服务端 (Backend)

- **RESTful API**: 提供标准化的 JSON 接口。
- **安全认证**: 基于 JWT 的用户身份认证与拦截器鉴权。
- **数据存储**: MySQL 存储业务数据，阿里云 OSS 存储图片资源。
- **高效查询**: 使用 MyBatis-Plus 进行数据访问，支持分页查询。

## 技术栈

### Android Client

- **语言**: Kotlin
- **架构**: MVVM (Model-View-ViewModel)
- **UI 组件**: RecyclerView, ConstraintLayout, Material Design, SwipeRefreshLayout
- **网络**: Retrofit 2, OkHttp 3, Gson
- **图片加载**: Glide, PhotoView
- **异步处理**: Coroutines, Flow
- **本地存储**: MMKV, Room (部分)
- **其他**: WorkManager, Amap Location (高德定位)

### Java Backend

- **语言**: Java 21
- **框架**: Spring Boot 3.3.5
- **ORM**: MyBatis-Plus 3.5.7
- **数据库**: MySQL 8.0
- **连接池**: Druid
- **对象存储**: Aliyun OSS
- **工具库**: Hutool, Lombok, JJWT (0.11.5)

### 数据库设计 (Schema)

<details>
<summary>点击展开查看 SQL 建表语句</summary>

```sql
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
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '直接父评论ID (回复了谁)',
  `root_parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '顶级根评论ID (为了快速聚合楼中楼)',
  `reply_to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被回复的用户ID (方便前端显示 "回复 @某某")',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT '评论图片URL',
  `image_width` INT UNSIGNED DEFAULT 0 COMMENT '图片宽度',
  `image_height` INT UNSIGNED DEFAULT 0 COMMENT '图片高度',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`) USING BTREE COMMENT '查询某帖子的评论',
  KEY `idx_root_parent` (`root_parent_id`) USING BTREE COMMENT '查询某根评论下的所有回复'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 帖子点赞记录表
CREATE TABLE IF NOT EXISTS `post_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的帖子ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`), 
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞记录表';

-- 评论点赞记录表
CREATE TABLE IF NOT EXISTS `comment_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的评论ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`), 
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录表';
```

</details>

## 快速开始

### 1. 环境准备

- Android Studio Ladybug | 2024.2.1 或更高版本
- JDK 21
- MySQL 8.0
- Maven 3.8+

### 2. 后端部署

1. 进入 `java-backend` 目录。

2. 配置 `src/main/resources/application.yml`，填入** MySQL 和 Aliyun OSS **配置。

3. 运行 `RedNoteApplication` 启动服务。
   
   ```bash
   mvn spring-boot:run
   ```

### 3. 客户端运行

1. 使用 Android Studio 打开 `app` 目录。

2. 在 `local.properties` 中配置高德地图 API Key (如果需要定位功能):
   
   ```properties
   AMAP_WEB_API_KEY=your_web_api_key
   AMAP_API_KEY=your_android_api_key
   ```

3. 修改 `RetrofitClient.kt` 中的 `BASE_URL` 为你的后端服务地址 (如 `http://10.0.2.2:8080/` 用于模拟器)。

4. 连接设备或模拟器，运行 `app`。

## 目录结构与核心类图

详细的代码架构说明和核心类图请参考 [ARCHITECTURE.md](./ARCHITECTURE.md)。
