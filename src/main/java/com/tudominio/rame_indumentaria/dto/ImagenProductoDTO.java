package com.tudominio.rame_indumentaria.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenProductoDTO {
    private Long id;
    private String url;
    private Integer orden;
}
