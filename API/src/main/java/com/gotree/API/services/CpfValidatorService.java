package com.gotree.API.services;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import org.springframework.stereotype.Service;

@Service
public class CpfValidatorService {

    /**
     * Valida se um número de CPF é válido conforme as regras da Receita Federal.
     *
     * @param cpf O número do CPF a ser validado (pode conter pontuação)
     * @throws IllegalArgumentException se o CPF fornecido for inválido
     */
    public void validateCpf(String cpf) {
        CPFValidator validator = new CPFValidator();
        try {
            validator.assertValid(cpf);
        } catch (InvalidStateException e) {
            throw new IllegalArgumentException("CPF inválido: " + cpf, e);
        }
    }
}
