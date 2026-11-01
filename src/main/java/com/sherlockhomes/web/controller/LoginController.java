package com.sherlockhomes.web.controller;

import com.sherlockhomes.web.api.ApiClient;
import com.sherlockhomes.web.api.ApiException;
import com.sherlockhomes.web.sesion.SesionUsuario;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final ApiClient api;
    private final SesionUsuario sesion;

    public LoginController(ApiClient api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
    }

    @GetMapping("/")
    public String inicio() {
        if (!sesion.estaLogueado()) {
            return "redirect:/login";
        }
        return switch (sesion.getTipoUsuario()) {
            case "ADMINISTRADOR" -> "redirect:/admin/plano";
            case "EMPLEADO" -> "redirect:/empleado/vehiculos";
            default -> "redirect:/socio/perfil";
        };
    }

    @GetMapping("/login")
    public String formulario(@RequestParam(required = false) String vencida, Model model) {
        if (vencida != null) {
            model.addAttribute("error", "La sesión venció. Ingresá de nuevo.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            Map<String, Object> r = api.autenticar(username.trim(), password);
            sesion.iniciar((String) r.get("token"), (String) r.get("tipoUsuario"), (String) r.get("username"));
            return "redirect:/";
        } catch (ApiException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String salir(HttpSession http) {
        sesion.cerrar();
        http.invalidate();
        return "redirect:/login";
    }
}
