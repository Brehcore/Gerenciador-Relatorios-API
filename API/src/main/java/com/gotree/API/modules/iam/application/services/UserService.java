package com.gotree.API.modules.iam.application.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.gotree.API.modules.iam.infrastructure.security.ClientUserDetails;
import com.gotree.API.modules.iam.infrastructure.security.CustomUserDetails;
import com.gotree.API.modules.customer.infrastructure.repositories.ClientRepository;
import com.gotree.API.modules.customer.domain.entities.Client;
import com.gotree.API.modules.iam.presentation.dto.CertificateUploadDTO;
import com.gotree.API.modules.iam.presentation.dto.UserRequestDTO;
import com.gotree.API.modules.iam.presentation.dto.UserUpdateDTO;
import com.gotree.API.modules.iam.domain.entities.User;
import com.gotree.API.modules.shared.exceptions.CpfValidationException;
import com.gotree.API.modules.shared.exceptions.ResourceNotFoundException;
import com.gotree.API.modules.iam.presentation.mappers.UserMapper;
import com.gotree.API.modules.iam.infrastructure.repositories.AccessProfileRepository;
import com.gotree.API.modules.iam.infrastructure.repositories.UserRepository;
import com.gotree.API.modules.operations.infrastructure.repositories.AepReportRepository;
import com.gotree.API.modules.operations.infrastructure.repositories.OccupationalRiskReportRepository;
import com.gotree.API.modules.operations.infrastructure.repositories.TechnicalVisitRepository;
import com.gotree.API.modules.shared.infrastructure.providers.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AepReportRepository aepReportRepository;
    private final OccupationalRiskReportRepository riskReportRepository;
    private final TechnicalVisitRepository technicalVisitRepository;
    private final ClientRepository clientRepository;
    private final SymmetricCryptoService cryptoService;
    private final EmailService emailService;
    private final AccessProfileRepository accessProfileRepository;
    private final CpfValidatorService cpfValidatorService;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
                       AepReportRepository aepReportRepository, OccupationalRiskReportRepository riskReportRepository,
                       TechnicalVisitRepository technicalVisitRepository, ClientRepository clientRepository,
                       SymmetricCryptoService cryptoService, AccessProfileRepository accessProfileRepository,
                       CpfValidatorService cpfValidatorService, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.aepReportRepository = aepReportRepository;
        this.riskReportRepository = riskReportRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.clientRepository = clientRepository;
        this.cryptoService = cryptoService;
        this.accessProfileRepository = accessProfileRepository;
        this.cpfValidatorService = cpfValidatorService;
        this.emailService = emailService;
    }

    public List<User> findAll() { return userRepository.findAll(); }
    public Page<User> findAll(Pageable pageable) { return userRepository.findAll(pageable); }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + id + " não encontrado."));
    }

    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }

    public User insertUser(UserRequestDTO dto) {
        validateUser(dto);
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (dto.getAccessProfileId() != null) {
            var profile = accessProfileRepository.findById(dto.getAccessProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado."));
            user.setProfile(profile);
        }

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserUpdateDTO dto) {
        User user = findById(id);

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getSiglaConselhoClasse() != null) user.setSiglaConselhoClasse(dto.getSiglaConselhoClasse());
        if (dto.getConselhoClasse() != null) user.setConselhoClasse(dto.getConselhoClasse());
        if (dto.getEspecialidade() != null) user.setEspecialidade(dto.getEspecialidade());

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
                throw new DataIntegrityViolationException("Email já cadastrado: " + u.getEmail());
            });
            user.setEmail(dto.getEmail());
        }

        if (dto.getCpf() != null) {
            String cleanCpf = dto.getCpf().replaceAll("\\D", "");
            try {
                cpfValidatorService.validateCpf(cleanCpf);
            } catch (IllegalArgumentException e) { // CORREÇÃO: Apanhar IllegalArgumentException
                throw new CpfValidationException("CPF inválido: " + dto.getCpf());
            }
            user.setCpf(dto.getCpf());
        }

        if (dto.getAccessProfileId() != null) {
            var profile = accessProfileRepository.findById(dto.getAccessProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado."));
            user.setProfile(profile);
        } else {
            user.setProfile(null); // Caso queira permitir remover o perfil
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        if (technicalVisitRepository.existsByTechnician_Id(id) ||
                riskReportRepository.existsByTechnician_Id(id) ||
                aepReportRepository.existsByEvaluator_Id(id)) {
            throw new IllegalStateException("Usuário não pode ser excluído pois possui relatórios vinculados.");
        }
        // Remove certificado físico se existir
        if (user.getCertificatePath() != null) {
            try { Files.deleteIfExists(Paths.get(user.getCertificatePath())); } catch (IOException ignored) {}
        }
        userRepository.deleteById(id);
    }

    public void resetPassword(Long userId) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(user.getEmail()));
        user.setPasswordResetRequired(true);
        userRepository.save(user);
    }

    /**
     * Completa o fluxo de redefinição de senha exigido pelo sistema.
     * Este metodo só permite a alteração se a flag passwordResetRequired estiver ativada.
     */
    @Transactional
    public void completePasswordReset(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        // Validação de segurança: Impede que a rota seja usada como "bypass" de senha
        // se o administrador não tiver forçado o reset previamente.
        if (user.getPasswordResetRequired() == null || !user.getPasswordResetRequired()) {
            throw new IllegalStateException("Nenhuma redefinição de senha pendente para este usuário.");
        }

        // Codifica a nova senha, aplica no usuário e desativa a flag de reset
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetRequired(false); // Libera o usuário para usar o sistema

        userRepository.save(user);
    }

    public void validateUser(UserRequestDTO userDTO) {
        userRepository.findByEmail(userDTO.getEmail()).ifPresent(u -> {
            throw new DataIntegrityViolationException("Email já cadastrado.");
        });

        if (userDTO.getCpf() != null) {
            String cleanCpf = userDTO.getCpf().replaceAll("\\D", "");
            try {
                cpfValidatorService.validateCpf(cleanCpf);
            } catch (IllegalArgumentException e) { // CORREÇÃO: Apanhar IllegalArgumentException
                throw new CpfValidationException("CPF inválido.");
            }
        }
    }

    @Transactional
    public void changePassword(String userEmail, String newPassword, String currentPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if(!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("A senha atual informada está incorreta.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetRequired(false);
        userRepository.save(user);
    }

    @Transactional
    public void changeEmail(String currentEmail, String newEmail, String currentPassword) {
       User user = userRepository.findByEmail(currentEmail).
               orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
       if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
           throw new IllegalArgumentException("A senha atual informada está incorreta.");
       }
       // Verifica se o novo e-mail já não pertence a outra pessoa
       Optional<User> userWithEmail = userRepository.findByEmail(newEmail);
       // Se encontrou alguém e esse alguém não é o mesmo usuário, lança a exceção
       if(userWithEmail.isPresent()) {
           throw new DataIntegrityViolationException("Este e-mail já está em uso");
       }
       user.setEmail(newEmail);
       userRepository.save(user);
    }

    /**
     * Inicia o fluxo de recuperação de senha.
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        // 1. Gera um token alfanumérico de 6 posições
        String token = generateAlphanumericToken(6);

        // 2. Salva o token no usuário
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        // 3. Monta o HTML pequeno e simples direto no código
        String emailBody = buildForgotPasswordHtml(user.getName(), token);

        // 4. Dispara o e-mail (Ajuste a chamada de acordo com o seu EmailService)
        emailService.sendHtmlEmail(user.getEmail(), "Recuperação de Senha - Go-Tree", emailBody);
    }

    @Transactional
    public void resetPasswordWithToken(String email, String token, String newPassword) {
        // 1. Procura o utilizador
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        // 2. Valida se existe um token e se ele coincide (Ignorando espaços e maiúsculas)
        if (user.getResetToken() == null || !user.getResetToken().equalsIgnoreCase(token.trim())) {
            throw new IllegalArgumentException("Código de recuperação inválido.");
        }

        // 3. Valida a expiração (LocalDateTime do Java comparado com o TIMESTAMP do banco)
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("O código de recuperação expirou. Solicite um novo e-mail.");
        }

        // 4. Sucesso! Encripta a nova senha e limpa os campos de segurança
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setPasswordResetRequired(false); // Garante que o utilizador possa logar direto

        userRepository.save(user);
    }

    /**
     * Gera o template HTML inline injetando o nome e o token.
     */
    private String buildForgotPasswordHtml(String name, String token) {
        // O .formatted() injeta o nome no primeiro %s e o token no segundo %s
        return """
            <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #eaeaea; border-radius: 8px; color: #333;">
                <h2 style="color: #005A32; text-align: center; border-bottom: 2px solid #BFD83A; padding-bottom: 10px;">Go-Tree Consultoria</h2>
                <p>Olá, <strong>%s</strong>!</p>
                <p>Recebemos um pedido para redefinir sua senha. Utilize o código de segurança abaixo no aplicativo:</p>
                
                <div style="text-align: center; margin: 25px 0;">
                    <span style="font-size: 28px; font-family: monospace; font-weight: bold; background: #f8f9fa; padding: 12px 24px; border-radius: 6px; border: 1px dashed #005A32; letter-spacing: 4px; color: #005A32;">
                        %s
                    </span>
                </div>
                
                <p style="font-size: 14px; color: #666; text-align: center;">Este código é válido por <strong>30 minutos</strong>.</p>
                <p style="font-size: 12px; color: #999; text-align: center; margin-top: 30px;">Se você não solicitou esta alteração, apenas ignore este e-mail.</p>
            </div>
            """.formatted(name, token);
    }

    /**
     * Utilitário para gerar tokens aleatórios e seguros.
     */
    private String generateAlphanumericToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void uploadCertificate(String userEmail, CertificateUploadDTO dto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (dto.getFile() == null || dto.getFile().isEmpty()) {
            throw new IllegalArgumentException("Arquivo do certificado é obrigatório.");
        }

        // 1. BARREIRA DE SEGURANÇA: Controle rígido do tamanho da senha
        // Impede ataques de exaustão de CPU (DoS) e satisfaz o analisador estático
        String rawPassword = dto.getPassword();
        if (rawPassword == null || rawPassword.isEmpty() || rawPassword.length() > 128) {
            throw new IllegalArgumentException("A senha do certificado é inválida ou excede o tamanho máximo.");
        }

        java.util.Date validity = null;

        // 2. Tenta abrir o arquivo PFX, validar senha e extrair data de validade
        try (java.io.InputStream is = dto.getFile().getInputStream()) {
            java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");

            ks.load(is, rawPassword.toCharArray());

            // Pega a data do primeiro certificado da cadeia
            java.util.Enumeration<String> aliases = ks.aliases();
            if (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                java.security.cert.Certificate cert = ks.getCertificate(alias);

                // Verifica se é X509Certificate para pegar a data
                if (cert instanceof java.security.cert.X509Certificate x509Cert) {
                    validity = x509Cert.getNotAfter();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Senha incorreta ou arquivo PFX inválido.");
        }

        // 3. Limpeza: Remove arquivo antigo se já existir
        if (user.getCertificatePath() != null) {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(user.getCertificatePath())); } catch (java.io.IOException ignored) {}
        }

        // 4. Salva novo arquivo e atualiza dados no banco
        try {
            String safeExt = getSafeExtension(dto.getFile().getOriginalFilename());

            String fileName = "CERT_" + user.getId() + "_" + java.util.UUID.randomUUID() + safeExt;
            java.nio.file.Path targetPath = java.nio.file.Paths.get(fileStoragePath, "certificates", fileName);

            java.nio.file.Files.createDirectories(targetPath.getParent());
            java.nio.file.Files.write(targetPath, dto.getFile().getBytes());

            user.setCertificatePath(targetPath.toString());

            // SEGURO: A variável 'rawPassword' já passou pelo IF de validação de tamanho.
            // A IDE agora sabe que o input está "Controlado".
            user.setCertificatePassword(cryptoService.encrypt(rawPassword));

            if (validity != null) {
                user.setCertificateValidity(validity.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }

            userRepository.save(user);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo no disco.", e);
        }
    }

    /**
     * Valida e retorna a extensão segura do arquivo (Whitelist contra Path Traversal).
     */
    private String getSafeExtension(String originalName) {
        if (originalName == null) {
            return ".pfx"; // Valor padrão seguro
        }

        String lowerName = originalName.toLowerCase();
        if (lowerName.endsWith(".p12")) {
            return ".p12";
        }
        if (!lowerName.endsWith(".pfx")) {
            throw new IllegalArgumentException("Extensão de arquivo inválida. Utilize apenas .pfx ou .p12");
        }

        return ".pfx";
    }

    @Transactional
    public void removeCertificate(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (user.getCertificatePath() != null) {
            try { Files.deleteIfExists(Paths.get(user.getCertificatePath())); } catch (IOException ignored) {}
        }

        // Limpa campos
        user.setCertificatePath(null);
        user.setCertificatePassword(null);
        user.setCertificateValidity(null);
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) return new CustomUserDetails(user.get());

        Optional<Client> client = clientRepository.findByEmail(email);
        if (client.isPresent()) return new ClientUserDetails(client.get());

        throw new UsernameNotFoundException("Usuário não encontrado: " + email);
    }
}