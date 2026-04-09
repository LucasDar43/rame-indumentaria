package com.tudominio.rame_indumentaria.service;

import com.tudominio.rame_indumentaria.dto.FilaErrorDTO;
import com.tudominio.rame_indumentaria.dto.ImportacionResultadoDTO;
import com.tudominio.rame_indumentaria.model.Producto;
import com.tudominio.rame_indumentaria.model.Variante;
import com.tudominio.rame_indumentaria.repository.ProductoRepository;
import com.tudominio.rame_indumentaria.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportacionService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ObjectProvider<ImportacionService> selfProvider;

    public ImportacionResultadoDTO importarProductos(MultipartFile archivo) throws IOException {

        List<FilaErrorDTO> errores = new ArrayList<>();
        int productosCreados = 0;
        int variantesCreadas = 0;

        LinkedHashMap<String, List<Row>> grupos = new LinkedHashMap<>();
        Map<Integer, FilaImportacionData> filasValidas = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row fila = sheet.getRow(i);

                if (fila == null) {
                    continue;
                }

                try {
                    FilaImportacionData datos = validarFila(fila);
                    filasValidas.put(i, datos);

                    String claveGrupo = construirClaveGrupo(datos.nombre(), datos.marca());
                    grupos.computeIfAbsent(claveGrupo, key -> new ArrayList<>()).add(fila);
                } catch (Exception e) {
                    errores.add(FilaErrorDTO.builder()
                            .fila(i + 1)
                            .mensaje(obtenerMensajeError(e))
                            .build());
                }
            }

            for (List<Row> filasGrupo : grupos.values()) {
                try {
                    int variantesDelGrupo = selfProvider.getObject().persistirGrupo(filasGrupo, filasValidas);
                    productosCreados++;
                    variantesCreadas += variantesDelGrupo;
                } catch (Exception e) {
                    String mensaje = obtenerMensajeError(e);
                    for (Row fila : filasGrupo) {
                        errores.add(FilaErrorDTO.builder()
                                .fila(fila.getRowNum() + 1)
                                .mensaje(mensaje)
                                .build());
                    }
                }
            }
        }

        return ImportacionResultadoDTO.builder()
                .productosCreados(productosCreados)
                .variantesCreadas(variantesCreadas)
                .errores(errores)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int persistirGrupo(List<Row> filasGrupo, Map<Integer, FilaImportacionData> filasValidas) {
        Row primeraFila = filasGrupo.get(0);
        FilaImportacionData datosProducto = filasValidas.get(primeraFila.getRowNum());

        Producto producto = Producto.builder()
                .nombre(datosProducto.nombre())
                .descripcion(datosProducto.descripcion())
                .precio(datosProducto.precio())
                .marca(datosProducto.marca())
                .categoria(datosProducto.categoria())
                .subcategoria(datosProducto.subcategoria())
                .imagenUrl(null)
                .activo(true)
                .build();

        Producto productoGuardado = productoRepository.save(producto);

        int variantesCreadas = 0;

        for (Row fila : filasGrupo) {
            FilaImportacionData datosVariante = filasValidas.get(fila.getRowNum());

            Variante variante = Variante.builder()
                    .producto(productoGuardado)
                    .talle(datosVariante.talle())
                    .color(datosVariante.color())
                    .stock(datosVariante.stock())
                    .sku(datosVariante.sku())
                    .activo(true)
                    .build();

            varianteRepository.save(variante);
            variantesCreadas++;
        }

        return variantesCreadas;
    }

    private FilaImportacionData validarFila(Row fila) {
        String nombre = obtenerTexto(fila, 0);
        String descripcion = obtenerTexto(fila, 1);
        String precioRaw = obtenerTexto(fila, 2);
        String marca = obtenerTexto(fila, 3);
        String categoria = obtenerTexto(fila, 4);
        String subcategoria = obtenerTexto(fila, 5);
        String talle = obtenerTexto(fila, 6);
        String color = obtenerTexto(fila, 7);
        String stockRaw = obtenerTexto(fila, 8);
        String sku = obtenerTexto(fila, 9);

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (marca == null || marca.isBlank()) {
            throw new IllegalArgumentException("La marca es obligatoria");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }
        if (talle == null || talle.isBlank()) {
            throw new IllegalArgumentException("El talle es obligatorio");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("El color es obligatorio");
        }

        Double precio = parsearPrecio(precioRaw);
        Integer stock = parsearStock(stockRaw);

        return new FilaImportacionData(
                nombre,
                descripcion,
                precio,
                marca,
                categoria,
                subcategoria,
                talle,
                color,
                stock,
                sku
        );
    }

    private Double parsearPrecio(String precioRaw) {
        if (precioRaw == null || precioRaw.isBlank()) {
            throw new IllegalArgumentException("El precio es obligatorio");
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

        return precio;
    }

    private Integer parsearStock(String stockRaw) {
        if (stockRaw == null || stockRaw.isBlank()) {
            throw new IllegalArgumentException("El stock es obligatorio");
        }

        int stock;
        try {
            stock = Integer.parseInt(stockRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El stock no es un numero valido: " + stockRaw);
        }

        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        return stock;
    }

    private String construirClaveGrupo(String nombre, String marca) {
        return nombre.trim().toLowerCase() + "|" + marca.trim().toLowerCase();
    }

    private String obtenerMensajeError(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Error al importar el grupo";
    }

    private record FilaImportacionData(
            String nombre,
            String descripcion,
            Double precio,
            String marca,
            String categoria,
            String subcategoria,
            String talle,
            String color,
            Integer stock,
            String sku
    ) {
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
