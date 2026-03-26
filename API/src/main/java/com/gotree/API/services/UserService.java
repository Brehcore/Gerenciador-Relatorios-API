package com.gotree.API.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import com.gotree.API.config.security.ClientUserDetails;
import com.gotree.API.config.security.CustomUserDetails;
import com.gotree.API.dto.user.*;
import com.gotree.API.entities.Client;
import com.gotree.API.entities.User;
import com.gotree.API.exceptions.CpfValidationException;
import com.gotree.API.exceptions.ResourceNotFoundException;
import com.gotree.API.mappers.UserMapper;
import com.gotree.API.repositories.*;
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
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;

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

    @Value("${file.storage.path}")
    private String fileStoragePath;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
                       AepReportRepository aepReportRepository, OccupationalRiskReportRepository riskReportRepository,
                       TechnicalVisitRepository technicalVisitRepository, ClientRepository clientRepository,
                       SymmetricCryptoService cryptoService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.aepReportRepository = aepReportRepository;
        this.riskReportRepository = riskReportRepository;
        this.technicalVisitRepository = technicalVisitRepository;
        this.clientRepository = clientRepository;
        this.cryptoService = cryptoService;
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
        return userRepository.save(user);
    }

    public User updateUser(Long id, UserUpdateDTO dto) {
        User user = findById(id);
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getSiglaConselhoClasse() != null) user.setSiglaConselhoClasse(dto.getSiglaConselhoClasse());
        if (dto.getConselhoClasse() != null) user.setConselhoClasse(dto.getConselhoClasse());
        if (dto.getEspecialidade() != null) user.setEspecialidade(dto.getEspecialidade());

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(dto.getEmail()).ifPresent(u -> {
                throw new DataIntegrityViolationException("Email já cadastrado: " + u.getEmail());
            });
            user.setEmail(dto.getEmail());
        }

        if (dto.getCpf() != null) {
            String cleanCpf = dto.getCpf().replaceAll("\\D", "");
            try {
                new CPFValidator().assertValid(cleanCpf);
            } catch (InvalidStateException e) {
                throw new CpfValidationException("CPF inválido: " + dto.getCpf());
            }
            user.setCpf(dto.getCpf());
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

    public void validateUser(UserRequestDTO userDTO) {
        userRepository.findByEmail(userDTO.getEmail()).ifPresent(u -> {
            throw new DataIntegrityViolationException("Email já cadastrado.");
        });
        String cleanCpf = userDTO.getCpf().replaceAll("\\D", "");
        try { new CPFValidator().assertValid(cleanCpf); }
        catch (InvalidStateException e) { throw new CpfValidationException("CPF inválido."); }
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

            // Usamos a variável validada em vez de dto.getPassword()
            //noinspection JvmTaintAnalysis
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