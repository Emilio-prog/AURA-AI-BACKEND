package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.MoodLog;
import com.auraia.backend.models.entities.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio para consultar y guardar registros de animo.
 */
public interface MoodLogRepository extends JpaRepository<MoodLog, UUID> {

    /**
     * Busca registros de animo entre dos fechas y los devuelve paginados.
     */
    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt >= :fechaInicio
        and registro.loggedAt <= :fechaFin
        """)
    Page<MoodLog> buscarPaginaEntreFechas(User usuario, Instant fechaInicio, Instant fechaFin, Pageable paginacion);

    /**
     * Devuelve los registros entre dos fechas, ordenados de antiguo a reciente.
     */
    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt >= :fechaInicio
        and registro.loggedAt <= :fechaFin
        order by registro.loggedAt asc
        """)
    List<MoodLog> buscarEntreFechas(User usuario, Instant fechaInicio, Instant fechaFin);

    /**
     * Busca registros anteriores a una fecha concreta.
     */
    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt < :fecha
        order by registro.loggedAt desc
        """)
    List<MoodLog> buscarAnteriores(User usuario, Instant fecha, Pageable paginacion);

    /**
     * Busca un registro concreto, pero solo si pertenece al usuario.
     */
    @Query("""
        select registro from MoodLog registro
        where registro.id = :id
        and registro.user = :usuario
        """)
    MoodLog buscarPorIdYUsuario(UUID id, User usuario);

    /**
     * Cuenta cuantos registros de animo tiene un usuario.
     */
    long countByUser(User user);

    /**
     * Comprueba si ya existe un registro dentro de un rango de fechas.
     */
    boolean existsByUserAndLoggedAtBetween(User user, Instant start, Instant end);

    /**
     * Devuelve solo las fechas de los registros del usuario.
     */
    @Query("select m.loggedAt from MoodLog m where m.user = :user")
    List<Instant> findLoggedAtByUser(User user);

    /**
     * Borra todos los registros de animo de un usuario.
     */
    void deleteAllByUser(User user);
}
