package com.gotree.API.entities;

import com.gotree.API.enums.AgendaEventType;
import com.gotree.API.enums.AgendaStatus;
import com.gotree.API.enums.Shift;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Entidade que representa eventos de agenda no sistema.
 * Esta classe é usada para registrar eventos relacionados a visitas técnicas,
 * como reagendamentos e cancelamentos.
 */
@Entity
@Table(name = "tb_agenda_event")
@Data
public class AgendaEvent {


    /**
     * Identificador único do evento de agenda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Título do evento de agenda.
     */
    private String title;

    /**
     * Descrição detalhada do evento.
     * Pode conter informações como motivo de reagendamento ou cancelamento.
     */
    @Lob
    private String description;

    /**
     * Data em que o evento está programado para ocorrer.
     * No caso de reagendamentos, representa a nova data agendada.
     */
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift; // Turno Obrigatório

    /**
     * Usuário associado ao evento.
     * Relacionamento muitos-para-um com a entidade User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_visit_id", nullable = true)
    private TechnicalVisit technicalVisit;

    // Permite vincular evento à empresa mesmo sem visita
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /**
     * Data original da visita técnica.
     * Utilizado para manter histórico em caso de reagendamentos.
     */
    @Column(name = "original_visit_date")
    private LocalDate originalVisitDate;

    /**
     * Tipo do evento de agenda.
     * Define a natureza do evento (ex: reagendamento, cancelamento).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private AgendaEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendaStatus status = AgendaStatus.A_CONFIRMAR; // Padrão

    // Para onde esse evento foi jogado?
    // Só será preenchido se status == REAGENDADO
    @Column(name = "rescheduled_to_date")
    private LocalDate rescheduledToDate;

    // Serão usados quando o técnico marcar "REUNIAO" ou "OUTROS"
    private String clientName;
    private String manualObservation;
}