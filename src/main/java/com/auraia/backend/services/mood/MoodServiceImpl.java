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

@Service
@RequiredArgsConstructor
public class MoodServiceImpl implements MoodService {

    private final MoodLogRepository moodLogRepository;
    private final UserRepository userRepository;
    private final ContentCryptoService contentCryptoService;
    private final WebPushService webPushService;

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

    private User usuarioActual() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String textoVacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

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

    private double calcularMediaAnterior(MoodLog[] registros) {
        if (registros.length < 2) {
            return mediaDespues(registros);
        }

        int mitad = registros.length / 2;
        return mediaDespues(registros, 0, mitad);
    }

    private double calcularMediaReciente(MoodLog[] registros) {
        if (registros.length < 2) {
            return mediaDespues(registros);
        }

        int mitad = registros.length / 2;
        return mediaDespues(registros, mitad, registros.length);
    }

    private double calcularMejora(MoodLog[] registros) {
        double antes = mediaAntes(registros);
        double despues = mediaDespues(registros);

        if (antes == 0) {
            return 0;
        }

        return ((antes - despues) / antes) * 100;
    }

    private double calcularDiferenciaTendencia(MoodLog[] registros) {
        return calcularMediaReciente(registros) - calcularMediaAnterior(registros);
    }

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

    private double mediaDespues(MoodLog[] registros) {
        return mediaDespues(registros, 0, registros.length);
    }

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

    private String calcularTendencia(double diferenciaTendencia) {
        if (diferenciaTendencia <= -1) {
            return "mejorando";
        }
        if (diferenciaTendencia >= 1) {
            return "empeorando";
        }
        return "estable";
    }

    private String estadoAnterior(String estado) {
        if ("mejorando".equals(estado)) {
            return "improving";
        }
        if ("empeorando".equals(estado)) {
            return "declining";
        }
        return "stable";
    }

    private boolean hayCaidaFuerte(MoodLog[] registros) {
        if (registros.length < 4) {
            return false;
        }

        MoodLog ultimo = registros[registros.length - 1];
        double mediaAnterior = mediaDespues(registros, registros.length - 4, registros.length - 1);
        return ultimo.getAfterLevel() >= mediaAnterior + 3;
    }

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
