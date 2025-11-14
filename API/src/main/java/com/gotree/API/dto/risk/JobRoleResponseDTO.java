package com.gotree.API.dto.risk;

import lombok.Data;

@Data
public class JobRoleResponseDTO {
    private Long id;
    private String name;
    private Long companyId;
    private String companyName;
}