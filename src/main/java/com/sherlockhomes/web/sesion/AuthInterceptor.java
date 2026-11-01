package com.sherlockhomes.web.sesion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Cada seccion exige un rol: /admin, /empleado y /socio. Si no corresponde, vuelve al login. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SesionUsuario sesion;

    public AuthInterceptor(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String rol = uri.startsWith("/admin") ? "ADMINISTRADOR" : uri.startsWith("/empleado") ? "EMPLEADO" : "SOCIO";
        if (sesion.estaLogueado() && rol.equals(sesion.getTipoUsuario())) {
            return true;
        }
        response.sendRedirect("/login");
        return false;
    }
}
