package com.sherlockhomes.web.util;

import java.util.Map;

/** Utilidades para leer los datos JSON (Map) que devuelve el backend. */
public final class Valores {

    private Valores() {
    }

    /** Lee una ruta con puntos, por ejemplo "zona.letra". Devuelve null si algo falta. */
    public static Object leer(Map<String, Object> mapa, String ruta) {
        Object actual = mapa;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> m)) {
                return null;
            }
            actual = m.get(parte);
        }
        return actual;
    }

    /** Texto crudo ("" si es null). Sirve para armar URLs y valores de formulario. */
    public static String s(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Texto para mostrar en una celda de tabla. */
    public static String celda(Object o) {
        if (o == null) {
            return "—";
        }
        if (o instanceof Boolean b) {
            return b ? "Sí" : "No";
        }
        return String.valueOf(o);
    }
}
