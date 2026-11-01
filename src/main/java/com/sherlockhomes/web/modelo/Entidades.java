package com.sherlockhomes.web.modelo;

import static com.sherlockhomes.web.util.Valores.leer;
import static com.sherlockhomes.web.util.Valores.s;

import com.sherlockhomes.web.api.ApiClient;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;

/** Catalogo de las entidades que administra el ADMINISTRADOR, con sus columnas, formularios y acciones. */
public final class Entidades {

    private static final Map<String, Entidad> TODAS = new LinkedHashMap<>();

    public static Entidad buscar(String id) {
        return TODAS.get(id);
    }

    public static Collection<Entidad> todas() {
        return TODAS.values();
    }

    // ---- etiquetas para los desplegables ----
    private static final Function<Map<String, Object>, String> L_ZONA = m -> s(m.get("letra")) + " · " + s(m.get("tipoVehiculos"));
    private static final Function<Map<String, Object>, String> L_PERSONA = m -> s(m.get("nombre")) + " (" + s(m.get("dni")) + ")";
    private static final Function<Map<String, Object>, String> L_VEHICULO = m -> s(m.get("patente")) + " · " + s(m.get("marca"));
    private static final Function<Map<String, Object>, String> L_GARAGE = m -> "N° " + s(m.get("numero")) + " · zona " + s(leer(m, "zona.letra"));

