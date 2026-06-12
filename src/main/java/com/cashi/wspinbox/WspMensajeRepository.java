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

    // Mensajes de un chat por JID con paginación (los más recientes primero,
    // filtrado por before cuando > 0). El Go service usa esto para cargar
    // mensajes on-demand cuando no los tiene en RAM.
    @Query("SELECT m FROM WspMensaje m " +
           "JOIN FETCH m.conversacion c " +
           "LEFT JOIN FETCH m.asesora " +
           "WHERE c.chatJid = :chatJid " +
           "  AND (:before = 0 OR m.timestampWsp < :before) " +
           "ORDER BY m.timestampWsp DESC")
    List<WspMensaje> findByJidPagedRaw(
        @Param("chatJid") String chatJid,
        @Param("before")  long   before,
        org.springframework.data.domain.Pageable pageable);

    default List<WspMensaje> findByJidPaged(String chatJid, int limit, long before) {
        List<WspMensaje> desc = findByJidPagedRaw(
            chatJid, before,
            org.springframework.data.domain.PageRequest.of(0, limit));
        // Revertir a orden ascendente para que el Go lo procese igual que el resto
        java.util.Collections.reverse(desc);
        return desc;
    }
}
