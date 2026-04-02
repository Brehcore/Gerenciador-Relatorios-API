package com.gotree.API.entities;

import com.gotree.API.enums.Shift;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_technical_visit")
@Data
public class TechnicalVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company clientCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    private User technician;

    @ManyToOne(fetch = FetchType.LAZY)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    private Sector sector;

    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;

    @Lob
    private String summary;

    private String pdfPath;

    @OneToMany(mappedBy = "technicalVisit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VisitFinding> findings = new ArrayList<>();

    // Campos de assinatura (reutilizando a lógica do InspectionReport)
    @Column(columnDefinition = "TEXT")
    private String technicianSignatureImageBase64;
    private LocalDateTime technicianSignedAt;

    private String clientSignerName;
    @Column(columnDefinition = "TEXT")
    private String clientSignatureImageBase64;
    private LocalDateTime clientSignedAt;
    private Double clientSignatureLatitude;
    private Double clientSignatureLongitude;

    // A entidade agenda armazenará os dados recebidos pelo DTO
//    @Column(name = "next_visit_date")
//    private LocalDate nextVisitDate;
//
//    @Enumerated(EnumType.STRING)
//    private Shift nextVisitShift;

    @Column(name = "sent_to_client_at")
    private java.time.LocalDateTime sentToClientAt;

    @Column(name = "icp_signed_at")
    private LocalDateTime icpSignedAt; // Data da assinatura PFX

    @Column(name = "is_draft", nullable = false)
    private boolean isDraft = true; // true = rascunho, false = relatório finalizado
}