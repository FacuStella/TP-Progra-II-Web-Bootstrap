package com.sherlockhomes.web.controller;

import com.sherlockhomes.web.api.ApiClient;
import com.sherlockhomes.web.api.ApiException;
import com.sherlockhomes.web.modelo.Accion;
import com.sherlockhomes.web.modelo.Campo;
import com.sherlockhomes.web.modelo.Entidad;
import com.sherlockhomes.web.modelo.Entidades;
import com.sherlockhomes.web.modelo.Fila;
import com.sherlockhomes.web.modelo.Lote;
import com.sherlockhomes.web.modelo.Opcion;
import com.sherlockhomes.web.modelo.PlanoZona;
import com.sherlockhomes.web.util.Valores;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantallas del ADMINISTRADOR. Un mismo controlador sirve a todas las entidades segun su descripcion en Entidades. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ApiClient api;

    public AdminController(ApiClient api) {
        this.api = api;
    }

    // ------------------------------------------------------------ plano
    @GetMapping("/plano")
    public String plano(Model m) {
        List<Map<String, Object>> zonas = api.lista("admin/zonas/listar");
        List<Map<String, Object>> garages = api.lista("admin/garages/listar");
        Set<String> conVehiculo = new HashSet<>();
        for (Map<String, Object> v : api.lista("admin/vehiculos/listar")) {
            Object g = Valores.leer(v, "garageAsignado.numero");
            if (g != null) {
                conVehiculo.add(String.valueOf(g));
            }
        }

        List<PlanoZona> plano = new ArrayList<>();
        zonas.sort(Comparator.comparing(z -> Valores.s(z.get("letra"))));
        for (Map<String, Object> z : zonas) {
            String letra = Valores.s(z.get("letra"));
            List<Lote> lotes = new ArrayList<>();
            int ocupados = 0;
            List<Map<String, Object>> deLaZona = garages.stream()
                    .filter(g -> letra.equals(Valores.s(Valores.leer(g, "zona.letra"))))
                    .sorted(Comparator.comparingInt(g -> ((Number) g.get("numero")).intValue()))
                    .toList();
            for (Map<String, Object> g : deLaZona) {
                String numero = Valores.s(g.get("numero"));
                boolean ocupado = conVehiculo.contains(numero);
                Object socio = Valores.leer(g, "socio.nombre");
                String clase = ocupado ? "lote-ocupado" : socio != null ? "lote-socio" : "";
                String titulo = "Garage " + numero + " · " + (ocupado ? "con vehículo"
                        : socio != null ? "de " + socio + ", sin vehículo" : "sin propietario");
                lotes.add(new Lote(numero, clase, titulo));
                if (ocupado) {
                    ocupados++;
                }
            }
            plano.add(new PlanoZona(letra,
                    Valores.s(z.get("tipoVehiculos")) + " · garage de " + z.get("profundidadGarage") + " × " + z.get("anchoGarage"),
                    ocupados + " de " + lotes.size() + " con vehículo", lotes));
        }
        long conVeh = garages.stream().filter(g -> conVehiculo.contains(Valores.s(g.get("numero")))).count();
        long sinDueno = garages.stream().filter(g -> g.get("socio") == null).count();
        m.addAttribute("zonas", plano);
        m.addAttribute("total", garages.size());
        m.addAttribute("conVehiculo", conVeh);
        m.addAttribute("sinPropietario", sinDueno);
        return "plano";
    }

    // ------------------------------------------------------------ listado
    @GetMapping("/{ent}")
    public String lista(@PathVariable String ent, @RequestParam(defaultValue = "") String q, Model m) {
        Entidad e = entidad(ent);
        String filtro = q.trim().toLowerCase();
        List<Fila> filas = api.lista(e.base() + "/listar").stream()
                .map(x -> Fila.de(x, e.columnas(), e.clave()))
                .filter(f -> filtro.isEmpty() || String.join(" ", f.celdas()).toLowerCase().contains(filtro))
                .toList();
        m.addAttribute("ent", e);
        m.addAttribute("filas", filas);
        m.addAttribute("q", q);
        return "lista";
    }

    // ------------------------------------------------------------ alta y edicion
    @GetMapping("/{ent}/nuevo")
    public String nuevo(@PathVariable String ent, Model m) {
        return formulario(m, entidad(ent), new HashMap<>(), false, null, null);
    }

    @PostMapping("/{ent}/guardar")
    public String guardar(@PathVariable String ent, @RequestParam Map<String, String> p, Model m, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        try {
            api.enviar(HttpMethod.POST, e.base() + "/registrar", cuerpo(e, p, false));
            ra.addFlashAttribute("ok", "Se creó " + e.singular() + ".");
            return "redirect:/admin/" + ent;
        } catch (NumberFormatException ex) {
            return formulario(m, e, p, false, null, "Hay un valor numérico inválido.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            return formulario(m, e, p, false, null, ex.getMessage());
        }
    }

    @GetMapping("/{ent}/{clave}/editar")
    public String editar(@PathVariable String ent, @PathVariable String clave, Model m) {
        Entidad e = entidad(ent);
        Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
        Map<String, String> valores = new HashMap<>();
        for (Campo c : e.campos()) {
            Object v = c.origen() != null ? Valores.leer(fila, c.origen()) : fila.get(c.nombre());
            valores.put(c.nombre(), c.tipo() == Campo.Tipo.CLAVE ? "" : Valores.s(v));
        }
        return formulario(m, e, valores, true, clave, null);
    }

    @PostMapping("/{ent}/{clave}/actualizar")
    public String actualizar(@PathVariable String ent, @PathVariable String clave, @RequestParam Map<String, String> p,
                             Model m, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        try {
            api.enviar(HttpMethod.PUT, e.base() + "/actualizar/" + clave, cuerpo(e, p, true));
            ra.addFlashAttribute("ok", "Se guardaron los cambios.");
            return "redirect:/admin/" + ent;
        } catch (NumberFormatException ex) {
            return formulario(m, e, p, true, clave, "Hay un valor numérico inválido.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            return formulario(m, e, p, true, clave, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ acciones (asignar, quitar, eliminar, listar)
    @GetMapping("/{ent}/{clave}/accion/{id}")
    public String accion(@PathVariable String ent, @PathVariable String clave, @PathVariable String id, Model m) {
        Entidad e = entidad(ent);
        Accion a = accion(e, id);
        Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
        m.addAttribute("ent", e);
        m.addAttribute("clave", clave);
        m.addAttribute("accion", a);
        if (a.tipo() == Accion.Tipo.SELECCIONAR) {
            m.addAttribute("opciones", Opcion.listar(api, a.origen().apply(fila), a.claveValor(), a.etiquetaOpcion()));
            return "seleccion";
        }
        if (a.tipo() == Accion.Tipo.CONFIRMAR) {
            return "confirmar";
        }
        m.addAttribute("titulo", a.etiqueta() + " · " + e.singular() + " " + clave);
        m.addAttribute("columnas", a.columnas());
        m.addAttribute("filas", api.lista(a.origen().apply(fila)).stream().map(x -> Fila.de(x, a.columnas(), null)).toList());
        return "resultado";
    }

    @PostMapping("/{ent}/{clave}/accion/{id}")
    public String ejecutar(@PathVariable String ent, @PathVariable String clave, @PathVariable String id,
                           @RequestParam(required = false) String valor, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        Accion a = accion(e, id);
        try {
            Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
            api.enviar(a.metodo(), a.destino().apply(fila, valor), null);
            ra.addFlashAttribute("ok", a.etiqueta() + ": listo.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/" + ent;
    }

    // ------------------------------------------------------------ auxiliares
    private Entidad entidad(String id) {
        Entidad e = Entidades.buscar(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return e;
    }

    private Accion accion(Entidad e, String id) {
        if ("eliminar".equals(id)) {
            return e.eliminar();
        }
        return e.acciones().stream().filter(a -> a.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String formulario(Model m, Entidad e, Map<String, String> valores, boolean editar, String clave, String error) {
        Map<String, List<Opcion>> opciones = new LinkedHashMap<>();
        for (Campo c : e.campos()) {
            if (c.opciones() != null) {
                opciones.put(c.nombre(), c.opciones().apply(api));
            }
        }
        m.addAttribute("ent", e);
        m.addAttribute("campos", e.campos());
        m.addAttribute("valores", valores);
        m.addAttribute("opciones", opciones);
        m.addAttribute("editar", editar);
        m.addAttribute("clave", clave);
        m.addAttribute("error", error);
        return "formulario";
    }

    /** Arma el JSON que espera el backend a partir de los datos del formulario. */
    private Map<String, Object> cuerpo(Entidad e, Map<String, String> p, boolean editando) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Campo c : e.campos()) {
            if (editando && c.soloCrear()) {
                continue;
            }
            if (c.esCheck()) {
                body.put(c.nombre(), p.containsKey(c.nombre()));
                continue;
            }
            String v = p.getOrDefault(c.nombre(), "").trim();
            if (v.isEmpty()) {
                continue;
            }
            body.put(c.nombre(), switch (c.tipo()) {
                case NUMERO, SELECT_NUMERO -> Integer.valueOf(v);
                case DECIMAL -> Double.valueOf(v.replace(',', '.'));
                default -> v;
            });
        }
        return body;
    }
}
