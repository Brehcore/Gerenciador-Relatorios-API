package com.gotree.API.services;

import com.gotree.API.dto.client.ClientFirstAccessRequestDTO;
import com.gotree.API.dto.client.ClientSetupPasswordDTO;
import com.gotree.API.entities.AgendaEvent;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.Company;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.repositories.AgendaEventRepository;
import com.gotree.API.repositories.ClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ClientPortalService {

    private final ClientRepository clientRepository;
    private final AgendaEventRepository agendaEventRepository;
    private final PasswordEncoder passwordEncoder;
    // Injetar aqui um serviço de envio de email (JavaMailSender)

    public ClientPortalService(ClientRepository clientRepository,
                               AgendaEventRepository agendaEventRepository,
                               PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.agendaEventRepository = agendaEventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Passo 1: Solicitar primeiro acesso
    @Transactional
    public void requestFirstAccess(ClientFirstAccessRequestDTO dto) {
        Client client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("E-mail não encontrado na base de clientes."));

        // Gera um código simples de 6 caracteres (pode usar UUID se preferir)
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        client.setAccessCode(code);
        client.setAccessCodeExpiration(LocalDateTime.now().plusMinutes(15)); // Validade de 15 min

        clientRepository.save(client);

        // TODO: Enviar e-mail real para o cliente com o código
        System.out.println("CÓDIGO DE ACESSO PARA " + dto.getEmail() + ": " + code);
    }

    // Passo 2: Validar código e criar senha
    @Transactional
    public void setupPassword(ClientSetupPasswordDTO dto) {
        Client client = clientRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        if (client.getAccessCode() == null ||
                !client.getAccessCode().equals(dto.getAccessCode()) ||
                client.getAccessCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código inválido ou expirado.");
        }

        // Define a senha criptografada
        client.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        // Limpa o código para não ser usado novamente
        client.setAccessCode(null);
        client.setAccessCodeExpiration(null);

        clientRepository.save(client);
    }

    // Funcionalidade: Buscar agenda do cliente logado
    public List<AgendaEvent> getClientAgenda(String clientEmail) {
        // 1. Busca o cliente e suas empresas
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));

        // 2. Extrai os IDs das empresas vinculadas a este cliente
        List<Long> companyIds = client.getCompanies().stream()
                .map(Company::getId)
                .toList();

        if (companyIds.isEmpty()) {
            return List.of();
        }

        // 3. Busca os eventos usando a Query ajustada
        return agendaEventRepository.findByClientCompanyIds(companyIds);
    }
}