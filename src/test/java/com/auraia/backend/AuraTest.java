package com.auraia.backend;

import com.auraia.backend.models.dto.request.AuthRequests;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.request.UserRequests;
import com.auraia.backend.models.dto.response.DomainResponses;
import java.util.Arrays;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * Pruebas sencillas para revisar los últimos cambios importantes del backend y que se vea claro 
 * qué se está comprobando.
 */
public class AuraTest {

    private AuthRequests.RegisterRequest peticionRegistro;
    private DomainResponses.MoodStatsResponse respuestaTendencia;
    private Date fechaInicio;
    private Date fechaFin;
    private String[] elementos = {};

    /**
     * Comprueba que la petición de registro guarda bien los datos básicos.
     */
    @Test
    public void registroConDatos() {
        peticionRegistro = new AuthRequests.RegisterRequest("Emilio", "emilio@test.com", "Password123!", null);

        if (!peticionRegistro.getName().equals("Emilio")) {
            throw new RuntimeException("El nombre no es correcto");
        }
        if (!peticionRegistro.getEmail().equals("emilio@test.com")) {
            throw new RuntimeException("El email no es correcto");
        }
        if (!peticionRegistro.getPassword().equals("Password123!")) {
            throw new RuntimeException("La contrasena no es correcta");
        }
    }

    /**
     * Revisa que un email mal escrito lance el error esperado.
     */
    @Test
    public void emailNoValido() {
        try {
            new AuthRequests.RegisterRequest("Emilio", "correo-mal", "Password123!", null);
            throw new RuntimeException("Tenia que fallar el email");
        } catch (IllegalArgumentException error) {
            if (!error.getMessage().equals("email no tiene un formato valido")) {
                throw new RuntimeException("El mensaje del email no es correcto");
            }
        }
    }

    /**
     * Prueba el caso en el que el onboarding llega sin nombre.
     */
    @Test
    public void nombreVacio() {
        try {
            crearOnboarding("");
            throw new RuntimeException("Tenia que fallar el nombre");
        } catch (IllegalArgumentException error) {
            if (!error.getMessage().contains("no puede estar vacio")) {
                throw new RuntimeException("El mensaje del nombre no es correcto");
            }
        }
    }

    /**
     * Comprueba que una entrada de diario sin contenido no sea válida.
     */
    @Test
    public void diarioSinContenido() {
        try {
            new DomainRequests.DiaryEntryRequest("Dia normal", "", 5, "bien", Arrays.asList(elementos));
            throw new RuntimeException("Tenia que fallar el diario");
        } catch (IllegalArgumentException error) {
            if (!error.getMessage().equals("content no puede estar vacio")) {
                throw new RuntimeException("El mensaje del diario no es correcto");
            }
        }
    }

    /**
     * Revisa los datos nuevos que se usan en la tendencia de ánimo.
     */
    @Test
    public void datosDeTendencia() {
        fechaInicio = new Date();
        fechaFin = new Date(fechaInicio.getTime() + 7L * 24 * 60 * 60 * 1000);

        respuestaTendencia = new DomainResponses.MoodStatsResponse(fechaInicio.toInstant(), fechaFin.toInstant(), 4,
            0, 0, 0, "improving", 8, 4, -4, true, "empeora");

        if (!respuestaTendencia.getFrom().equals(fechaInicio.toInstant())) {
            throw new RuntimeException("La fecha de inicio no es correcta");
        }
        if (!respuestaTendencia.getTo().equals(fechaFin.toInstant())) {
            throw new RuntimeException("La fecha de fin no es correcta");
        }
        if (respuestaTendencia.getCount() != 4) {
            throw new RuntimeException("El contador no es correcto");
        }
        if (!respuestaTendencia.getTrend().equals("improving")) {
            throw new RuntimeException("La tendencia antigua no es correcta");
        }
        if (respuestaTendencia.getMediaAnterior() != 8) {
            throw new RuntimeException("La media anterior no es correcta");
        }
        if (respuestaTendencia.getMediaReciente() != 4) {
            throw new RuntimeException("La media reciente no es correcta");
        }
        if (respuestaTendencia.getDiferenciaTendencia() != -4) {
            throw new RuntimeException("La diferencia de tendencia no es correcta");
        }
        if (!respuestaTendencia.isAlertaCaida()) {
            throw new RuntimeException("La alerta de caida no es correcta");
        }
        if (!respuestaTendencia.getTendencia().equals("empeora")) {
            throw new RuntimeException("La tendencia no es correcta");
        }
    }

    /**
     * Crea una petición de onboarding con datos mínimos para las pruebas.
     */
    private UserRequests.CompleteOnboardingRequest crearOnboarding(String nombre) {
        return new UserRequests.CompleteOnboardingRequest(nombre, "es", "Europe/Madrid", true, true, true, true,
            Arrays.asList(elementos), Arrays.asList(elementos), null, Arrays.asList(elementos), null, null);
    }
}
