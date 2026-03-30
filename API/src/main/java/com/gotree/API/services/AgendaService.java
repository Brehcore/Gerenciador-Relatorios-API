package com.gotree.API.services;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.dto.agenda.CreateEventDTO;
import com.gotree.API.dto.agenda.MonthlyAvailabilityDTO;
import com.gotree.API.dto.agenda.ReportNotRealizedDTO;
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
            // 1. A verificação do dia inteiro sempre roda (máximo 2 visitas por dia)
            long eventsInDay = agendaEventRepository.countByUserAndEventDate(technician, date);
            if (eventsInDay >= 2) {
                return "Você já possui 2 eventos agendados nesta data. Escolha outra data.";
            }

            // 2. A verificação por turno SÓ roda se o turno foi informado
            if (shiftStr != null && !shiftStr.isBlank()) {
                Shift shift = Shift.valueOf(shiftStr.toUpperCase());
                long eventsInShift = agendaEventRepository.countByUserAndEventDateAndShift(technician, date, shift);
                if (eventsInShift > 0) {
                    return "Você já possui uma visita agendada neste turno (" + shift + "). Escolha outro turno.";
                }
            }

            return null; // Tudo limpo, pode agendar!

        } catch (IllegalArgumentException e) {
            return "Turno inválido.";
        }
    }

    /**
     * Cria um novo evento na agenda (Manual).
     */
    @Transactional
    public AgendaEvent createEvent(CreateEventDTO dto, User user) {
        // Agora o checkAvailability sabe lidar com turno nulo
        String conflict = checkAvailability(user, dto.getEventDate(), dto.getShift());
        if (conflict != null) {
            throw new IllegalStateException(conflict);
        }

        AgendaEvent event = new AgendaEvent();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setUser(user);
        event.setStatus(AgendaStatus.CONFIRMADO);
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        try {
            // Tratamento seguro para o turno que agora pode vir nulo
            if (dto.getShift() != null && !dto.getShift().isBlank()) {
                event.setShift(Shift.valueOf(dto.getShift().toUpperCase()));
            }

            // O tipo de evento continua obrigatório
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
    public void rescheduleVisit(Long eventId, RescheduleVisitDTO dto, User currentUser) {
        // 1. Busca o evento existente (não cria um novo!)
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        // 2. Garante que só o dono da agenda pode alterar
        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para reagendar a agenda de outro técnico.");
        }

        // 3. Salva a data original (se já houver uma data original gravada, ele não mexe, para manter o histórico da PRIMEIRA data marcada)
        if (event.getOriginalVisitDate() == null) {
            event.setOriginalVisitDate(event.getEventDate());
        }

        // 4. Aplica a nova data e o novo turno
        event.setEventDate(dto.getNewDate());

        if (dto.getShift() != null) {
            event.setShift(com.gotree.API.enums.Shift.valueOf(dto.getShift()));
        } else {
            event.setShift(null); // Permite ficar "A Definir"
        }

        // 5. Atualiza o status para REAGENDADO
        event.setStatus(AgendaStatus.REAGENDADO);

        // 6. Registra o motivo do reagendamento na observação (se houver)
        if (dto.getReason() != null && !dto.getReason().trim().isEmpty()) {
            String obs = event.getManualObservation() != null ? event.getManualObservation() + " | " : "";
            event.setManualObservation(obs + "Motivo Reagendamento: " + dto.getReason());
        }

        // 7. Salva a MESMA entidade (Isso gera um UPDATE no banco, não um INSERT)
        agendaEventRepository.save(event);
    }

    /**
     * Registra que uma visita não pôde ser realizada e salva o motivo.
     */
    @Transactional
    public void reportVisitNotRealized(Long eventId, ReportNotRealizedDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        // Garante que só o próprio técnico pode dar a visita como não realizada
        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para alterar o status do agendamento de outro técnico.");
        }

        // Atualiza a execução sem mexer no status original de agendamento
        event.setIsRealized(false);
        event.setNonCompletionReason(dto.getReason());

        agendaEventRepository.save(event);
    }

    /**
     * Marca um evento específico da agenda como REALIZADO.
     * Operação independente da geração do relatório técnico.
     */
    @Transactional
    public void markEventAsRealized(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        // Garante que só o próprio técnico (dono do evento) pode dar a baixa
        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para alterar a execução da agenda de outro técnico.");
        }

        // Marca como realizada e, por precaução, limpa qualquer motivo de "não realização" que pudesse estar salvo por engano
        event.setIsRealized(true);
        event.setNonCompletionReason(null);

        agendaEventRepository.save(event);
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
     * Busca os próximos eventos com base na data de hoje
     */
    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findUpcomingEventsForUser(User user) {
        LocalDate today = LocalDate.now();
        // Traz apenas as visitas de hoje para frente
        return agendaEventRepository.findByUserAndEventDateGreaterThanEqualOrderByEventDateAsc(user, today)
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
        dto.setOriginalVisitDate(event.getOriginalVisitDate());
        dto.setType(event.getEventType().name());
        dto.setDescription(event.getDescription());
        dto.setIsRealized(event.getIsRealized());
        dto.setNonCompletionReason(event.getNonCompletionReason());
        // --- MAPEAMENTO DE UNIDADE E SETOR DA AGENDA ---
        if (event.getUnit() != null) {
            dto.setUnitId(event.getUnit().getId());
            dto.setUnitName(event.getUnit().getName());
        }
        if (event.getSector() != null) {
            dto.setSectorId(event.getSector().getId());
            dto.setSectorName(event.getSector().getName());
        }
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
    public void confirmVisit(Long eventId, User currentUser) {
        // Agora buscamos pelo ID da Agenda (que é o que o Front-end envia)
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        // Garante que só o técnico dono do agendamento pode confirmar
        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão para confirmar a agenda de outro técnico.");
        }

        // Simplesmente atualiza o status para CONFIRMADO
        event.setStatus(AgendaStatus.CONFIRMADO);

        agendaEventRepository.save(event);
    }
}