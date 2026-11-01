package com.sherlockhomes.web.sesion;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/** Datos del usuario logueado. Hay una instancia por sesion HTTP. */
@Component
@SessionScope
public class SesionUsuario {

    private String token;
    private String tipoUsuario;
    private String username;

    public void iniciar(String token, String tipoUsuario, String username) {
        this.token = token;
        this.tipoUsuario = tipoUsuario;
        this.username = username;
    }

    public void cerrar() {
        token = null;
        tipoUsuario = null;
        username = null;
    }

    public boolean estaLogueado() {
        return token != null;
    }

    public String getToken() {
        return token;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public String getUsername() {
        return username;
    }
}
