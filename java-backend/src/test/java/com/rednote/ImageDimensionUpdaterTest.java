package com.rednote;

import com.rednote.entity.Post;
import com.rednote.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

@SpringBootTest
public class ImageDimensionUpdaterTest {

    @Autowired
    private PostService postService;

    @Test
    public void updateImageDimensions() {
        // 1. Fetch all posts
        List<Post> posts = postService.list();
        System.out.println("Found " + posts.size() + " posts.");

        int updatedCount = 0;
        for (Post post : posts) {
            List<String> images = post.getImages();
            if (images != null && !images.isEmpty()) {
                String firstImageUrl = images.get(0);
                try {
                    System.out.println("Processing Post ID " + post.getId() + "...");
                    // 2. Get dimensions
                    URL url = new URL(firstImageUrl);
                    BufferedImage image = ImageIO.read(url);
                    if (image != null) {
                        int width = image.getWidth();
                        int height = image.getHeight();

                        // 3. Update post
                        post.setImgWidth(width);
                        post.setImgHeight(height);
                        postService.updateById(post);

                        System.out.println("Updated Post ID " + post.getId() + ": " + width + "x" + height);
                        updatedCount++;
                    } else {
                        System.err.println("Could not read image for Post ID " + post.getId() + ": " + firstImageUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing Post ID " + post.getId() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Finished updating " + updatedCount + " posts.");
    }
}
