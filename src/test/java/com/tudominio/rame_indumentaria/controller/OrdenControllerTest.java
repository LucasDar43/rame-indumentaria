package com.tudominio.rame_indumentaria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tudominio.rame_indumentaria.dto.OrdenItemDTO;
import com.tudominio.rame_indumentaria.dto.OrdenRequestDTO;
import com.tudominio.rame_indumentaria.dto.OrdenResponseDTO;
import com.tudominio.rame_indumentaria.exception.GlobalExceptionHandler;
import com.tudominio.rame_indumentaria.model.EstadoOrden;
import com.tudominio.rame_indumentaria.service.OrdenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrdenControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrdenService ordenService;

    @InjectMocks
    private OrdenController ordenController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(ordenController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void crearOrdenDevuelveOrdenConInitPointCuandoElPayloadEsValido() throws Exception {
        OrdenResponseDTO response = OrdenResponseDTO.builder()
                .id(10L)
                .nombreComprador("Lucas Diaz")
                .emailComprador("lucas@test.com")
                .total(24999.0)
                .estado(EstadoOrden.PENDIENTE)
                .mpPreferenceId("pref-test")
                .initPoint("https://sandbox.mercadopago.com/init")
                .items(List.of(
                        OrdenItemDTO.builder()
                                .productoId(1L)
                                .nombreProducto("Remera")
                                .cantidad(2)
                                .precioUnitario(12499.5)
                                .subtotal(24999.0)
                                .build()
                ))
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(ordenService.crearOrden(any(OrdenRequestDTO.class))).thenReturn(response);

        String payload = """
                {
                  "nombreComprador": "Lucas Diaz",
                  "emailComprador": "lucas@test.com",
                  "items": [
                    {
                      "productoId": 1,
                      "cantidad": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.initPoint").value("https://sandbox.mercadopago.com/init"));

        ArgumentCaptor<OrdenRequestDTO> captor = ArgumentCaptor.forClass(OrdenRequestDTO.class);
        verify(ordenService).crearOrden(captor.capture());

        OrdenRequestDTO dto = captor.getValue();
        assertThat(dto).isNotNull();
        assertThat(dto.getNombreComprador()).isEqualTo("Lucas Diaz");
        assertThat(dto.getEmailComprador()).isEqualTo("lucas@test.com");
        assertThat(dto.getItems()).isNotNull().hasSize(1);
        assertThat(dto.getItems().get(0).getProductoId()).isEqualTo(1L);
        assertThat(dto.getItems().get(0).getCantidad()).isEqualTo(2);
    }

    @Test
    void crearOrdenDevuelveBadRequestCuandoFaltanNombreYEmail() throws Exception {
        String payload = """
                {
                  "nombreComprador": "",
                  "emailComprador": "",
                  "items": [
                    {
                      "productoId": 1,
                      "cantidad": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Error de validacion"))
                .andExpect(jsonPath("$.errores.nombreComprador").exists())
                .andExpect(jsonPath("$.errores.emailComprador").exists());

        verify(ordenService, never()).crearOrden(any(OrdenRequestDTO.class));
    }
}
