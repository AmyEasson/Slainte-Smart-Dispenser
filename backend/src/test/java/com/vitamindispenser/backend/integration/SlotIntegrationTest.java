package com.vitamindispenser.backend.integration;

import com.vitamindispenser.backend.device.DeviceRepository;
import com.vitamindispenser.backend.schedule.ScheduleEntryRepository;
import com.vitamindispenser.backend.schedule.SlotRepository;
import com.vitamindispenser.backend.user.UserRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SlotIntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        slotRepository.deleteAll();
        scheduleEntryRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();

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
    }

    // helper to post a schedule
    private void postSchedule(String body) throws Exception {
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void postSchedule_creates15Slots_withSlot15Reserved() throws Exception {
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin C",
                  "numberOfPills": 1,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(15))
                .andExpect(jsonPath("$.slots[14].slotNumber").value(15))
                .andExpect(jsonPath("$.slots[14].reserved").value(true));
    }

    @Test
    void postSchedule_vitaminsAtSameTime_groupedIntoSameSlot() throws Exception {
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin A",
                  "numberOfPills": 2,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                },
                {
                  "vitaminType": "Magnesium",
                  "numberOfPills": 1,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].assignedDay").value("monday"))
                .andExpect(jsonPath("$.slots[0].assignedTime").value("09:00"))
                .andExpect(jsonPath("$.slots[0].vitamins.length()").value(2));
    }

    @Test
    void postSchedule_weeklyPatternRepeatsAcrossAll14Slots() throws Exception {
        // 3 unique times per week — should repeat across all 14 slots
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin A",
                  "numberOfPills": 1,
                  "schedule": [
                    {"day": "monday", "times": ["09:00", "18:00"]},
                    {"day": "tuesday", "times": ["09:00"]}
                  ]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // all 14 non-reserved slots should be assigned
                .andExpect(jsonPath("$.slots[0].assignedDay").value("monday"))
                .andExpect(jsonPath("$.slots[0].assignedTime").value("09:00"))
                .andExpect(jsonPath("$.slots[3].assignedDay").value("monday"))
                .andExpect(jsonPath("$.slots[3].assignedTime").value("09:00"))
                // slot 4 (index 3) should repeat slot 1 (index 0)
                .andExpect(jsonPath("$.slots[13].assignedDay").isNotEmpty())
                // slot 15 still reserved
                .andExpect(jsonPath("$.slots[14].reserved").value(true));
    }

    @Test
    void postSchedule_tooManySlots_returns400() throws Exception {
        // 15 unique times — should exceed the 14 slot limit
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "vitamins": [
                            {
                              "vitaminType": "Vitamin A",
                              "numberOfPills": 1,
                              "schedule": [
                                {"day": "monday", "times": [
                                  "06:00","07:00","08:00","09:00","10:00",
                                  "11:00","12:00","13:00","14:00","15:00",
                                  "16:00","17:00","18:00","19:00","20:00"
                                ]}
                              ]
                            }
                          ]
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refillInfo_correctDaysCalculated() throws Exception {
        // 3 unique times → floor(14/3) * 7 = 28 days
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin A",
                  "numberOfPills": 1,
                  "schedule": [
                    {"day": "monday", "times": ["09:00", "18:00"]},
                    {"day": "tuesday", "times": ["09:00"]}
                  ]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daysUntilRefill").value(28))
                .andExpect(jsonPath("$.warning").isEmpty());
    }

    @Test
    void refillInfo_emptySchedule_returnsNoScheduleWarning() throws Exception {
        // don't post any schedule — just check refill info
        mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning").value("NO_SCHEDULE"));
    }

    @Test
    void refillInfo_scheduleChangedMidCycle_returnsWarning() throws Exception {
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin A",
                  "numberOfPills": 1,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                }
              ]
            }
            """);

        // confirm fill to set lastFillDate
        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // now change the schedule
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Magnesium",
                  "numberOfPills": 1,
                  "schedule": [{"day": "tuesday", "times": ["18:00"]}]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning").value("SCHEDULE_CHANGED"));
    }

    @Test
    void confirmFill_setsLastFillDateToToday() throws Exception {
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin C",
                  "numberOfPills": 1,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                }
              ]
            }
            """);

        String today = LocalDate.now().toString();

        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastFillDate").value(today));
    }

    @Test
    void postSchedule_overwritesPreviousSlots() throws Exception {
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Vitamin A",
                  "numberOfPills": 1,
                  "schedule": [{"day": "monday", "times": ["09:00"]}]
                }
              ]
            }
            """);

        // overwrite with completely different schedule
        postSchedule("""
            {
              "vitamins": [
                {
                  "vitaminType": "Magnesium",
                  "numberOfPills": 2,
                  "schedule": [{"day": "friday", "times": ["21:00"]}]
                }
              ]
            }
            """);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].assignedDay").value("friday"))
                .andExpect(jsonPath("$.slots[0].assignedTime").value("21:00"))
                .andExpect(jsonPath("$.slots[0].vitamins[0].vitaminType").value("Magnesium"));
    }
}
