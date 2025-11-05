package com.gotree.API.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "tb_agenda_event")
@Data
public class AgendaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // Ex: "Integração - Empresa X"

    @Lob
    private String description; // Opcional

    private LocalDate eventDate; // A data do evento

    // A quem este evento pertence?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}