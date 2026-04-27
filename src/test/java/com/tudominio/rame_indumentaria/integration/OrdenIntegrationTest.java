package com.tudominio.rame_indumentaria.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.tudominio.rame_indumentaria.model.Orden;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.repository.OrdenRepository;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private com.tudominio.rame_indumentaria.repository.VarianteRepository varianteRepository;

    @BeforeEach
    void setUp() {
        varianteRepository.deleteAll();
        ordenRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    @Transactional
    void crearOrdenPersisteLaOrdenYDevuelveDatosBasicos() throws Exception {
        Producto producto = productoRepository.save(Producto.builder()
                .nombre("Remera Dry Fit")
                .descripcion("Remera deportiva")
                .precio(BigDecimal.valueOf(15999.0))
                .marca("Rame")
                .categoria("Remeras")
                .imagenUrl("https://cdn.test/remera.jpg")
                .build());

        com.tudominio.rame_indumentaria.model.Variante variante = varianteRepository.save(
                com.tudominio.rame_indumentaria.model.Variante.builder()
                        .producto(producto)
                        .talle("M")
                        .color("Negro")
                        .stock(10)
                        .activo(true)
                        .build()
        );

        Preference preference = mock(Preference.class);
        when(preference.getId()).thenReturn("pref-integration");
        when(preference.getInitPoint()).thenReturn("https://sandbox.mercadopago.com/test-checkout");

        Map<String, Object> payload = Map.of(
                "nombreComprador", "Lucia Test",
                "emailComprador", "lucia@test.com",
                "telefonoComprador", "1122334455",
                "direccionEnvio", "Calle 123",
                "items", new Object[]{
                        Map.of(
                                "productoId", producto.getId(),
                                "varianteId", variante.getId(),
                                "cantidad", 2
                        )
                }
        );

        try (MockedConstruction<PreferenceClient> mocked = org.mockito.Mockito.mockConstruction(
                PreferenceClient.class,
                (clientMock, context) -> when(clientMock.create(any()))
                        .thenReturn(preference)
        )) {
            mockMvc.perform(post("/api/ordenes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.initPoint").value("https://sandbox.mercadopago.com/test-checkout"))
                    .andExpect(jsonPath("$.emailComprador").value("lucia@test.com"))
                    .andExpect(jsonPath("$.items[0].productoId").value(producto.getId()))
                    .andExpect(jsonPath("$.items[0].cantidad").value(2));

            assertThat(mocked.constructed()).hasSize(1);
        }

        assertThat(ordenRepository.findAll()).hasSize(1);

        Orden orden = ordenRepository.findAll().get(0);
        assertThat(orden.getId()).isNotNull();
        assertThat(orden.getNombreComprador()).isEqualTo("Lucia Test");
        assertThat(orden.getEmailComprador()).isEqualTo("lucia@test.com");
        assertThat(orden.getMpPreferenceId()).isEqualTo("pref-integration");
        assertThat(orden.getItems()).hasSize(1);
        assertThat(orden.getItems().get(0).getProductoId()).isEqualTo(producto.getId());
        assertThat(orden.getItems().get(0).getCantidad()).isEqualTo(2);
        assertThat(orden.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(34998.0));
    }
}
