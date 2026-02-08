package com.gotree.API.services;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.RescheduleVisitDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.User;
import com.gotree.API.enums.AgendaEventType;
import com.gotree.API.enums.AgendaStatus;
import com.gotree.API.enums.Shift;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.TechnicalVisitRepository;
import com.gotree.API.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Serviço responsável pelo gerenciamento de eventos de agenda e visitas técnicas.
 * Fornece funcionalidades para criar, atualizar, excluir e consultar eventos,
 * além de gerenciar o reagendamento de visitas técnicas.
 */
@Service
public class AgendaService {

    private final AgendaEventRepository agendaEventRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final UserRepository userRepository;

    public AgendaService(AgendaEventRepository agendaEventRepository,
                         TechnicalVisitRepository technicalVisitRepository,
                         UserRepository userRepository) {
        this.agendaEventRepository = agendaEventRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.userRepository = userRepository;
    }

    /**
     * Bloqueia se houver conflito com visita de outra empresa no mesmo turno.
     *
     * @param visitId       ID da visita técnica que está sendo finalizada
     * @param technician    Técnico responsável
     * @param date          Data do relatório/visita
     * @param shiftStr      Turno selecionado
     * @param targetCompany Empresa alvo da visita
     */
    public void validateReportSubmission(Long visitId, User technician, LocalDate date, String shiftStr, Company targetCompany) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());

            // Busca eventos existentes nesse horário
            List<AgendaEvent> conflictingEvents = agendaEventRepository.findByUserAndEventDateAndShift(technician, date, shift);

            for (AgendaEvent event : conflictingEvents) {
                // Se for a própria visita (em caso de edição), ignora
                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getId().equals(visitId)) {
                    continue;
                }

                // Se o evento estiver CANCELADO ou REAGENDADO (histórico), não deve bloquear
                if (event.getStatus() == AgendaStatus.CANCELADO || event.getStatus() == AgendaStatus.REAGENDADO) {
                    continue;
                }

                // Identifica a empresa do evento conflitante
                Long conflictingCompanyId = null;
                String conflictingClientName = "Outro Cliente";

                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getClientCompany() != null) {
                    conflictingCompanyId = event.getTechnicalVisit().getClientCompany().getId();
                    conflictingClientName = event.getTechnicalVisit().getClientCompany().getName();
                } else if (event.getCompany() != null) {
                    conflictingCompanyId = event.getCompany().getId();
                    conflictingClientName = event.getCompany().getName();
                }

                // A LÓGICA MÁGICA:
                // Se temos uma empresa alvo e ela é IGUAL à empresa do evento existente -> PERMITE
                if (targetCompany != null && conflictingCompanyId != null && targetCompany.getId().equals(conflictingCompanyId)) {
                    continue; // Mesma empresa, segue o jogo
                }

                // Se chegou aqui, é conflito real (empresas diferentes)
                throw new IllegalStateException("BLOQUEIO DE AGENDA: Você já possui um compromisso na empresa '"
                        + conflictingClientName + "' neste turno (" + shift + "). "
                        + "Agendamentos simultâneos só são permitidos na mesma empresa.");
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Turno inválido fornecido para validação.");
        }
    }

    // Metodo sobrecarregado para compatibilidade
    public void validateReportSubmission(Long visitId, User technician, LocalDate date, String shiftStr) {
        validateReportSubmission(visitId, technician, date, shiftStr, null);
    }

    /**
     * Verifica a disponibilidade de um técnico em uma data e turno específicos.
     */
    public String checkAvailability(User technician, LocalDate date, String shiftStr) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());

            // Nota: Idealmente filtrar eventos CANCELADOS/REAGENDADOS nestas contagens também
            // Mas mantendo a lógica simples original:
            long eventsInDay = agendaEventRepository.countByUserAndEventDate(technician, date);
            if (eventsInDay >= 2) {
                return "Você já possui visitas agendadas para os turnos manhã e tarde nesta data. Escolha outra data.";
            }
            long eventsInShift = agendaEventRepository.countByUserAndEventDateAndShift(technician, date, shift);
            if (eventsInShift > 0) {
                return "Você já possui uma visita agendada neste turno (" + shift + "). Escolha outro turno.";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return "Turno inválido.";
        }
    }

    /**
     * Cria um novo evento na agenda (Manual).
     */
    @Transactional
    public AgendaEvent createEvent(CreateEventDTO dto, User user) {
        String conflict = checkAvailability(user, dto.getEventDate(), dto.getShift());
        if (conflict != null) {
            throw new IllegalStateException(conflict);
        }

        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setUser(user);

        // Eventos criados manualmente nascem como "À Confirmar" ou "Confirmado" dependendo da regra de negócio.
        // Assumindo padrão "Confirmado" para manuais (ex: Reunião Interna) ou "À Confirmar"
        event.setStatus(AgendaStatus.CONFIRMADO); // Ajuste conforme sua necessidade

        // Salva os campos manuais
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        try {
            event.setShift(Shift.valueOf(dto.getShift().toUpperCase()));
            AgendaEventType type = AgendaEventType.valueOf(dto.getEventType().toUpperCase());

            // Se tentar criar VISITA_TECNICA manualmente, garantimos que não é um hack de reagendamento
            event.setEventType(type);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dados inválidos: " + e.getMessage());
        }

        return agendaEventRepository.save(event);
    }

    /**
     * Atualiza um evento existente na agenda.
     */
    @Transactional
    public AgendaEvent updateEvent(Long eventId, CreateEventDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão.");
        }

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        // Atualiza manuais também
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        return agendaEventRepository.save(event);
    }

    /**
     * Reagenda uma visita técnica.
     * Gera um evento de histórico na data antiga e move a visita para a nova data.
     */
    @Transactional
    public void rescheduleVisit(Long visitId, RescheduleVisitDTO dto, User currentUser) {
        // 1. Busca a visita original
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visita não encontrada"));

        LocalDate dataAntiga = visit.getNextVisitDate();
        LocalDate dataNova = dto.getNewDate();

        // 2. Cria o "Fantasma" do passado (Rastro para o relatório)
        AgendaEvent historicoEvent = new AgendaEvent();
        historicoEvent.setUser(currentUser);
        historicoEvent.setEventType(AgendaEventType.VISITA_TECNICA);
        historicoEvent.setStatus(AgendaStatus.REAGENDADO); // Define status Reagendado

        // Título explicativo e vínculo
        historicoEvent.setTitle("Visita: " + (visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A"));
        historicoEvent.setRescheduledToDate(dataNova); // Campo crucial para o PDF ("Reagendado p/...")

        historicoEvent.setEventDate(dataAntiga); // Fica preso na data ANTIGA
        historicoEvent.setShift(visit.getNextVisitShift()); // Turno original
        historicoEvent.setTechnicalVisit(visit); // Vincula para referência

        agendaEventRepository.save(historicoEvent);

        // 3. Atualiza a Visita Técnica real para o futuro
        visit.setNextVisitDate(dataNova);

        // Opcional: Se reagendou, precisa confirmar novamente?
        // Se TechnicalVisit não tiver campo status, o status será inferido como "À Confirmar"
        // no mapVisitToDto na próxima vez que essa visita for listada na data nova.

        technicalVisitRepository.save(visit);
    }

    /**
     * Remove um evento da agenda.
     */
    @Transactional
    public void deleteEvent(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão.");
        }
        agendaEventRepository.delete(event);
    }

    /**
     * Busca todos os eventos de agenda para um usuário específico.
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForUser(User user) {
        List<AgendaEvent> persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
        List<TechnicalVisit> scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(user);
        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    /**
     * Busca todos os eventos de agenda com filtro opcional por usuário (visão administrativa).
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForAdmin(Long userId) {
        List<AgendaEvent> persistentEvents;
        List<TechnicalVisit> scheduledVisits;

        if (userId != null) {
            User filterUser = userRepository.findById(userId).orElseThrow();
            persistentEvents = agendaEventRepository.findByUserIdWithUserOrderByEventDateAsc(userId);
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompanyByTechnician(filterUser);
        } else {
            persistentEvents = agendaEventRepository.findAllWithUserByOrderByEventDateAsc();
            scheduledVisits = technicalVisitRepository.findAllScheduledWithCompany();
        }

        return aggregateAndSortEvents(persistentEvents, scheduledVisits);
    }

    /**
     * Método específico para gerar os dados do relatório PDF (12 meses, etc).
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> getReportData(LocalDate start, LocalDate end) {
        // Reutiliza a lógica global que já busca manuais + visitas automáticas
        List<AgendaResponseDTO> reportData = getGlobalEvents(start, end);

        // Garantia extra: se algum item ficou sem status, define padrão
        for (AgendaResponseDTO dto : reportData) {
            if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
                dto.setStatus(AgendaStatus.A_CONFIRMAR.name());
                dto.setStatusDescricao(AgendaStatus.A_CONFIRMAR.getDescricao());
            }
        }
        return reportData;
    }

    @Transactional(readOnly = true)
    public List<MonthlyAvailabilityDTO> getMonthAvailability(User technician, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<TechnicalVisit> visitsRealized = technicalVisitRepository.findByTechnicianAndDateRange(technician, start, end);
        List<TechnicalVisit> visitsScheduled = technicalVisitRepository.findByTechnicianAndNextVisitDateBetween(technician, start, end);
        List<AgendaEvent> manualEvents = agendaEventRepository.findByUserAndEventDateBetween(technician, start, end);

        List<MonthlyAvailabilityDTO> availability = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            boolean morningBusy = false;
            boolean afternoonBusy = false;

            // --- A. Checa Visitas REALIZADAS ---
            for (TechnicalVisit v : visitsRealized) {
                if (v.getVisitDate().equals(currentDate)) {
                    if (v.getStartTime().getHour() < 12) morningBusy = true;
                    else afternoonBusy = true;
                }
            }

            // --- B. Checa Visitas AGENDADAS ---
            for (TechnicalVisit v : visitsScheduled) {
                if (v.getNextVisitDate().equals(currentDate)) {
                    if (v.getNextVisitShift() == Shift.MANHA) morningBusy = true;
                    else if (v.getNextVisitShift() == Shift.TARDE) afternoonBusy = true;
                }
            }

            // --- C. Checa Eventos Manuais ---
            for (AgendaEvent e : manualEvents) {
                if (e.getEventDate().equals(currentDate)) {
                    // Ignora cancelados
                    if (e.getStatus() == AgendaStatus.CANCELADO) continue;
                    // Evita duplicidade
                    if (e.getTechnicalVisit() != null) continue;

                    if (e.getShift() == Shift.MANHA) morningBusy = true;
                    else if (e.getShift() == Shift.TARDE) afternoonBusy = true;
                }
            }

            if (morningBusy || afternoonBusy) {
                MonthlyAvailabilityDTO dto = new MonthlyAvailabilityDTO();
                dto.setDate(date);
                dto.setMorningBusy(morningBusy);
                dto.setAfternoonBusy(afternoonBusy);
                dto.setFullDayBusy(morningBusy && afternoonBusy);
                availability.add(dto);
            }
        }
        return availability;
    }

    /**
     * Unifica eventos manuais (persistidos) e visitas técnicas (virtuais) em uma lista única ordenada.
     */
    private List<AgendaResponseDTO> aggregateAndSortEvents(List<AgendaEvent> persistentEvents, List<TechnicalVisit> scheduledVisits) {
        List<AgendaResponseDTO> allEvents = new ArrayList<>();
        Set<String> addedVirtualVisits = new java.util.HashSet<>();

        // 1. Adiciona TODOS os eventos manuais (incluindo os "rastros" de reagendamento)
        for (AgendaEvent event : persistentEvents) {
            allEvents.add(mapToDto(event));

            // Se esse evento está vinculado a uma visita, marcamos a visita como "processada"
            // para não adicioná-la duplicada (como visita virtual) nesta mesma data.
            if (event.getTechnicalVisit() != null) {
                // OBS: O rastro de reagendamento está na data antiga.
                // A visita virtual (data futura) ainda deve aparecer.
                // Portanto, só marcamos como processada se as datas baterem.
                if (event.getEventDate().equals(event.getTechnicalVisit().getNextVisitDate())) {
                    String shiftKey = (event.getShift() != null) ? event.getShift().name() : "N/A";
                    String clientKey = (event.getCompany() != null) ? event.getCompany().getId().toString() :
                            (event.getTechnicalVisit().getClientCompany() != null ? event.getTechnicalVisit().getClientCompany().getId().toString() : "0");
                    String uniqueKey = event.getEventDate().toString() + "_" + shiftKey + "_" + clientKey;
                    addedVirtualVisits.add(uniqueKey);
                }
            }
        }

        // 2. Adiciona as visitas técnicas futuras (que ainda não viraram eventos manuais/reagendados NESTA data)
        for (TechnicalVisit visit : scheduledVisits) {
            String shiftKey = (visit.getNextVisitShift() != null) ? visit.getNextVisitShift().name() : "N/A";
            String clientKey = (visit.getClientCompany() != null) ? visit.getClientCompany().getId().toString() : "0";
            String uniqueKey = visit.getNextVisitDate().toString() + "_" + shiftKey + "_" + clientKey;

            if (!addedVirtualVisits.contains(uniqueKey)) {
                allEvents.add(mapVisitToDto(visit));
                addedVirtualVisits.add(uniqueKey);
            }
        }

        allEvents.sort(Comparator.comparing(AgendaResponseDTO::getDate));
        return allEvents;
    }

    /**
     * Converte um evento de agenda (tabela tb_agenda_event) para DTO.
     */
    public AgendaResponseDTO mapToDto(AgendaEvent event) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        dto.setReferenceId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDate(event.getEventDate());
        dto.setType(event.getEventType().name());
        dto.setDescription(event.getDescription());
        if (event.getShift() != null) dto.setShift(event.getShift().name());
        if (event.getUser() != null) dto.setResponsibleName(event.getUser().getName());

        // Mapeia Status e Descrição
        if (event.getStatus() != null) {
            dto.setStatus(event.getStatus().name());

            // LÓGICA DE EXIBIÇÃO NO PDF (REAGENDAMENTO)
            if (event.getStatus() == AgendaStatus.REAGENDADO && event.getRescheduledToDate() != null) {
                String novaDataStr = event.getRescheduledToDate().format(DateTimeFormatter.ofPattern("dd/MM"));
                dto.setStatusDescricao("Reagendado p/ " + novaDataStr);
            } else {
                dto.setStatusDescricao(event.getStatus().getDescricao());
            }
        } else {
            dto.setStatus(AgendaStatus.A_CONFIRMAR.name());
            dto.setStatusDescricao(AgendaStatus.A_CONFIRMAR.getDescricao());
        }

        // Dados do Cliente
        if (event.getTechnicalVisit() != null) {
            TechnicalVisit v = event.getTechnicalVisit();
            dto.setSourceVisitId(v.getId());
            if (v.getClientCompany() != null) dto.setClientName(v.getClientCompany().getName());
            if (v.getUnit() != null) dto.setUnitName(v.getUnit().getName());
            if (v.getSector() != null) dto.setSectorName(v.getSector().getName());
        } else {
            dto.setClientName(event.getClientName());
            if (event.getManualObservation() != null) {
                String obs = dto.getDescription() != null ? dto.getDescription() + " | " : "";
                dto.setDescription(obs + event.getManualObservation());
            }
        }
        return dto;
    }

    /**
     * Converte uma Visita Técnica (virtual, sem ID em agenda_event) para DTO.
     */
    private AgendaResponseDTO mapVisitToDto(TechnicalVisit visit) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        String companyName = (visit.getClientCompany() != null) ? visit.getClientCompany().getName() : "Empresa N/A";
        String unitName = (visit.getUnit() != null) ? visit.getUnit().getName() : null;
        String sectorName = (visit.getSector() != null) ? visit.getSector().getName() : null;

        StringBuilder titleBuilder = new StringBuilder("Próxima Visita: " + companyName);
        if (unitName != null) titleBuilder.append(" (").append(unitName).append(")");

        dto.setTitle(titleBuilder.toString());
        dto.setDate(visit.getNextVisitDate());
        dto.setType(AgendaEventType.VISITA_TECNICA.name()); // Tipo padronizado
        dto.setReferenceId(visit.getId());

        if (visit.getNextVisitShift() != null) {
            dto.setShift(visit.getNextVisitShift().name());
        }

        dto.setClientName(companyName);
        dto.setUnitName(unitName);
        dto.setSectorName(sectorName);

        if (visit.getTechnician() != null) {
            dto.setResponsibleName(visit.getTechnician().getName());
        }

        // Visitas automáticas nascem "À Confirmar" até que alguém interaja
        dto.setStatus(AgendaStatus.A_CONFIRMAR.name());
        dto.setStatusDescricao(AgendaStatus.A_CONFIRMAR.getDescricao());

        return dto;
    }

    @Transactional(readOnly = true)
    public String checkGlobalConflicts(LocalDate date, String shiftStr, User currentUser) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());
            List<String> busyTechnicians = new ArrayList<>();

            List<AgendaEvent> events = agendaEventRepository.findAllByEventDateAndShift(date, shift);
            for (AgendaEvent evt : events) {
                // Ignora cancelados
                if (evt.getStatus() == AgendaStatus.CANCELADO) continue;

                if (!evt.getUser().getId().equals(currentUser.getId())) {
                    busyTechnicians.add(evt.getUser().getName());
                }
            }

            List<TechnicalVisit> visits = technicalVisitRepository.findAllByNextVisitDateAndNextVisitShift(date, shift);
            for (TechnicalVisit tv : visits) {
                if (tv.getTechnician() != null && !tv.getTechnician().getId().equals(currentUser.getId())) {
                    busyTechnicians.add(tv.getTechnician().getName());
                }
            }

            if (!busyTechnicians.isEmpty()) {
                String names = String.join(", ", busyTechnicians.stream().distinct().toList());
                return "Atenção: Os seguintes técnicos já possuem agendamento nesta data/turno: " + names;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Retorna a Agenda Global (Eventos de TODOS os técnicos) para um intervalo de datas.
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> getGlobalEvents(LocalDate startDate, LocalDate endDate) {
        List<AgendaEvent> allEvents = agendaEventRepository.findAllByEventDateBetween(startDate, endDate);
        List<TechnicalVisit> allVisits = technicalVisitRepository.findAllByNextVisitDateBetween(startDate, endDate);
        return aggregateAndSortEvents(allEvents, allVisits);
    }
}