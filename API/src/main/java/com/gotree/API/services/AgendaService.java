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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de eventos de agenda e visitas técnicas.
 * Fornece funcionalidades para criar, atualizar, excluir e consultar eventos,
 * além de gerenciar o reagendamento de visitas técnicas.
 */
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
                if (targetCompany != null && targetCompany.getId().equals(conflictingCompanyId)) {
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

        event.setStatus(AgendaStatus.CONFIRMADO); // Ajuste conforme sua necessidade

        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        try {
            event.setShift(Shift.valueOf(dto.getShift().toUpperCase()));
            AgendaEventType type = AgendaEventType.valueOf(dto.getEventType().toUpperCase());

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
     * Gera um evento de histórico na data antiga e move a visita para a nova data e turno.
     */
    @Transactional
    public void rescheduleVisit(Long visitId, RescheduleVisitDTO dto, User currentUser) {
        // 1. Busca a visita original
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visita não encontrada"));

        LocalDate dataAntiga = visit.getNextVisitDate();
        LocalDate dataNova = dto.getNewDate();

        // Guarda o turno antigo e converte o novo turno que veio do Frontend
        Shift turnoAntigo = visit.getNextVisitShift();
        Shift turnoNovo = Shift.valueOf(dto.getShift().toUpperCase());

        // 2. Cria o "Fantasma" do passado (Rastro para o relatório)
        AgendaEvent historicalEvent = new AgendaEvent();
        historicalEvent.setUser(currentUser);
        historicalEvent.setEventType(com.gotree.API.enums.AgendaEventType.VISITA_TECNICA);
        historicalEvent.setStatus(com.gotree.API.enums.AgendaStatus.REAGENDADO);

        // Título explicativo e vínculo
        historicalEvent.setTitle("Visita: " + (visit.getClientCompany() != null ? visit.getClientCompany().getName() : "N/A"));

        // Marca para quando foi reagendado (Auditoria)
        historicalEvent.setRescheduledToDate(dataNova);
        historicalEvent.setEventDate(dataAntiga);
        historicalEvent.setShift(turnoAntigo); // O histórico guarda o turno que era antes
        historicalEvent.setTechnicalVisit(visit);

        agendaEventRepository.save(historicalEvent);

        // 3. Atualiza a Visita Técnica real para o futuro (Nova Data + Novo Turno)
        visit.setNextVisitDate(dataNova);
        visit.setNextVisitShift(turnoNovo); // A CORREÇÃO ESTÁ AQUI!

        // Salva a visita com as novas definições
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
        // Simples e direto! Busca apenas na tabela de eventos e converte para DTO
        return agendaEventRepository.findByUserOrderByEventDateAsc(user)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Busca todos os eventos de agenda com filtro opcional por usuário (visão administrativa).
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForAdmin(Long userId) {
        List<AgendaEvent> persistentEvents;

        if (userId != null) {
            persistentEvents = agendaEventRepository.findByUserIdWithUserOrderByEventDateAsc(userId);
        } else {
            // Se já atualizou a query para não puxar lixo:
            persistentEvents = agendaEventRepository.findAllByOrderByEventDateAsc();
        }

        return persistentEvents.stream().map(this::mapToDto).toList();
    }

    /**
     * Específico para gerar os dados do relatório PDF (12 meses, etc).
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

    /**
     * Retorna a disponibilidade GLOBAL do mês para o calendário do Frontend.
     * Mostra se qualquer técnico da empresa tem visita agendada.
     */
    @Transactional(readOnly = true)
    public List<MonthlyAvailabilityDTO> getMonthAvailability(User user, int year, int month) {
        List<MonthlyAvailabilityDTO> availabilityList = new ArrayList<>();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // 1. BUSCA GLOBAL: Traz os eventos de TODOS os técnicos neste período
        List<AgendaEvent> monthEvents = agendaEventRepository.findAllByEventDateBetween(startDate, endDate);
        // 2. BUSCA GLOBAL: Traz as visitas de TODOS os técnicos neste período

        List<TechnicalVisit> monthVisits = technicalVisitRepository.findAllByNextVisitDateBetween(startDate, endDate);

        for (int i = 1; i <= startDate.lengthOfMonth(); i++) {
            LocalDate currentDate = LocalDate.of(year, month, i);

            boolean morningBusy = false;
            boolean afternoonBusy = false;

            // Checa eventos globais
            for (AgendaEvent event : monthEvents) {
                if (event.getEventDate().equals(currentDate) && event.getStatus() != com.gotree.API.enums.AgendaStatus.CANCELADO) {
                    String shiftStr = event.getShift().name().toUpperCase();
                    if (shiftStr.equals("MANHA") || shiftStr.equals("MORNING")) morningBusy = true;
                    else if (shiftStr.equals("TARDE") || shiftStr.equals("AFTERNOON")) afternoonBusy = true;
                }
            }

            // Checa visitas globais
            for (TechnicalVisit visit : monthVisits) {
                if (visit.getNextVisitDate() != null && visit.getNextVisitDate().equals(currentDate)) {
                    String shiftStr = visit.getNextVisitShift().name().toUpperCase();
                    if (shiftStr.equals("MANHA") || shiftStr.equals("MORNING")) morningBusy = true;
                    else if (shiftStr.equals("TARDE") || shiftStr.equals("AFTERNOON")) afternoonBusy = true;
                }
            }

            MonthlyAvailabilityDTO dto = new MonthlyAvailabilityDTO();
            dto.setDate(currentDate);
            dto.setMorningBusy(morningBusy);
            dto.setAfternoonBusy(afternoonBusy);
            dto.setFullDayBusy(morningBusy && afternoonBusy);

            availabilityList.add(dto);
        }

        return availabilityList;
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
        if (event.getUser() != null) {
            dto.setResponsibleName(event.getUser().getName());
            dto.setResponsibleId(event.getUser().getId());
        }

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
        return agendaEventRepository.findAllByEventDateBetween(startDate, endDate)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void confirmVisit(Long visitId, User currentUser) {
        // Busca a visita técnica
        TechnicalVisit visit = technicalVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visita Técnica não encontrada."));

        if (!visit.getTechnician().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão para confirmar a visita de outro técnico.");
        }

        // Opção A: Apenas cria um AgendaEvent "Confirmado" vinculado (Recomendado para manter padrão)
        AgendaEvent confirmation = agendaEventRepository.findByTechnicalVisit_Id(visitId)
                .orElse(new AgendaEvent());

        if (confirmation.getId() == null) {
            // Se ainda não existe evento manual vinculado, cria um novo
            confirmation.setTechnicalVisit(visit);
            confirmation.setUser(visit.getTechnician());
            confirmation.setEventDate(visit.getNextVisitDate());
            confirmation.setShift(visit.getNextVisitShift());
            confirmation.setTitle(visit.getTitle());
            confirmation.setEventType(AgendaEventType.VISITA_TECNICA);
        }

        // Atualiza o status para CONFIRMADO
        confirmation.setStatus(AgendaStatus.CONFIRMADO);

        agendaEventRepository.save(confirmation);
    }
}