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

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    private void postSchedule(String body) throws Exception {
        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private void confirmFill() throws Exception {
        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode getSlotsJson() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static final String SCHEDULE_1_TIME = """
        {
          "vitamins":[
            {
              "vitaminType":"Vitamin C",
              "numberOfPills":1,
              "schedule":[
                {"day":"monday","times":["09:00"]}
              ]
            }
          ]
        }
        """;

    private static final String SCHEDULE_3_TIMES = """
    {
      "vitamins":[
        {
          "vitaminType":"Vitamin A",
          "numberOfPills":1,
          "schedule":[
            {"day":"monday","times":["09:00"]},
            {"day":"wednesday","times":["13:00"]},
            {"day":"friday","times":["18:00"]}
          ]
        }
      ]
    }
    """;

    private static final String SCHEDULE_14_TIMES = """
        {
          "vitamins":[
            {
              "vitaminType":"Vitamin A",
              "numberOfPills":1,
              "schedule":[
                {"day":"monday","times":[
                  "06:00","07:00","08:00","09:00","10:00","11:00","12:00",
                  "13:00","14:00","15:00","16:00","17:00","18:00","19:00"
                ]}
              ]
            }
          ]
        }
        """;

    private static final String SCHEDULE_15_TIMES = """
        {
          "vitamins":[
            {
              "vitaminType":"Vitamin A",
              "numberOfPills":1,
              "schedule":[
                {"day":"monday","times":[
                  "05:00","06:00","07:00","08:00","09:00","10:00","11:00","12:00",
                  "13:00","14:00","15:00","16:00","17:00","18:00","19:00"
                ]}
              ]
            }
          ]
        }
        """;

    @Test
    void postSchedule_creates15Slots_withReservedSlot15() throws Exception {

        postSchedule(SCHEDULE_1_TIME);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(15))
                .andExpect(jsonPath("$.slots[14].reserved").value(true));
    }

    @Test
    void postSchedule_all14NonReservedSlotsAssigned() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);

        JsonNode json = getSlotsJson();

        assertNotNull(json.get("slots").get(0).get("assignedDay"));
        assertNotNull(json.get("slots").get(13).get("assignedDay"));
        assertTrue(json.get("slots").get(14).get("reserved").asBoolean());
    }

    @Test
    void vitaminsAtSameTime_groupedIntoSameSlot() throws Exception {

        postSchedule("""
            {
              "vitamins":[
                {
                  "vitaminType":"Vitamin A",
                  "numberOfPills":2,
                  "schedule":[{"day":"monday","times":["09:00"]}]
                },
                {
                  "vitaminType":"Magnesium",
                  "numberOfPills":1,
                  "schedule":[{"day":"monday","times":["09:00"]}]
                }
              ]
            }
            """);

        JsonNode json = getSlotsJson();

        boolean found = false;

        for (JsonNode slot : json.get("slots")) {

            if (slot.has("assignedDay")
                    && slot.get("assignedDay").asText().equals("monday")
                    && slot.get("assignedTime").asText().equals("09:00")) {

                assertEquals(2, slot.get("vitamins").size());
                found = true;
            }
        }

        assertTrue(found);
    }

    @Test
    void schedulePatternRepeatsCorrectly() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);

        JsonNode json = getSlotsJson();

        String day0 = json.get("slots").get(0).get("assignedDay").asText();
        String time0 = json.get("slots").get(0).get("assignedTime").asText();

        String day3 = json.get("slots").get(3).get("assignedDay").asText();
        String time3 = json.get("slots").get(3).get("assignedTime").asText();

        assertEquals(day0, day3);
        assertEquals(time0, time3);
    }

    @Test
    void scheduleExactly14Times_fillsAllSlots() throws Exception {

        postSchedule(SCHEDULE_14_TIMES);

        JsonNode json = getSlotsJson();

        assertNotNull(json.get("slots").get(0).get("assignedDay"));
        assertNotNull(json.get("slots").get(13).get("assignedDay"));
        assertTrue(json.get("slots").get(14).get("reserved").asBoolean());
    }

    @Test
    void scheduleMoreThan14Times_isAccepted() throws Exception {

        mockMvc.perform(post("/api/mobile/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SCHEDULE_15_TIMES))
                .andExpect(status().isOk());
    }

    @Test
    void refillInfo_emptySchedule_returnsWarning() throws Exception {

        mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning").value("NO_SCHEDULE"));
    }

    @Test
    void refillInfo_hasSchedule_returnsRefillData() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);

        mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refillDate").isNotEmpty())
                .andExpect(jsonPath("$.daysUntilRefill").isNumber());
    }

    @Test
    void refillDate_isInFuture() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);

        MvcResult result = mockMvc.perform(get("/api/mobile/slots/refill-info")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        LocalDate refillDate = LocalDate.parse(json.get("refillDate").asText());

        assertTrue(refillDate.isAfter(LocalDate.now()));
    }

    @Test
    void confirmFill_setsLastFillDate() throws Exception {

        postSchedule(SCHEDULE_1_TIME);

        String today = LocalDate.now().toString();

        mockMvc.perform(post("/api/mobile/slots/confirm-fill")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastFillDate").value(today));
    }

    @Test
    void confirmFill_changesSlotOffset_forLargeSchedule() throws Exception {

        postSchedule(SCHEDULE_15_TIMES);

        JsonNode before = getSlotsJson();
        String beforeTime = before.get("slots").get(0).get("assignedTime").asText();

        confirmFill();

        JsonNode after = getSlotsJson();
        String afterTime = after.get("slots").get(0).get("assignedTime").asText();

        assertNotEquals(beforeTime, afterTime);
    }

    @Test
    void confirmFill_exactly14Times_wrapsOffset() throws Exception {

        postSchedule(SCHEDULE_14_TIMES);

        JsonNode before = getSlotsJson();
        confirmFill();
        JsonNode after = getSlotsJson();

        assertEquals(
                before.get("slots").get(0).get("assignedDay").asText(),
                after.get("slots").get(0).get("assignedDay").asText()
        );
    }

    @Test
    void slotCount_neverExceeds15() throws Exception {

        postSchedule(SCHEDULE_15_TIMES);

        mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(15));
    }

    @Test
    void slot15_isAlwaysReserved() throws Exception {
        // Post any schedule (here using SCHEDULE_1_TIME)
        postSchedule(SCHEDULE_1_TIME);

        // Fetch all slots
        MvcResult result = mockMvc.perform(get("/api/mobile/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        com.jayway.jsonpath.DocumentContext json = com.jayway.jsonpath.JsonPath.parse(body);

        // Slot 15 (index 14) should always be reserved
        boolean reserved = json.read("$.slots[14].reserved", Boolean.class);
        assertTrue(reserved, "Slot 15 should always be reserved");
        
        assertNull(json.read("$.slots[14].assignedDay", String.class), "Slot 15 assignedDay should be null");
        assertNull(json.read("$.slots[14].assignedTime", String.class), "Slot 15 assignedTime should be null");
    }

    @Test
    void identicalSchedule_producesSameSlotLayout() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);
        JsonNode first = getSlotsJson();

        postSchedule(SCHEDULE_3_TIMES);
        JsonNode second = getSlotsJson();

        assertEquals(first.get("slots").toString(), second.get("slots").toString());
    }

    @Test
    void repeatingPattern_keepsCorrectOrder() throws Exception {

        postSchedule(SCHEDULE_3_TIMES);

        JsonNode json = getSlotsJson();

        String t0 = json.get("slots").get(0).get("assignedTime").asText();
        String t1 = json.get("slots").get(1).get("assignedTime").asText();
        String t2 = json.get("slots").get(2).get("assignedTime").asText();

        String t3 = json.get("slots").get(3).get("assignedTime").asText();

        assertEquals(t0, t3);
        assertNotEquals(t0, t1);
        assertNotEquals(t1, t2);
    }

    @Test
    void largeSchedule_assignsAll14NonReservedSlots() throws Exception {

        postSchedule(SCHEDULE_15_TIMES);

        JsonNode json = getSlotsJson();

        for (int i = 0; i < 14; i++) {

            assertTrue(json.get("slots").get(i).has("assignedDay"));
            assertTrue(json.get("slots").get(i).has("assignedTime"));
        }
    }

    @Test
    void groupedVitamins_repeatTogetherAcrossSlots() throws Exception {

        postSchedule("""
        {
          "vitamins":[
            {
              "vitaminType":"Vitamin A",
              "numberOfPills":1,
              "schedule":[{"day":"monday","times":["09:00"]}]
            },
            {
              "vitaminType":"Vitamin D",
              "numberOfPills":1,
              "schedule":[{"day":"monday","times":["09:00"]}]
            }
          ]
        }
        """);

        JsonNode json = getSlotsJson();

        for (int i = 0; i < 14; i++) {

            JsonNode slot = json.get("slots").get(i);

            if (slot.has("vitamins")) {
                assertEquals(2, slot.get("vitamins").size());
            }
        }
    }
}
