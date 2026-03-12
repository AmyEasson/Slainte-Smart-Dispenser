package com.vitamindispenser.backend.integration;

import com.vitamindispenser.backend.repository.DeviceRepository;
import com.vitamindispenser.backend.repository.ScheduleEntryRepository;
import com.vitamindispenser.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        scheduleEntryRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();

        // Register and login to get a token
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "testuser", "password", "testpassword"))))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "testuser", "password", "testpassword"))))
                .andExpect(status().isOk())
                .andReturn();

        token = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("token").asText();

        // Claim device
        mockMvc.perform(post("/api/mobile/claim-device")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("deviceId", "DISPENSER_001"))))
                .andExpect(status().isOk());
    }

    @Test
    void saveAndRetrieveSchedule_returnsCorrectData() throws Exception {
        // Save schedule
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "vitamins": [
                            {
                              "vitaminType": "Vitamin C",
                              "numberOfPills": 1,
                              "schedule": [
                                {"day": "monday", "times": ["09:00"]}
                              ]
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk());

        // Retrieve and verify
        mockMvc.perform(get("/api/mobile/getSchedule")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitamins[0].vitaminType").value("Vitamin C"))
                .andExpect(jsonPath("$.vitamins[0].numberOfPills").value(1))
                .andExpect(jsonPath("$.vitamins[0].schedule[0].day").value("monday"))
                .andExpect(jsonPath("$.vitamins[0].schedule[0].times[0]").value("09:00"));
    }

    @Test
    void saveSchedule_overwritesPreviousSchedule() throws Exception {
        // Save first schedule
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "vitamins": [
                            {
                              "vitaminType": "Vitamin C",
                              "numberOfPills": 1,
                              "schedule": [
                                {"day": "monday", "times": ["09:00"]}
                              ]
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk());

        // Overwrite with new schedule
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "vitamins": [
                            {
                              "vitaminType": "Magnesium",
                              "numberOfPills": 2,
                              "schedule": [
                                {"day": "friday", "times": ["21:00"]}
                              ]
                            }
                          ]
                        }
                        """))
                .andExpect(status().isOk());

        // Verify only new schedule exists
        mockMvc.perform(get("/api/mobile/getSchedule")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitamins.length()").value(1))
                .andExpect(jsonPath("$.vitamins[0].vitaminType").value("Magnesium"));
    }

    @Test
    void getSchedule_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/mobile/getSchedule"))
                .andExpect(status().isForbidden());
    }
}
