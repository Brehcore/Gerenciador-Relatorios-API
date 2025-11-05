package com.gotree.API.services;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AgendaService {

    private final AgendaEventRepository agendaEventRepository;
    private final TechnicalVisitRepository technicalVisitRepository;

    public AgendaService(AgendaEventRepository agendaEventRepository,
                         TechnicalVisitRepository technicalVisitRepository) {
        this.agendaEventRepository = agendaEventRepository;
        this.technicalVisitRepository = technicalVisitRepository;
    }

    /**
     * Cria um novo evento de agenda genérico (ex: Reunião, Integração).
     */
    @Transactional
    public AgendaEvent createEvent(CreateEventDTO dto, User user) {
        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setUser(user);

        return agendaEventRepository.save(event);
    }

    /**
     * Busca TODOS os compromissos de um usuário (Eventos + Visitas Agendadas)
     * e os consolida em uma única lista para o front-end.
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForUser(User user) {

        List<AgendaResponseDTO> allEvents = new ArrayList<>();

        // 1. Buscar os Eventos Genéricos (Reuniões, Integrações, etc.)
        List<AgendaEvent> genericEvents = agendaEventRepository.findByUserOrderByEventDateAsc(user);
        for (AgendaEvent event : genericEvents) {
            AgendaResponseDTO dto = new AgendaResponseDTO();
            dto.setTitle(event.getTitle());
            dto.setDate(event.getEventDate());
            dto.setDescription(event.getDescription());
            dto.setType("EVENTO");
            dto.setReferenceId(event.getId());
            allEvents.add(dto);
        }

        // 2. Buscar as Visitas Agendadas (que vêm dos relatórios)
        List<TechnicalVisit> scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(user);

        for (TechnicalVisit visit : scheduledVisits) {
            AgendaResponseDTO dto = new AgendaResponseDTO();

            // --- Lógica de nomes (com verificação de nulo) ---
            String companyName = (visit.getClientCompany() != null) ? visit.getClientCompany().getName() : "Empresa N/A";
            String unitName = (visit.getUnit() != null) ? visit.getUnit().getName() : null;
            String sectorName = (visit.getSector() != null) ? visit.getSector().getName() : null;

            // --- Construção do Título ---
            // Título base: "Próxima Visita: Nome da Empresa"
            StringBuilder titleBuilder = new StringBuilder("Próxima Visita: " + companyName);

            // Adiciona Unidade, se existir
            if (unitName != null && !unitName.isBlank()) {
                titleBuilder.append(" (Unidade: ").append(unitName);

                // Adiciona Setor, mas SÓ SE a unidade também existir
                if (sectorName != null && !sectorName.isBlank()) {
                    titleBuilder.append(" - Setor: ").append(sectorName);
                }
                titleBuilder.append(")"); // Fecha o parêntese
            }
            // Caso não tenha Unidade, mas tenha Setor
            else if (sectorName != null && !sectorName.isBlank()) {
                titleBuilder.append(" (Setor: ").append(sectorName).append(")");
            }

            // --- Atribuição dos valores ao DTO ---
            dto.setTitle(titleBuilder.toString());
            dto.setDate(visit.getNextVisitDate());
            dto.setDescription(null);
            dto.setType("VISITA");
            dto.setReferenceId(visit.getId());

            // Popula os novos campos do DTO
            dto.setUnitName(unitName);
            dto.setSectorName(sectorName);

            allEvents.add(dto);
        }

        // 3. Ordenar a lista combinada por data
        allEvents.sort(Comparator.comparing(AgendaResponseDTO::getDate));

        return allEvents;
    }
}