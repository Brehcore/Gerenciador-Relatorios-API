package com.gotree.API.repositories;

import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    /**
     * Encontra todos os eventos de agenda (ex: Reunião, Integração)
     * para um usuário específico, ordenados pela data.
     */
    List<AgendaEvent> findByUserOrderByEventDateAsc(User user);
}