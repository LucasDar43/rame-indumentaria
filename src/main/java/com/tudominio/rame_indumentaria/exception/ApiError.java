package com.tudominio.rame_indumentaria.exception;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private int status;
    private String mensaje;
    private LocalDateTime timestamp;
    private Map<String, String> errores;
}
