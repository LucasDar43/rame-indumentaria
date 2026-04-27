package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.FilaErrorDTO;
import com.tudominio.rame_indumentaria.dto.ImportacionResultadoDTO;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.model.Variante;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportacionSimpleService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;

    public ImportacionResultadoDTO importarSimple(MultipartFile archivo) throws IOException {
        List<FilaErrorDTO> errores = new ArrayList<>();
        int importados = 0;
        int variantesCreadas = 0;

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row encabezado = sheet.getRow(0);

            int idxCodigo = -1;
            int idxDescripcion = -1;
            int idxPrecioVenta = -1;
            int idxInventario = -1;

            if (encabezado != null) {
                for (int i = 0; i < encabezado.getLastCellNum(); i++) {
                    String header = obtenerTexto(encabezado, i);
                    if (header == null) {
                        continue;
                    }

                    String normalizado = header.trim().toLowerCase();
                    switch (normalizado) {
                        case "codigo" -> idxCodigo = i;
                        case "descripcion" -> idxDescripcion = i;
                        case "precio venta" -> idxPrecioVenta = i;
                        case "inventario" -> idxInventario = i;
                        default -> {
                        }
                    }
                }
            }

            if (idxDescripcion < 0) {
                throw new IllegalArgumentException("Formato de Excel no reconocido");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row fila = sheet.getRow(i);

                if (fila == null) {
                    continue;
                }

                String descripcion = obtenerTexto(fila, idxDescripcion);
                String precioRaw = idxPrecioVenta >= 0 ? obtenerTexto(fila, idxPrecioVenta) : null;
                String codigoRaw = idxCodigo >= 0 ? obtenerTexto(fila, idxCodigo) : null;
                String inventarioRaw = idxInventario >= 0 ? obtenerTexto(fila, idxInventario) : null;

                if (esFilaVacia(descripcion, precioRaw, codigoRaw, inventarioRaw)) {
                    continue;
                }

                if (descripcion == null || descripcion.isBlank()) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje("Descripcion vacía")
                            .build());
                    continue;
                }

                if (precioRaw == null || precioRaw.isBlank()) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje("Precio vacío")
                            .build());
                    continue;
                }

                BigDecimal precio;
                try {
                    String precioNormalizado = normalizarPrecio(precioRaw);
                    precio = new BigDecimal(precioNormalizado);
                } catch (Exception e) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje("Precio no válido: " + precioRaw)
                            .build());
                    continue;
                }

                if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje("El precio debe ser mayor a 0")
                            .build());
                    continue;
                }

                int stock = 0;
                if (inventarioRaw != null && !inventarioRaw.isBlank()) {
                    try {
                        double inventario = Double.parseDouble(inventarioRaw.replace(",", ".").trim());
                        if (inventario >= 0) {
                            stock = (int) Math.floor(inventario);
                        }
                    } catch (Exception e) {
                        stock = 0;
                    }
                }

                try {
                    Producto producto = Producto.builder()
                            .nombre(descripcion.trim())
                            .precio(precio)
                            .marca("Sin marca")
                            .categoria("General")
                            .activo(true)
                            .build();

                    Producto guardado = productoRepository.save(producto);

                    Variante variante = Variante.builder()
                            .producto(guardado)
                            .talle("ÚNICO")
                            .color("ÚNICO")
                            .stock(stock)
                            .sku(codigoRaw != null ? codigoRaw.trim() : null)
                            .activo(true)
                            .build();

                    varianteRepository.save(variante);

                    importados++;
                    variantesCreadas++;
                } catch (Exception e) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje(e.getMessage())
                            .build());
                }
            }
        }

        return ImportacionResultadoDTO.builder()
                .productosCreados(importados)
                .variantesCreadas(variantesCreadas)
                .errores(errores)
                .build();
    }

    private boolean esFilaVacia(String descripcion, String precioRaw, String codigoRaw, String inventarioRaw) {
        return esVacio(descripcion) && esVacio(precioRaw) && esVacio(codigoRaw) && esVacio(inventarioRaw);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String normalizarPrecio(String precioRaw) {
        String valor = precioRaw.trim().replace("$", "").replace(" ", "");

        if (valor.contains(",") && valor.contains(".")) {
            valor = valor.replace(".", "").replace(",", ".");
        } else {
            valor = valor.replace(",", ".");
        }

        return valor;
    }

    private String obtenerTexto(Row fila, int columna) {
        Cell celda = fila.getCell(columna);
        if (celda == null) return null;

        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> {
                double val = celda.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case BLANK -> null;
            default -> null;
        };
    }
}
