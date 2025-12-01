package com.rednote;

import com.rednote.entity.Comment;
import com.rednote.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import com.rednote.common.UserContext;
import com.rednote.entity.dto.AddCommentDTO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class CommentGenerationTest {

    @Autowired
    private CommentService commentService;

    private static final String PICTURES_DIR = "src/main/resources/picture";
    private static final Random RANDOM = new Random();

    private static final String[] COMMENT_CONTENTS = {
            "这也太好看了吧！😍", "绝绝子！", "爱了爱了❤️", "求教程！", "这是哪里呀？",
            "拍照技术真好📷", "哇，好想去！", "收藏了✨", "博主好美/帅！", "这就是向往的生活吧~",
            "太治愈了🌿", "心情瞬间变好了", "可以做壁纸了", "一定要去一次！", "羡慕哭了😭",
            "氛围感拉满💯", "这是什么神仙地方", "好喜欢这种风格", "拍出了电影感🎬", "我也想拍同款",
            "太有感觉了", "美哭了", "这是在人间吗？", "好温柔的画面", "一眼万年",
            "这个色调好喜欢", "怎么拍的呀？", "求原图！", "太赞了👍", "必须点赞",
            "不仅风景美，人更美", "看着就好舒服", "想去这里发呆", "这里是天堂吗？", "被种草了🌱",
            "好想拥有同款", "太会拍了", "每一张都是大片", "这个构图绝了", "光影太美了",
            "这是什么神仙滤镜", "好想去这里散步", "感觉时间都慢下来了", "这里好适合拍照", "太有生活气息了",
            "这就是我想要的生活", "好想去这里度假", "这里好适合约会", "太浪漫了💕", "这里好适合放空"
    };

    @Test
    public void generateComments() throws Exception {
        File dir = new File(PICTURES_DIR);
        File[] files = dir
                .listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.out.println("No pictures found in " + PICTURES_DIR);
            return;
        }

        List<File> fileList = new ArrayList<>();
        Collections.addAll(fileList, files);

        // Post ID 150 ~ 161
        for (long postId = 150; postId <= 161; postId++) {
            System.out.println("Generating comments for post " + postId + "...");

            // User ID 6 ~ 33
            for (long userId = 6; userId <= 33; userId++) {
                // 每个用户发 0-2 条一级评论
                int rootCommentCount = RANDOM.nextInt(3);

                for (int k = 0; k < rootCommentCount; k++) {
                    // 1. 创建一级评论
                    Comment rootComment = createComment(postId, userId, null, null, fileList);

                    // 2. 创建回复 (多级评论)
                    // 随机生成 0-5 条回复
                    int replyCount = RANDOM.nextInt(6);
                    List<Comment> currentThreadComments = new ArrayList<>();
                    currentThreadComments.add(rootComment);

                    for (int r = 0; r < replyCount; r++) {
                        // 随机选择一个回复对象 (可以是根评论，也可以是之前的回复)
                        Comment parent = currentThreadComments.get(RANDOM.nextInt(currentThreadComments.size()));

                        // 回复者ID (随机选择一个其他用户)
                        long replyUserId = 6 + RANDOM.nextInt(28);
                        while (replyUserId == parent.getUserId()) {
                            replyUserId = 6 + RANDOM.nextInt(28);
                        }

                        // 创建回复
                        Comment reply = createComment(postId, replyUserId, rootComment.getId(), parent, null); // 回复通常不带图
                        currentThreadComments.add(reply);
                    }
                }
            }
        }
        System.out.println("Comment generation completed!");
    }

    private Comment createComment(Long postId, Long userId, Long rootParentId, Comment parent, List<File> fileList) {
        // 模拟用户登录
        UserContext.setUserId(userId);

        AddCommentDTO dto = new AddCommentDTO();
        dto.setPostId(postId);
        dto.setContent(COMMENT_CONTENTS[RANDOM.nextInt(COMMENT_CONTENTS.length)]);

        if (rootParentId != null) {
            dto.setRootParentId(rootParentId);
            if (parent != null) {
                dto.setParentId(parent.getId());
                dto.setReplyToUserId(parent.getUserId());
            }
        }

        MultipartFile multipartFile = null;

        // 随机上传图片 (仅一级评论且 fileList 不为空时)
        if (fileList != null && !fileList.isEmpty() && RANDOM.nextBoolean()) {
            File imageFile = fileList.get(RANDOM.nextInt(fileList.size()));
            try {
                // 读取宽高
                BufferedImage image = ImageIO.read(imageFile);
                if (image != null) {
                    dto.setImageWidth(image.getWidth());
                    dto.setImageHeight(image.getHeight());
                }

                try (FileInputStream input2 = new FileInputStream(imageFile)) {
                    multipartFile = new MockMultipartFile(
                            imageFile.getName(),
                            imageFile.getName(),
                            "image/jpeg",
                            input2);
                }
            } catch (Exception e) {
                System.err.println("Failed to process image: " + imageFile.getName());
                e.printStackTrace();
            }
        }

        Comment comment = commentService.publishComment(dto, multipartFile);
        UserContext.clear();
        return comment;
    }
}
