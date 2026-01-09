package com.gotree.API.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientFirstAccessRequestDTO {

    @NotBlank @Email
    private String email;
}
