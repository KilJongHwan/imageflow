package com.imageflow.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsUserAndImageJobAndReadsItBack() throws Exception {
        MvcResult createUserResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hello@imageflow.dev",
                                  "plan": "PRO",
                                  "initialCredits": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("hello@imageflow.dev"))
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.creditBalance").value(10))
                .andReturn();

        String userId = JsonTestUtils.read(createUserResult.getResponse().getContentAsString(), "id");

        MvcResult createImageJobResult = mockMvc.perform(post("/api/image-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "prompt": "Turn this product shot into a clean banner",
                                  "sourceImageUrl": "https://example.com/source.png",
                                  "creditsToUse": 3
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.creditsUsed").value(3))
                .andReturn();

        String imageJobId = JsonTestUtils.read(createImageJobResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(get("/api/image-jobs/{imageJobId}", imageJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageJobId))
                .andExpect(jsonPath("$.prompt").value("Turn this product shot into a clean banner"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void rejectsImageJobWhenUserDoesNotHaveEnoughCredits() throws Exception {
        MvcResult createUserResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "free@imageflow.dev",
                                  "initialCredits": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = JsonTestUtils.read(createUserResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/api/image-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "prompt": "Need a high-end product composite",
                                  "creditsToUse": 5
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("not enough credits"));
    }
}
