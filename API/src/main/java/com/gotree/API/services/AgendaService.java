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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de eventos de agenda e visitas técnicas.
 */
@Service
public class AgendaService {

    private final AgendaEventRepository agendaEventRepository;

    public AgendaService(AgendaEventRepository agendaEventRepository) {
        this.agendaEventRepository = agendaEventRepository;
    }

    public void validateReportSubmission(Long visitId, User technician, LocalDate date, String shiftStr, Company targetCompany) {
        try {
            Shift shift = Shift.valueOf(shiftStr.toUpperCase());
            List<AgendaEvent> conflictingEvents = agendaEventRepository.findByUserAndEventDateAndShift(technician, date, shift);

            for (AgendaEvent event : conflictingEvents) {
                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getId().equals(visitId)) {
                    continue;
                }
                if (event.getStatus() == AgendaStatus.CANCELADO || event.getStatus() == AgendaStatus.REAGENDADO) {
                    continue;
                }

                Long conflictingCompanyId = null;
                String conflictingClientName = "Outro Cliente";

                if (event.getTechnicalVisit() != null && event.getTechnicalVisit().getClientCompany() != null) {
                    conflictingCompanyId = event.getTechnicalVisit().getClientCompany().getId();
                    conflictingClientName = event.getTechnicalVisit().getClientCompany().getName();
                } else if (event.getCompany() != null) {
                    conflictingCompanyId = event.getCompany().getId();
                    conflictingClientName = event.getCompany().getName();
                }

                if (targetCompany != null && targetCompany.getId().equals(conflictingCompanyId)) {
                    continue;
                }

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

    public String checkAvailability(User technician, LocalDate date, String shiftStr) {
        try {
            long eventsInDay = agendaEventRepository.countByUserAndEventDate(technician, date);
            if (eventsInDay >= 2) {
                return "Você já possui 2 eventos agendados nesta data. Escolha outra data.";
            }

            if (shiftStr != null && !shiftStr.isBlank()) {
                Shift shift = Shift.valueOf(shiftStr.toUpperCase());
                long eventsInShift = agendaEventRepository.countByUserAndEventDateAndShift(technician, date, shift);
                if (eventsInShift > 0) {
                    return "Você já possui uma visita agendada neste turno (" + shift + "). Escolha outro turno.";
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            return "Turno inválido.";
        }
    }

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
        event.setStatus(AgendaStatus.CONFIRMADO);
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        try {
            if (dto.getShift() != null && !dto.getShift().isBlank()) {
                event.setShift(Shift.valueOf(dto.getShift().toUpperCase()));
            }
            AgendaEventType type = AgendaEventType.valueOf(dto.getEventType().toUpperCase());
            event.setEventType(type);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Dados inválidos: " + e.getMessage());
        }

        return agendaEventRepository.save(event);
    }

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
        event.setClientName(dto.getClientName());
        event.setManualObservation(dto.getManualObservation());

        return agendaEventRepository.save(event);
    }

    @Transactional
    public void rescheduleVisit(Long eventId, RescheduleVisitDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para reagendar a agenda de outro técnico.");
        }

        if (event.getOriginalVisitDate() == null) {
            event.setOriginalVisitDate(event.getEventDate());
        }

        event.setEventDate(dto.getNewDate());

        if (dto.getShift() != null) {
            event.setShift(com.gotree.API.enums.Shift.valueOf(dto.getShift()));
        } else {
            event.setShift(null);
        }

        event.setStatus(AgendaStatus.REAGENDADO);

        if (dto.getReason() != null && !dto.getReason().trim().isEmpty()) {
            String obs = event.getManualObservation() != null ? event.getManualObservation() + " | " : "";
            event.setManualObservation(obs + "Motivo Reagendamento: " + dto.getReason());
        }

        agendaEventRepository.save(event);
    }

    @Transactional
    public void reportVisitNotRealized(Long eventId, ReportNotRealizedDTO dto, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para alterar o status do agendamento de outro técnico.");
        }

        event.setIsRealized(false);
        event.setNonCompletionReason(dto.getReason());
        agendaEventRepository.save(event);
    }

