package com.imageflow.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signsUpCreatesImageJobAndReadsItBack() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hello@imageflow.dev",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("hello@imageflow.dev"))
                .andExpect(jsonPath("$.user.plan").value("FREE"))
                .andReturn();

        String token = JsonTestUtils.read(signupResult.getResponse().getContentAsString(), "token");

        MvcResult createImageJobResult = mockMvc.perform(post("/api/image-jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "Turn this product shot into a clean banner",
                                  "sourceImageUrl": "https://example.com/source.png",
                                  "creditsToUse": 3,
                                  "targetWidth": 400,
                                  "quality": 70,
                                  "outputFormat": "webp"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.creditsUsed").value(3))
                .andExpect(jsonPath("$.targetWidth").value(400))
                .andExpect(jsonPath("$.quality").value(70))
                .andExpect(jsonPath("$.outputFormat").value("webp"))
                .andReturn();

        String imageJobId = JsonTestUtils.read(createImageJobResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/image-jobs/{imageJobId}", imageJobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageJobId))
                .andExpect(jsonPath("$.prompt").value("Turn this product shot into a clean banner"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void rejectsImageJobWhenUserDoesNotHaveEnoughCredits() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "free@imageflow.dev",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String token = JsonTestUtils.read(signupResult.getResponse().getContentAsString(), "token");

        mockMvc.perform(post("/api/image-jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "Need a high-end product composite",
                                  "creditsToUse": 25
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("not enough credits"));
    }

    @Test
    void rejectsImageJobAccessWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/image-jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void uploadsMultipleImagesAndDownloadsZip() throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "batch@imageflow.dev",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String token = JsonTestUtils.read(signupResult.getResponse().getContentAsString(), "token");

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "first.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "second.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/image-jobs/uploads")
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + token)
                        .param("width", "400")
                        .param("quality", "80"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.succeededCount").value(2))
                .andReturn();

        String firstJobId = JsonTestUtils.read(uploadResult.getResponse().getContentAsString(), "jobs[0].id");
        String secondJobId = JsonTestUtils.read(uploadResult.getResponse().getContentAsString(), "jobs[1].id");

        mockMvc.perform(get("/api/image-jobs/download")
                        .header("Authorization", "Bearer " + token)
                        .param("jobIds", firstJobId, secondJobId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"imageflow-batch.zip\""));
    }

    private byte[] pngBytes() {
        try {
            BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setRGB(x, y, (x + y) % 2 == 0 ? Color.WHITE.getRGB() : new Color(216, 93, 53).getRGB());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to generate png bytes for test", exception);
        }
    }
}
