package com.cashi.wspinbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WspConversacionRepository extends JpaRepository<WspConversacion, Long> {

    Optional<WspConversacion> findByChatJid(String chatJid);

    List<WspConversacion> findAllByOrderByUltimaActividadDesc();

    /**
     * INSERT IGNORE evita el duplicate-key cuando múltiples mensajes llegan
     * concurrentemente para el mismo JID (p.ej. en el history sync inicial).
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO wsp_conversaciones (chat_jid, chat_titulo, instancia_id) " +
                   "VALUES (:jid, :titulo, :instanciaId)",
           nativeQuery = true)
    void insertIgnore(@Param("jid") String jid,
                      @Param("titulo") String titulo,
                      @Param("instanciaId") String instanciaId);
}
