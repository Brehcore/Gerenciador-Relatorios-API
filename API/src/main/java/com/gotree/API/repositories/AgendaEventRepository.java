package com.gotree.API.repositories;

import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    List<AgendaEvent> findByUserOrderByEventDateAsc(User user);

    List<AgendaEvent> findAllByOrderByEventDateAsc();

    // Encontra um evento de reagendamento pelo ID do relatório original
    Optional<AgendaEvent> findBySourceVisitId(Long sourceVisitId);

    // Encontra todos os eventos de um usuário que são de um certo tipo
    List<AgendaEvent> findByUserAndEventTypeIn(User user, List<String> types);
}