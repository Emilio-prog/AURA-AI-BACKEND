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

public interface MoodLogRepository extends JpaRepository<MoodLog, UUID> {

    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt >= :fechaInicio
        and registro.loggedAt <= :fechaFin
        """)
    Page<MoodLog> buscarPaginaEntreFechas(User usuario, Instant fechaInicio, Instant fechaFin, Pageable paginacion);

    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt >= :fechaInicio
        and registro.loggedAt <= :fechaFin
        order by registro.loggedAt asc
        """)
    List<MoodLog> buscarEntreFechas(User usuario, Instant fechaInicio, Instant fechaFin);

    @Query("""
        select registro from MoodLog registro
        where registro.user = :usuario
        and registro.loggedAt < :fecha
        order by registro.loggedAt desc
        """)
    List<MoodLog> buscarAnteriores(User usuario, Instant fecha, Pageable paginacion);

    @Query("""
        select registro from MoodLog registro
        where registro.id = :id
        and registro.user = :usuario
        """)
    MoodLog buscarPorIdYUsuario(UUID id, User usuario);

    long countByUser(User user);

    boolean existsByUserAndLoggedAtBetween(User user, Instant start, Instant end);

    @Query("select m.loggedAt from MoodLog m where m.user = :user")
    List<Instant> findLoggedAtByUser(User user);

    void deleteAllByUser(User user);
}
