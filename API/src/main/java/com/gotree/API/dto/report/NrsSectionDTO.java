package com.gotree.API.dto.report;

import lombok.Data;
import java.util.List;

@Data
public class NrsSectionDTO {
    private String title;
    private List<NrsChecklistItemDTO> items;
}