    @Transactional
    public void markEventAsRealized(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Você não tem permissão para alterar a execução da agenda de outro técnico.");
        }

        event.setIsRealized(true);
        event.setNonCompletionReason(null);
        agendaEventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão.");
        }
        agendaEventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForUser(User user) {
        return agendaEventRepository.findByUserOrderByEventDateAsc(user)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findUpcomingEventsForUser(User user) {
        LocalDate today = LocalDate.now();
        return agendaEventRepository.findByUserAndEventDateGreaterThanEqualOrderByEventDateAsc(user, today)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> findAllEventsForAdmin(Long userId) {
        List<AgendaEvent> persistentEvents;

        if (userId != null) {
            persistentEvents = agendaEventRepository.findByUserIdWithUserOrderByEventDateAsc(userId);
        } else {
            persistentEvents = agendaEventRepository.findAllByOrderByEventDateAsc();
        }

        return persistentEvents.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> getReportData(LocalDate start, LocalDate end, Long userId, String eventType, String companyName) {
        List<AgendaEvent> allEventsInPeriod = agendaEventRepository.findAllByEventDateBetween(start, end);

        return allEventsInPeriod.stream()
                .filter(e -> userId == null || (e.getUser() != null && e.getUser().getId().equals(userId)))
                .filter(e -> eventType == null || eventType.isBlank() || (e.getEventType() != null && e.getEventType().name().equalsIgnoreCase(eventType)))
                .map(this::mapToDto)
                .filter(dto -> companyName == null || companyName.isBlank() ||
                        (dto.getClientName() != null && dto.getClientName().toLowerCase().contains(companyName.toLowerCase())))
                .peek(dto -> {
                    if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
                        dto.setStatus(com.gotree.API.enums.AgendaStatus.A_CONFIRMAR.name());
                        dto.setStatusDescricao(com.gotree.API.enums.AgendaStatus.A_CONFIRMAR.getDescricao());
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyAvailabilityDTO> getMonthAvailability(int year, int month) {
        List<MonthlyAvailabilityDTO> availabilityList = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<AgendaEvent> monthEvents = agendaEventRepository.findAllByEventDateBetween(startDate, endDate);

        for (int i = 1; i <= startDate.lengthOfMonth(); i++) {
            LocalDate currentDate = LocalDate.of(year, month, i);
            MonthlyAvailabilityDTO dto = getMonthlyAvailabilityDTO(monthEvents, currentDate);
            availabilityList.add(dto);
        }

        return availabilityList;
    }

    private static @NonNull MonthlyAvailabilityDTO getMonthlyAvailabilityDTO(List<AgendaEvent> monthEvents, LocalDate currentDate) {
        boolean morningBusy = false;
        boolean afternoonBusy = false;

        for (AgendaEvent event : monthEvents) {
            if (event.getEventDate().equals(currentDate) && event.getStatus() != AgendaStatus.CANCELADO) {
                if (event.getShift() != null) {
                    String shiftStr = event.getShift().name().toUpperCase();
                    if (shiftStr.equals("MANHA") || shiftStr.equals("MORNING")) morningBusy = true;
                    else if (shiftStr.equals("TARDE") || shiftStr.equals("AFTERNOON")) afternoonBusy = true;
                }
            }
        }

        MonthlyAvailabilityDTO dto = new MonthlyAvailabilityDTO();
        dto.setDate(currentDate);
        dto.setMorningBusy(morningBusy);
        dto.setAfternoonBusy(afternoonBusy);
        dto.setFullDayBusy(morningBusy && afternoonBusy);
        return dto;
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
        if (event.getEventType() != null) {
            String tipoRaw = event.getEventType().name();
            switch (tipoRaw) {
                case "EVENTO":
                    dto.setType("Evento");
                    break;
                case "TREINAMENTO":
                    dto.setType("Treinamento");
                    break;
                case "VISITA_TECNICA":
                    dto.setType("Visita Técnica");
                    break;
                case "VISITA_COMERCIAL":
                    dto.setType("Visita Comercial");
                    break;
                case "VISITA_SAUDE":
                    dto.setType("Visita Saúde");
                    break;
                case "GESTÃO":
                    dto.setType("Gestão");
                    break;
                case "PERICIA":
                    dto.setType("Perícia");
                    break;
                default:
                    // Fallback genérico: remove sublinhados e capitaliza
                    String formatado = tipoRaw.replace("_", " ").toLowerCase();
                    dto.setType(formatado.substring(0, 1).toUpperCase() + formatado.substring(1));
            }
        }
        dto.setDescription(event.getDescription());
        dto.setIsRealized(event.getIsRealized());
        dto.setNonCompletionReason(event.getNonCompletionReason());

        // --- MAPEAMENTO DE EMPRESA UNIDADE E SETOR DA AGENDA COM CNPJ ---
        if (event.getUnit() != null) {
            dto.setUnitId(event.getUnit().getId());
            dto.setUnitName(event.getUnit().getName());
            dto.setUnitCnpj(event.getUnit().getCnpj());
        }
        if (event.getSector() != null) {
            dto.setSectorId(event.getSector().getId());
            dto.setSectorName(event.getSector().getName());
        }
        // FORMATAÇÃO DO TURNO
        if (event.getShift() != null) {
            String turnoRaw = event.getShift().name();
            if (turnoRaw.equals("MANHA") || turnoRaw.equals("MORNING")) {
                dto.setShift("Manhã");
            } else if (turnoRaw.equals("TARDE") || turnoRaw.equals("AFTERNOON")) {
                dto.setShift("Tarde");
            } else {
                // Prevenção genérica: Primeira maiúscula, resto minúscula
                dto.setShift(turnoRaw.substring(0, 1).toUpperCase() + turnoRaw.substring(1).toLowerCase());
            }
        }
        if (event.getUser() != null) {
            dto.setResponsibleName(event.getUser().getName());
            dto.setResponsibleId(event.getUser().getId());
        }

        if (event.getStatus() != null) {
            dto.setStatus(event.getStatus().name());
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

        // --- DADOS DO CLIENTE COM CNPJ ---
        // Dados do Cliente
        if (event.getTechnicalVisit() != null) {
            TechnicalVisit v = event.getTechnicalVisit();
            dto.setSourceVisitId(v.getId());
            if (v.getClientCompany() != null) {
                dto.setClientName(v.getClientCompany().getName());
                dto.setCompanyCnpj(v.getClientCompany().getCnpj());
            }
            if (v.getUnit() != null) {
                dto.setUnitName(v.getUnit().getName());
                dto.setUnitCnpj(v.getUnit().getCnpj());
            }
            if (v.getSector() != null) dto.setSectorName(v.getSector().getName());
        } else {
            // CORREÇÃO: Traz o nome do cliente original
            dto.setClientName(event.getClientName());

            if (event.getCompany() != null) {
                dto.setCompanyCnpj(event.getCompany().getCnpj());

                // NOVO: Se o clientName veio nulo ou vazio do banco, puxa o nome oficial da tabela de Empresa
                if (dto.getClientName() == null || dto.getClientName().isBlank()) {
                    dto.setClientName(event.getCompany().getName());
                }
            }

            // Tratamento contra null pointer caso nem a empresa, nem o clientName existam
            if (dto.getClientName() == null || dto.getClientName().isBlank()) {
                dto.setClientName("Empresa não informada");
            }

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
                if (evt.getStatus() == AgendaStatus.CANCELADO) continue;

                if (!evt.getUser().getId().equals(currentUser.getId())) {
                    busyTechnicians.add(evt.getUser().getName());
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

    @Transactional(readOnly = true)
    public List<AgendaResponseDTO> getGlobalEvents(LocalDate startDate, LocalDate endDate) {
        return agendaEventRepository.findAllByEventDateBetween(startDate, endDate)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void confirmVisit(Long eventId, User currentUser) {
        AgendaEvent event = agendaEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Evento da agenda não encontrado."));

        if (!event.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Sem permissão para confirmar a agenda de outro técnico.");
        }

        event.setStatus(AgendaStatus.CONFIRMADO);
        agendaEventRepository.save(event);
    }
}