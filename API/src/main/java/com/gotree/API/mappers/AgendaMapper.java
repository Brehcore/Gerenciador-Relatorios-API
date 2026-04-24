package com.gotree.API.mappers;

import com.gotree.API.dto.agenda.AgendaResponseDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.Company;
import com.gotree.API.entities.Sector;
import com.gotree.API.entities.TechnicalVisit;
import com.gotree.API.entities.Unit;
import com.gotree.API.enums.AgendaStatus;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class AgendaMapper {

    public AgendaResponseDTO mapToDto(AgendaEvent event) {
        AgendaResponseDTO dto = new AgendaResponseDTO();
        dto.setReferenceId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDate(event.getEventDate());
        dto.setOriginalVisitDate(event.getOriginalVisitDate());
        dto.setDescription(event.getDescription());
        dto.setIsRealized(event.getIsRealized());
        dto.setNonCompletionReason(event.getNonCompletionReason());
        dto.setEventHour(event.getEventHour());

        if (event.getEventType() != null) dto.setType(formatEventType(event.getEventType().name()));
        if (event.getShift() != null) dto.setShift(formatShift(event.getShift().name()));

        if (event.getUser() != null) {
            dto.setResponsibleName(event.getUser().getName());
            dto.setResponsibleId(event.getUser().getId());
        }

        mapStatusFields(dto, event);

        // Separação limpa entre Visita e Evento Manual
        if (event.getTechnicalVisit() != null) {
            mapVisitDetails(dto, event.getTechnicalVisit());
        } else {
            mapManualEventDetails(dto, event);
        }

        return dto;
    }

    public void updateEntityFromDto(AgendaEvent event, com.gotree.API.dto.agenda.CreateEventDTO dto) {
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());

        try {
            if (dto.getShift() != null && !dto.getShift().isBlank()) {
                event.setShift(com.gotree.API.enums.Shift.valueOf(dto.getShift().toUpperCase()));
            } else {
                event.setShift(null);
            }
            if (dto.getEventType() != null) {
                event.setEventType(dto.getEventType());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valores de Turno ou Tipo inválidos.");
        }
    }

    // --- MÉTODOS AUXILIARES PRIVADOS ---

    private void mapStatusFields(AgendaResponseDTO dto, AgendaEvent event) {
        if (event.getStatus() != null) {
            dto.setStatus(event.getStatus().name());
            if (event.getStatus() == AgendaStatus.REAGENDADO && event.getRescheduledToDate() != null) {
                dto.setStatusDescricao("Reagendado p/ " + event.getRescheduledToDate().format(DateTimeFormatter.ofPattern("dd/MM")));
            } else {
                dto.setStatusDescricao(event.getStatus().getDescricao());
            }
        } else {
            dto.setStatus(AgendaStatus.A_CONFIRMAR.name());
            dto.setStatusDescricao(AgendaStatus.A_CONFIRMAR.getDescricao());
        }
    }

    private void mapVisitDetails(AgendaResponseDTO dto, TechnicalVisit v) {
        dto.setSourceVisitId(v.getId());

        // Usa o metodo extraído passando as entidades da Visita
        mapLocationDetails(dto, v.getClientCompany(), v.getUnit(), v.getSector());
    }

    private void mapManualEventDetails(AgendaResponseDTO dto, AgendaEvent event) {
        // Usa o metodo extraído passando as entidades do Evento Manual
        mapLocationDetails(dto, event.getCompany(), event.getUnit(), event.getSector());

        // Regra de fallback (Exclusiva de Evento Manual)
        if (event.getCompany() == null) {
            dto.setCompanyName("Empresa não informada");
            // O ID já fica null por padrão na criação do DTO
        }

    }

    /**
     * Helper extraído para resolver a duplicação de código (DRY).
     * Mapeia Empresa, Unidade e Setor independentemente da origem (Visita ou Evento).
     */
    private void mapLocationDetails(AgendaResponseDTO dto, Company company, Unit unit, Sector sector) {
        if (company != null) {
            dto.setCompanyId(company.getId());
            dto.setCompanyName(company.getName());
            dto.setCompanyCnpj(company.getCnpj());
        }

        if (unit != null) {
            dto.setUnitId(unit.getId());
            dto.setUnitName(unit.getName());
            dto.setUnitCnpj(unit.getCnpj());
        }

        if (sector != null) {
            dto.setSectorId(sector.getId());
            dto.setSectorName(sector.getName());
        }
    }

    private String formatShift(String raw) {
        if (raw.equalsIgnoreCase("MANHA") || raw.equalsIgnoreCase("MORNING")) return "Manhã";
        if (raw.equalsIgnoreCase("TARDE") || raw.equalsIgnoreCase("AFTERNOON")) return "Tarde";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    private String formatEventType(String raw) {
        return switch (raw) {
            case "VISITA_TECNICA" -> "Visita Técnica";
            case "PERICIA" -> "Perícia";
            case "GESTAO", "GESTÃO" -> "Gestão";
            default -> raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase().replace("_", " ");
        };
    }
}