    // ---- columnas compartidas (tambien las usan las pantallas de empleado y socio) ----
    public static final List<Columna> C_VEHICULOS = List.of(col("patente", "Patente"), col("marca", "Marca"), col("tipo", "Tipo"),
            col("dimensiones", "Dimensiones"), col("propietario.nombre", "Propietario"), col("garageAsignado.numero", "Garage"));
    public static final List<Columna> C_GARAGES = List.of(col("numero", "N°"), col("zona.letra", "Zona"), col("socio.nombre", "Socio"),
            col("lecturaContadorLuz", "Contador de luz"), col("mantenimientoContratado", "Mantenimiento"), col("fechaCompra", "Compra"));
    public static final List<Columna> C_ZONAS = List.of(col("letra", "Letra"), col("tipoVehiculos", "Vehículos"),
            col("profundidadGarage", "Profundidad"), col("anchoGarage", "Ancho"));
    private static final List<Columna> C_EMPLEADOS_ZONA = List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("especialidad", "Especialidad"));

    private static Columna col(String ruta, String titulo) {
        return new Columna(ruta, titulo);
    }

    // ---- opciones de desplegables que consultan al backend ----
    private static List<Opcion> zonas(ApiClient api) {
        return Opcion.listar(api, "admin/zonas/listar", "letra", L_ZONA);
    }

    private static List<Opcion> socios(ApiClient api) {
        return Opcion.listar(api, "admin/socios/listar", "dni", L_PERSONA);
    }

    // ---- campos compartidos ----
    private static List<Campo> persona() {
        return List.of(Campo.texto("nombre", "Nombre").req(), Campo.numero("dni", "DNI").req().fijo(),
                Campo.texto("direccion", "Dirección"), Campo.texto("telefono", "Teléfono"));
    }

    private static List<Campo> acceso() {
        return List.of(Campo.texto("username", "Usuario").req().soloCrearA().desde("usuario.username"),
                Campo.clave("password", "Contraseña (mínimo 6 caracteres)").req());
    }

    private static List<Campo> unir(List<Campo> a, List<Campo> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    private static void registrar(Entidad e) {
        TODAS.put(e.id(), e);
    }

    static {
        registrar(new Entidad("socios", "Socios", "admin/socios", "dni", "el socio", "Nuevo socio",
                List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("telefono", "Teléfono"),
                        col("usuario.username", "Usuario"), col("fechaIngreso", "Ingreso")),
                unir(persona(), acceso()),
                List.of(
                        Accion.listar("vehiculos", "Vehículos", f -> "admin/socios/listarVehiculos/" + s(f.get("dni")), C_VEHICULOS),
                        Accion.listar("garages", "Garages", f -> "admin/socios/listarGarages/" + s(f.get("dni")), C_GARAGES))));

        registrar(new Entidad("empleados", "Empleados", "admin/empleados", "dni", "el empleado", "Nuevo empleado",
                List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("codigo", "Código"), col("especialidad", "Especialidad"),
                        col("telefono", "Teléfono"), col("usuario.username", "Usuario")),
                unir(unir(persona(), List.of(Campo.numero("codigo", "Código").req(), Campo.texto("especialidad", "Especialidad").req())), acceso()),
                List.of(
                        Accion.seleccionar("asignar-zona", "Asignar zona", HttpMethod.POST, f -> "admin/zonas/listar", "letra", L_ZONA,
                                (f, v) -> "admin/empleados/asignarZona/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("quitar-zona", "Quitar zona", HttpMethod.DELETE, f -> "admin/empleados/listarZonas/" + s(f.get("dni")), "letra", L_ZONA,
                                (f, v) -> "admin/empleados/quitarZona/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("asignar-vehiculo", "Asignar vehículo", HttpMethod.POST, f -> "admin/vehiculos/listar", "patente", L_VEHICULO,
                                (f, v) -> "admin/empleados/asignarVehiculo/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("quitar-vehiculo", "Quitar vehículo", HttpMethod.DELETE, f -> "admin/empleados/listarVehiculos/" + s(f.get("dni")), "patente", L_VEHICULO,
                                (f, v) -> "admin/empleados/quitarVehiculo/" + s(f.get("dni")) + "/" + v),
                        Accion.listar("zonas", "Zonas", f -> "admin/empleados/listarZonas/" + s(f.get("dni")), C_ZONAS),
                        Accion.listar("vehiculos", "Vehículos", f -> "admin/empleados/listarVehiculos/" + s(f.get("dni")), C_VEHICULOS))));

        registrar(new Entidad("zonas", "Zonas", "admin/zonas", "letra", "la zona", "Nueva zona", C_ZONAS,
                List.of(Campo.texto("letra", "Letra").req().fijo(), Campo.texto("tipoVehiculos", "Tipo de vehículos").req(),
                        Campo.numero("profundidadGarage", "Profundidad del garage").req(), Campo.numero("anchoGarage", "Ancho del garage").req()),
                List.of(
                        Accion.seleccionar("asignar-empleado", "Asignar empleado", HttpMethod.POST, f -> "admin/empleados/listar", "dni", L_PERSONA,
                                (f, v) -> "admin/zonas/asignarEmpleado/" + s(f.get("letra")) + "/" + v),
                        Accion.seleccionar("quitar-empleado", "Quitar empleado", HttpMethod.DELETE, f -> "admin/zonas/listarEmpleados/" + s(f.get("letra")), "dni", L_PERSONA,
                                (f, v) -> "admin/zonas/quitarEmpleado/" + s(f.get("letra")) + "/" + v),
                        Accion.listar("empleados", "Empleados", f -> "admin/zonas/listarEmpleados/" + s(f.get("letra")), C_EMPLEADOS_ZONA),
                        Accion.listar("garages", "Garages", f -> "admin/zonas/listarGarages/" + s(f.get("letra")), C_GARAGES))));

        registrar(new Entidad("garages", "Garages", "admin/garages", "numero", "el garage", "Nuevo garage", C_GARAGES,
                List.of(Campo.numero("numero", "Número").req().fijo(),
                        Campo.select("letraZona", "Zona", Entidades::zonas).req().desde("zona.letra"),
                        Campo.selectNumero("dniSocio", "Socio propietario", Entidades::socios).desde("socio.dni").conVacio("Sin propietario"),
                        Campo.decimal("lecturaContadorLuz", "Lectura del contador de luz"),
                        Campo.check("mantenimientoContratado", "Mantenimiento contratado")),
                List.of(
                        Accion.seleccionar("asignar-socio", "Asignar socio", HttpMethod.PUT, f -> "admin/socios/listar", "dni", L_PERSONA,
                                (f, v) -> "admin/garages/asignarSocio/" + s(f.get("numero")) + "/" + v),
                        Accion.confirmar("quitar-socio", "Quitar socio", HttpMethod.DELETE, f -> "admin/garages/quitarSocio/" + s(f.get("numero"))),
                        Accion.seleccionar("asignar-vehiculo", "Asignar vehículo", HttpMethod.POST, f -> "admin/vehiculos/listar", "patente", L_VEHICULO,
                                (f, v) -> "admin/garages/asignarVehiculo/" + s(f.get("numero")) + "/" + v),
                        Accion.confirmar("quitar-vehiculo", "Quitar vehículo", HttpMethod.DELETE, f -> "admin/garages/quitarVehiculo/" + s(f.get("numero"))))));

        registrar(new Entidad("vehiculos", "Vehículos", "admin/vehiculos", "patente", "el vehículo", "Nuevo vehículo", C_VEHICULOS,
                List.of(Campo.texto("patente", "Patente").req().fijo(), Campo.texto("marca", "Marca").req(),
                        Campo.texto("tipo", "Tipo").req(), Campo.texto("dimensiones", "Dimensiones").req(),
                        Campo.selectNumero("dniPropietario", "Propietario", Entidades::socios).req().desde("propietario.dni")),
                List.of(
                        Accion.seleccionar("asignar-garage", "Asignar garage", HttpMethod.POST, f -> "admin/garages/listar", "numero", L_GARAGE,
                                (f, v) -> "admin/vehiculos/asignarGarage/" + s(f.get("patente")) + "/" + v),
                        Accion.confirmar("quitar-garage", "Quitar garage", HttpMethod.DELETE, f -> "admin/vehiculos/quitarGarage/" + s(f.get("patente"))))));
    }

    private Entidades() {
    }
}
