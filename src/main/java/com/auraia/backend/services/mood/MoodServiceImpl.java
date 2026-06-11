package com.auraia.backend.services.mood;

import com.auraia.backend.exceptions.ResourceNotFoundException;
import com.auraia.backend.models.dto.request.DomainRequests;
import com.auraia.backend.models.dto.response.AuthResponses;
import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.PageResponse;
import com.auraia.backend.models.entities.MoodLog;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.MoodLogRepository;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.security.SecurityUtils;
import com.auraia.backend.services.privacy.ContentCryptoService;
import com.auraia.backend.services.push.WebPushService;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que trabaja con los registros de ánimo del usuario.
 * Aquí se guardan sesiones y se calculan estadísticas sencillas.
 */
@Service
@RequiredArgsConstructor
public class MoodServiceImpl implements MoodService {

    private final MoodLogRepository moodLogRepository;
    private final UserRepository userRepository;
    private final ContentCryptoService contentCryptoService;
    private final WebPushService webPushService;

    /**
     * Busca los registros de ánimo del usuario entre dos fechas.
     * Si no llegan fechas, usa un rango amplio por defecto.
     */
    @Override
    @Transactional
    public PageResponse<DomainResponses.MoodLogResponse> list(Instant desde, Instant hasta, Pageable paginacion) {
        User usuario = usuarioActual();
        Instant fechaInicio = Instant.EPOCH;
        Instant fechaFin = Instant.now().plusSeconds(24 * 60 * 60);

        if (desde != null) {
            fechaInicio = desde;
        }
        if (hasta != null) {
            fechaFin = hasta;
        }

        return PageResponse.from(moodLogRepository.buscarPaginaEntreFechas(usuario, fechaInicio, fechaFin, paginacion)
            .map(registro -> desencriptarYProteger(registro, usuario)));
    }

    /**
     * Guarda una sesión de ánimo y revisa si hay una caída fuerte.
     */
    @Override
    @Transactional
    public DomainResponses.MoodLogResponse create(DomainRequests.MoodLogRequest peticion) {
        User usuario = usuarioActual();
        String nota = textoVacioANull(peticion.getNote());
        Instant fechaRegistro = Instant.now();
        if (peticion.getLoggedAt() != null) {
            fechaRegistro = peticion.getLoggedAt();
        }

        MoodLog registro = MoodLog.builder()
            .user(usuario)
            .beforeLevel(peticion.getBeforeLevel())
            .afterLevel(peticion.getAfterLevel())
            .note(contentCryptoService.encrypt(usuario.getId(), "mood.note", nota))
            .loggedAt(fechaRegistro)
            .build();

        MoodLog registroGuardado = moodLogRepository.save(registro);
        avisarSiCaeMucho(usuario, registroGuardado);
        return crearRespuesta(registroGuardado, nota);
    }

    /**
     * Calcula medias y tendencia usando los registros del usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public DomainResponses.MoodStatsResponse stats(Instant desde, Instant hasta) {
        User usuario = usuarioActual();
        Date fechaFin = new Date();
        if (hasta != null) {
            fechaFin = Date.from(hasta);
        }

        Date fechaInicio = new Date(fechaFin.getTime() - 7L * 24 * 60 * 60 * 1000);
        if (desde != null) {
            fechaInicio = Date.from(desde);
        }

        MoodLog[] registros = moodLogRepository.buscarEntreFechas(usuario, fechaInicio.toInstant(), fechaFin.toInstant())
            .toArray(new MoodLog[0]);
        if (registros.length == 0) {
            return new DomainResponses.MoodStatsResponse(fechaInicio.toInstant(), fechaFin.toInstant(), 0, 0, 0, 0, "stable",
                0, 0, 0, false, "estable");
        }

        double diferenciaTendencia = calcularDiferenciaTendencia(registros);
        String estado = calcularTendencia(diferenciaTendencia);

        return new DomainResponses.MoodStatsResponse(
            fechaInicio.toInstant(),
            fechaFin.toInstant(),
            registros.length,
            redondear(mediaAntes(registros)),
            redondear(mediaDespues(registros)),
            redondear(calcularMejora(registros)),
            estadoAnterior(estado),
            redondear(calcularMediaAnterior(registros)),
            redondear(calcularMediaReciente(registros)),
            redondear(diferenciaTendencia),
            hayCaidaFuerte(registros),
            estado
        );
    }

    /**
     * Borra un registro de animo del usuario actual.
     */
    @Override
    @Transactional
    public AuthResponses.MessageResponse delete(UUID id) {
        User usuario = usuarioActual();
        MoodLog registro = moodLogRepository.buscarPorIdYUsuario(id, usuario);
        if (registro == null) {
            throw new ResourceNotFoundException("Mood log not found");
        }
        moodLogRepository.delete(registro);
        return new AuthResponses.MessageResponse("OK");
    }

