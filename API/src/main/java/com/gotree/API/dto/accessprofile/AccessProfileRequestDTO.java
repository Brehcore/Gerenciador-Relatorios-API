package com.gotree.API.dto.accessprofile;

import com.gotree.API.enums.SystemPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AccessProfileRequestDTO {
    @NotBlank
    private String name; // "Supervisão"

    @NotEmpty
    private Set<SystemPermission> permissions; // ["VIEW_AGENDA", "VIEW_REPORTS"]
}
