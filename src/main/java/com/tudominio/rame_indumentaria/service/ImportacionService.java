package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.FilaErrorDTO;
import com.tudominio.rame_indumentaria.dto.ImportacionResultadoDTO;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportacionService {

    private final ProductoRepository productoRepository;

    public ImportacionResultadoDTO importarProductos(MultipartFile archivo) throws IOException {

        List<FilaErrorDTO> errores = new ArrayList<>();
        int importados = 0;

        // Abrimos el archivo Excel
        Workbook workbook = new XSSFWorkbook(archivo.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // Tomamos la primera hoja

        // Iteramos desde la fila 1 (la 0 es el encabezado)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row fila = sheet.getRow(i);

            // Si la fila está completamente vacía, la saltamos
            if (fila == null) continue;

            try {
                String nombre = obtenerTexto(fila, 0);
                String descripcion = obtenerTexto(fila, 1);
                String precioRaw = obtenerTexto(fila, 2);
                String marca = obtenerTexto(fila, 3);
                String categoria = obtenerTexto(fila, 4);
                String subcategoria = obtenerTexto(fila, 5);

                // Validaciones manuales
                if (nombre == null || nombre.isBlank()) {
                    throw new IllegalArgumentException("El nombre es obligatorio");
                }
                if (precioRaw == null || precioRaw.isBlank()) {
                    throw new IllegalArgumentException("El precio es obligatorio");
                }
                if (marca == null || marca.isBlank()) {
                    throw new IllegalArgumentException("La marca es obligatoria");
                }
                if (categoria == null || categoria.isBlank()) {
                    throw new IllegalArgumentException("La categoria es obligatoria");
                }

                double precio;
                try {
                    precio = Double.parseDouble(precioRaw);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El precio no es un numero valido: " + precioRaw);
                }

                if (precio <= 0) {
                    throw new IllegalArgumentException("El precio debe ser mayor a 0");
                }

                // Construimos y guardamos el producto
                Producto producto = Producto.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .precio(precio)
                        .marca(marca)
                        .categoria(categoria)
                        .subcategoria(subcategoria)
                        .activo(true)
                        .build();

                productoRepository.save(producto);
                importados++;

            } catch (Exception e) {
                // Numero de fila en base 1 para que sea legible por humanos (+1 por header, +1 por base 0)
                errores.add(FilaErrorDTO.builder()
                        .fila(i + 1)
                        .mensaje(e.getMessage())
                        .build());
            }
        }

        workbook.close();

        return ImportacionResultadoDTO.builder()
                .importados(importados)
                .errores(errores)
                .build();
    }

    // Helper: obtiene el valor de una celda siempre como String,
    // independientemente de si Excel la guardó como número, texto, etc.
    private String obtenerTexto(Row fila, int columna) {
        Cell celda = fila.getCell(columna);
        if (celda == null) return null;

        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> {
                // Excel guarda los números sin decimales como 1000.0, lo limpiamos
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