    /**
     * Busca el usuario que está usando la aplicación.
     */
    private User usuarioActual() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Convierte textos vacíos en null para no guardar espacios sin valor.
     */
    private String textoVacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /**
     * Redondea números para que las estadísticas salgan más limpias.
     */
    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Mira los últimos registros y manda aviso si el ánimo baja mucho.
     */
    private void avisarSiCaeMucho(User usuario, MoodLog registro) {
        MoodLog[] registrosAnteriores = moodLogRepository.buscarAnteriores(
            usuario,
            registro.getLoggedAt(),
            PageRequest.of(0, 3)
        ).toArray(new MoodLog[0]);

        if (registrosAnteriores.length < 3) {
            return;
        }

        double mediaAnterior = mediaDespues(registrosAnteriores);
        if (registro.getAfterLevel() >= mediaAnterior + 3) {
            webPushService.enviarAlertaCaidaAnimo(usuario, registro, redondear(mediaAnterior));
        }
    }

    /**
     * Calcula la media de la parte antigua de los registros.
     */
    private double calcularMediaAnterior(MoodLog[] registros) {
        if (registros.length < 2) {
            return mediaDespues(registros);
        }

        int mitad = registros.length / 2;
        return mediaDespues(registros, 0, mitad);
    }

    /**
     * Saca la media de los registros más recientes.
     */
    private double calcularMediaReciente(MoodLog[] registros) {
        if (registros.length < 2) {
            return mediaDespues(registros);
        }

        int mitad = registros.length / 2;
        return mediaDespues(registros, mitad, registros.length);
    }

    /**
     * Devuelve el porcentaje de mejora entre antes y después.
     */
    private double calcularMejora(MoodLog[] registros) {
        double antes = mediaAntes(registros);
        double despues = mediaDespues(registros);

        if (antes == 0) {
            return 0;
        }

        return ((antes - despues) / antes) * 100;
    }

    /**
     * Resta la media reciente menos la media anterior.
     */
    private double calcularDiferenciaTendencia(MoodLog[] registros) {
        return calcularMediaReciente(registros) - calcularMediaAnterior(registros);
    }

    /**
     * Calcula la media del nivel antes de usar Aura.
     */
    private double mediaAntes(MoodLog[] registros) {
        if (registros.length == 0) {
            return 0;
        }

        double suma = 0;
        for (MoodLog registro : registros) {
            suma = suma + registro.getBeforeLevel();
        }
        return suma / registros.length;
    }

    /**
     * Calcula la media del nivel después de usar Aura.
     */
    private double mediaDespues(MoodLog[] registros) {
        return mediaDespues(registros, 0, registros.length);
    }

    /**
     * Calcula una media usando solo una parte del array.
     */
    private double mediaDespues(MoodLog[] registros, int inicio, int fin) {
        if (fin <= inicio) {
            return 0;
        }

        double suma = 0;
        for (int i = inicio; i < fin; i++) {
            suma = suma + registros[i].getAfterLevel();
        }
        return suma / (fin - inicio);
    }

    /**
     * Devuelve el texto que verá el frontend para la tendencia.
     */
    private String calcularTendencia(double diferenciaTendencia) {
        if (diferenciaTendencia <= -1) {
            return "mejorando";
        }
        if (diferenciaTendencia >= 1) {
            return "empeorando";
        }
        return "estable";
    }

    /**
     * Devuelve el valor antiguo que ya usaba el frontend.
     */
    private String estadoAnterior(String estado) {
        if ("mejorando".equals(estado)) {
            return "improving";
        }
        if ("empeorando".equals(estado)) {
            return "declining";
        }
        return "stable";
    }

    /**
     * Comprueba si el último registro está mucho peor que los anteriores.
     */
    private boolean hayCaidaFuerte(MoodLog[] registros) {
        if (registros.length < 4) {
            return false;
        }

        MoodLog ultimo = registros[registros.length - 1];
        double mediaAnterior = mediaDespues(registros, registros.length - 4, registros.length - 1);
        return ultimo.getAfterLevel() >= mediaAnterior + 3;
    }

    /**
     * Devuelve la nota del registro ya preparada para mostrarla.
     * Si hace falta, tambien actualiza el valor protegido en base de datos.
     */
    private DomainResponses.MoodLogResponse desencriptarYProteger(MoodLog registro, User usuario) {
        String nota = contentCryptoService.decrypt(usuario.getId(), "mood.note", registro.getNote());
        if (contentCryptoService.isEnabled()) {
            String notaCifrada = registro.getNote();
            if (registro.getNote() != null && !contentCryptoService.isEncrypted(registro.getNote())) {
                notaCifrada = contentCryptoService.encrypt(usuario.getId(), "mood.note", nota);
            }
            if (!java.util.Objects.equals(registro.getNote(), notaCifrada)) {
                registro.setNote(notaCifrada);
                moodLogRepository.save(registro);
            }
        }
        return crearRespuesta(registro, nota);
    }

    /**
     * Crea la respuesta final de un registro de animo.
     */
    private DomainResponses.MoodLogResponse crearRespuesta(MoodLog registro, String nota) {
        return new DomainResponses.MoodLogResponse(
            registro.getId(),
            registro.getBeforeLevel(),
            registro.getAfterLevel(),
            nota,
            registro.getLoggedAt(),
            registro.getCreatedAt(),
            registro.getUpdatedAt()
        );
    }
}
