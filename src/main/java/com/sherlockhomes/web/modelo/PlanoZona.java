package com.sherlockhomes.web.modelo;

import java.util.List;

/** Una zona del plano con sus garages. */
public record PlanoZona(String letra, String detalle, String resumen, List<Lote> lotes) {
}
