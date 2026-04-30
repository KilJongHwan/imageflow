package com.imageflow.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.imageflow.backend.common.storage.StorageService;
import com.imageflow.backend.queue.ImageJobQueuePublisher;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StorageService storageService;

    @MockBean
    private ImageJobQueuePublisher imageJobQueuePublisher;

    @Test
    void signsUpUploadsImageJobAndReadsItBack() throws Exception {
        String token = signupAndVerify("hello@imageflow.dev");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "single.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        MvcResult createImageJobResult = mockMvc.perform(multipart("/api/image-jobs/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token)
                        .param("width", "400")
                        .param("quality", "70"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.creditsUsed").value(1))
                .andExpect(jsonPath("$.targetWidth").value(400))
                .andExpect(jsonPath("$.quality").value(70))
                .andExpect(jsonPath("$.outputFormat").value("jpg"))
                .andReturn();

        String imageJobId = JsonTestUtils.read(createImageJobResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(patch("/api/image-jobs/{imageJobId}/result", imageJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUCCEEDED",
                                  "resultImageUrl": "http://localhost/result.jpg",
                                  "outputObjectKey": "optimized/%s.jpg",
                                  "sourceFileSizeBytes": 1200,
                                  "resultFileSizeBytes": 800
                                }
                                """.formatted(imageJobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/image-jobs/{imageJobId}", imageJobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageJobId))
                .andExpect(jsonPath("$.prompt").value("Simple optimize upload"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void createsPromptBasedImageGenerationJobWhenWorkerQueueIsEnabled() throws Exception {
        String token = signupAndVerify("prompt@imageflow.dev");

        mockMvc.perform(post("/api/image-jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "Turn this product shot into a clean banner",
                                  "creditsToUse": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void rejectsImageJobWhenUserDoesNotHaveEnoughCredits() throws Exception {
        String token = signupAndVerify("free@imageflow.dev");

        MockMultipartFile[] batch = tenPngFiles();

        mockMvc.perform(multipart("/api/image-jobs/uploads")
                        .file(batch[0])
                        .file(batch[1])
                        .file(batch[2])
                        .file(batch[3])
                        .file(batch[4])
                        .file(batch[5])
                        .file(batch[6])
                        .file(batch[7])
                        .file(batch[8])
                        .file(batch[9])
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/image-jobs/uploads")
                        .file(batch[0])
                        .file(batch[1])
                        .file(batch[2])
                        .file(batch[3])
                        .file(batch[4])
                        .file(batch[5])
                        .file(batch[6])
                        .file(batch[7])
                        .file(batch[8])
                        .file(batch[9])
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/image-jobs/upload")
                        .file(new MockMultipartFile("file", "overflow.png", MediaType.IMAGE_PNG_VALUE, pngBytes()))
                        .header("Authorization", "Bearer " + token))
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
        String token = signupAndVerify("batch@imageflow.dev");

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
                .andExpect(jsonPath("$.succeededCount").value(0))
                .andReturn();

        String firstJobId = JsonTestUtils.read(uploadResult.getResponse().getContentAsString(), "jobs[0].id");
        String secondJobId = JsonTestUtils.read(uploadResult.getResponse().getContentAsString(), "jobs[1].id");

        markJobSucceeded(firstJobId, "optimized/" + firstJobId + ".jpg");
        markJobSucceeded(secondJobId, "optimized/" + secondJobId + ".jpg");

        mockMvc.perform(get("/api/image-jobs/download")
                        .header("Authorization", "Bearer " + token)
                        .param("jobIds", firstJobId, secondJobId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"imageflow-batch.zip\""));
    }

    @Test
    void rejectsZipArchiveWithTooManyEntries() throws Exception {
        String token = signupAndVerify("zip-limit@imageflow.dev");

        MockMultipartFile zipArchive = new MockMultipartFile(
                "files",
                "batch.zip",
                "application/zip",
                zipBytes(4)
        );

        mockMvc.perform(multipart("/api/image-jobs/uploads")
                        .file(zipArchive)
                        .header("Authorization", "Bearer " + token)
                        .param("width", "400")
                        .param("quality", "80"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("zip archive contains too many entries"));
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

    private byte[] zipBytes(int imageCount) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (int index = 0; index < imageCount; index++) {
                zipOutputStream.putNextEntry(new ZipEntry("image-" + index + ".png"));
                zipOutputStream.write(pngBytes());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to generate zip bytes for test", exception);
        }
    }

    private MockMultipartFile[] tenPngFiles() {
        MockMultipartFile[] files = new MockMultipartFile[10];
        for (int index = 0; index < files.length; index++) {
            files[index] = new MockMultipartFile(
                    "files",
                    "batch-" + index + ".png",
                    MediaType.IMAGE_PNG_VALUE,
                    pngBytes()
            );
        }
        return files;
    }

    private void markJobSucceeded(String imageJobId, String outputObjectKey) throws Exception {
        String filename = fileNameOf(outputObjectKey);
        Files.write(storageService.resolveOutputFile(filename), pngBytes());
        mockMvc.perform(patch("/api/image-jobs/{imageJobId}/result", imageJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUCCEEDED",
                                  "resultImageUrl": "http://localhost/%s",
                                  "outputObjectKey": "%s",
                                  "sourceFileSizeBytes": 1200,
                                  "resultFileSizeBytes": 800
                                }
                                """.formatted(filename, outputObjectKey)))
                .andExpect(status().isOk());
    }

    private String fileNameOf(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String signupAndVerify(String email) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                .andReturn();

        return JsonTestUtils.read(signupResult.getResponse().getContentAsString(), "token");
    }
}
