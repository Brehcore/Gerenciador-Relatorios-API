package com.gotree.API.dto.agenda;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateEventDTO {
    private String title;
    private String description;
    private LocalDate eventDate;
}