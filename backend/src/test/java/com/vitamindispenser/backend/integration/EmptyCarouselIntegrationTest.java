package com.vitamindispenser.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import com.vitamindispenser.backend.schedule.SlotRepository;
import com.vitamindispenser.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EmptyCarouselIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private SlotRepository slotRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;

    private static final String SCHEDULE = """
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
        """;

    @BeforeEach
    void setUp() throws Exception {
        slotRepository.deleteAll();
        scheduleEntryRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();

        com.vitamindispenser.backend.device.Device device = new com.vitamindispenser.backend.device.Device();
        device.setDeviceId("DISPENSER_001");
        deviceRepository.save(device);

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

        mockMvc.perform(post("/api/mobile/claim-device")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("deviceId", "DISPENSER_001"))))
                .andExpect(status().isOk());

        // Save a schedule and confirm fill so slots are assigned and lastFillDate is set
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SCHEDULE))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void emptyCarousel_withoutToken_returns403() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel"))
                .andExpect(status().isForbidden());
    }

    @Test
    void emptyCarousel_returns200() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void emptyCarousel_pollReturnsEmptyCommand() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("EMPTY"))
                .andExpect(jsonPath("$.intakeIds").isEmpty())
                .andExpect(jsonPath("$.slotNumber").doesNotExist());
    }

    @Test
    void emptyCarousel_secondPollReturnsIdle() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // First poll consumes the EMPTY command
        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("EMPTY"));

        // Second poll should be IDLE — command is one-shot
        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("IDLE"));
    }

    @Test
    void emptyCarousel_clearsAllSlotAssignments() throws Exception {
        // Verify slots are assigned before emptying
        MvcResult before = mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode beforeJson = objectMapper.readTree(before.getResponse().getContentAsString());
        assertNotNull(beforeJson.get("slots").get(0).get("assignedDay").asText());

        // Empty the carousel (poll to trigger clearSlots)
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("EMPTY"));

        // Slots should now be cleared
        MvcResult after = mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterJson = objectMapper.readTree(after.getResponse().getContentAsString());
        for (JsonNode slot : afterJson.get("slots")) {
            if (!slot.get("reserved").asBoolean()) {
                assertTrue(
                        slot.get("assignedDay").isNull(),
                        "Slot " + slot.get("slotNumber") + " assignedDay should be null after empty"
                );
                assertTrue(
                        slot.get("assignedTime").isNull(),
                        "Slot " + slot.get("slotNumber") + " assignedTime should be null after empty"
                );
            }
        }
    }

    @Test
    void emptyCarousel_clearsLastFillDate() throws Exception {
        // Verify lastFillDate is set before emptying
        MvcResult before = mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode beforeJson = objectMapper.readTree(before.getResponse().getContentAsString());
        assertFalse(beforeJson.get("lastFillDate").isNull(), "lastFillDate should be set before emptying");

        // Trigger empty via poll
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("EMPTY"));

        // lastFillDate will be returned as TODAY (not null)
        MvcResult after = mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterJson = objectMapper.readTree(after.getResponse().getContentAsString());

        String today = java.time.LocalDate.now().toString();

        assertEquals(
                today,
                afterJson.get("lastFillDate").asText(),
                "lastFillDate should default to today after empty"
        );
    }

    @Test
    void emptyCarousel_slotCount_remains15() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(15));
    }

    @Test
    void emptyCarousel_slot15_remainsReserved() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[14].reserved").value(true));
    }

    @Test
    void emptyCarousel_schedulePreservedAfterEmpty() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk());

        // Schedule entries should still exist — only slots are cleared
        mockMvc.perform(get("/api/mobile/getSchedule")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitamins[0].vitaminType").value("Vitamin C"));
    }

    @Test
    void emptyCarousel_thenConfirmFill_reassignsSlots() throws Exception {
        mockMvc.perform(post("/api/mobile/empty-carousel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/firmware/poll")
                        .param("deviceId", "DISPENSER_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command").value("EMPTY"));

        // Confirm fill after emptying — slots should be reassigned
        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(
                json.get("slots").get(0).get("assignedDay").isNull(),
                "Slots should be reassigned after confirm fill"
        );
    }
}
