package com.cashi.wspinbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WspMensajeRepository extends JpaRepository<WspMensaje, Long> {

    Optional<WspMensaje> findByWspMsgId(String wspMsgId);

    List<WspMensaje> findByConversacionIdOrderByTimestampWspAsc(Long conversacionId);

    // JOIN FETCH: carga conversacion y asesora de forma ansiosa para evitar
    // LazyInitializationException al mapear DTOs fuera de la transacción
    // (OSIV está deshabilitado: spring.jpa.open-in-view=false).
    @Query("SELECT m FROM WspMensaje m " +
           "JOIN FETCH m.conversacion c " +
           "LEFT JOIN FETCH m.asesora " +
           "WHERE c.instanciaId = :instanciaId " +
           "ORDER BY m.timestampWsp ASC")
    List<WspMensaje> findByInstanciaWithRelations(@Param("instanciaId") String instanciaId);
}
