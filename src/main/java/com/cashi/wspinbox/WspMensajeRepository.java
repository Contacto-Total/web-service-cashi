package com.cashi.wspinbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WspMensajeRepository extends JpaRepository<WspMensaje, Long> {

    Optional<WspMensaje> findByWspMsgId(String wspMsgId);

    List<WspMensaje> findByConversacionIdOrderByTimestampWspAsc(Long conversacionId);
}
