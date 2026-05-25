package com.cashi.wspinbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WspInstanciaRepository extends JpaRepository<WspInstancia, String> {
    List<WspInstancia> findByActivaTrue();
}
