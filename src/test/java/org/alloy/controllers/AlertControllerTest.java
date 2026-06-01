package org.alloy.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alloy.AlloyWebMvcTest;
import org.alloy.models.dto.AlertDTO;
import org.alloy.models.entities.Alert;
import org.alloy.services.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AlloyWebMvcTest(AlertController.class)
@WithMockUser(roles = "USER")
public class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    private Alert testAlert;
    private AlertDTO testAlertDto;

    @BeforeEach
    void setUp() {
        testAlert = new Alert();
        testAlert.setId(1);
        testAlert.setType("INFO");
        testAlert.setMessage("Test alert");
        testAlert.setSeverity("LOW");

        testAlertDto = new AlertDTO();
        testAlertDto.setId(1);
        testAlertDto.setType("INFO");
        testAlertDto.setMessage("Test alert");
    }

    @Test
    void getAllAlerts_ShouldReturnListOfAlerts() throws Exception {
        when(alertService.findAll()).thenReturn(Arrays.asList(testAlert));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(alertService).findAll();
    }

    @Test
    void getAlertById_WhenAlertExists_ShouldReturnAlert() throws Exception {
        when(alertService.findById(1)).thenReturn(Optional.of(testAlert));

        mockMvc.perform(get("/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(alertService).findById(1);
    }

    @Test
    void getAlertById_WhenAlertDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(alertService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/alerts/999"))
                .andExpect(status().isNotFound());

        verify(alertService).findById(999);
    }

    @Test
    void createAlert_ShouldReturnCreatedAlert() throws Exception {
        when(alertService.createAlert(any(Alert.class))).thenReturn(testAlert);

        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAlertDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(alertService).createAlert(any(Alert.class));
    }

    @Test
    void updateAlert_WhenAlertExists_ShouldReturnUpdatedAlert() throws Exception {
        when(alertService.updateAlert(any(Alert.class))).thenReturn(testAlert);

        mockMvc.perform(put("/alerts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAlertDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(alertService).updateAlert(any(Alert.class));
    }

    @Test
    void updateAlert_WhenAlertDoesNotExist_ShouldStillUpdate() throws Exception {
        when(alertService.updateAlert(any(Alert.class))).thenReturn(testAlert);

        mockMvc.perform(put("/alerts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testAlertDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(alertService).updateAlert(any(Alert.class));
    }

    @Test
    void deleteAlert_WhenAlertExists_ShouldReturnNoContent() throws Exception {
        when(alertService.findById(1)).thenReturn(Optional.of(testAlert));
        doNothing().when(alertService).deleteById(1);

        mockMvc.perform(delete("/alerts/1"))
                .andExpect(status().isNoContent());

        verify(alertService).findById(1);
        verify(alertService).deleteById(1);
    }

    @Test
    void deleteAlert_WhenAlertDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(alertService.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/alerts/999"))
                .andExpect(status().isNotFound());

        verify(alertService).findById(999);
        verify(alertService, never()).deleteById(anyInt());
    }
}
