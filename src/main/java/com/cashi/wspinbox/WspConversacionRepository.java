package com.cashi.wspinbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WspConversacionRepository extends JpaRepository<WspConversacion, Long> {

    Optional<WspConversacion> findByChatJid(String chatJid);

    List<WspConversacion> findAllByOrderByUltimaActividadDesc();
}
