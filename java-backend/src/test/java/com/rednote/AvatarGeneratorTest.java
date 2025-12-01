package com.rednote;

import com.rednote.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class AvatarGeneratorTest {

    @Autowired
    private UserService userService;

    private static final String AVATAR_DIR = "src/main/resources/avatar";

    @Test
    public void generateAvatars() throws Exception {
        File dir = new File(AVATAR_DIR);
        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No avatars found in " + AVATAR_DIR);
            return;
        }

        // Filter valid images
        List<File> fileList = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (f.isFile() && (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")
                    || name.endsWith(".heif"))) {
                fileList.add(f);
            }
        }

        System.out.println("Found " + fileList.size() + " avatar images.");

        // Target User IDs: 6 to 33, excluding 7
        List<Long> userIds = new ArrayList<>();
        for (long i = 6; i <= 33; i++) {
            if (i != 7) {
                userIds.add(i);
            }
        }

        System.out.println("Target User IDs: " + userIds);

        int count = 0;
        for (int i = 0; i < userIds.size(); i++) {
            if (i >= fileList.size()) {
                System.out.println("Not enough images for all users. Stopping at user index " + i);
                break;
            }

            Long userId = userIds.get(i);
            File imageFile = fileList.get(i);

            System.out.println("Processing User ID: " + userId + " with image: " + imageFile.getName());

            try (FileInputStream input = new FileInputStream(imageFile)) {
                MultipartFile multipartFile = new MockMultipartFile(
                        "avatar",
                        imageFile.getName(),
                        "image/jpeg",
                        input);

                // Call the service method which handles upload + db update
                String resultUrl = userService.uploadAvatar(multipartFile, userId);
                System.out.println("Successfully updated avatar for User ID " + userId + ": " + resultUrl);
                count++;
            } catch (Exception e) {
                System.err.println("Failed to update avatar for User ID " + userId);
                e.printStackTrace();
            }
        }

        System.out.println("Avatar generation completed! Updated " + count + " users.");
    }
}
