package org.alloy.services;

import org.alloy.models.dto.WelderDTO;
import org.alloy.models.entities.RfidPass;
import org.alloy.models.entities.Welder;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.RfidPassRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WelderService {

    @Autowired
    private WelderRepository welderRepository;

    @Autowired
    private RfidPassRepository rfidPassRepository;

    @Autowired
    private WeldingMachineRepository weldingMachineRepository;

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private WeldingMachineLastPoweredOnService weldingMachineLastPoweredOnService;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    private static final String UPLOAD_DIR = "uploads/welders";

    /**
     * Уникальные должности сварщиков (для комбобокса на карточке сварщика).
     * Если задан organizationId — только по подразделениям этого предприятия.
     * ponytail: справочник должностей выводится из уже сохранённых сварщиков (вариант B);
     * при необходимости мгновенной видимости новой должности до сохранения — завести отдельную сущность.
     */
    public List<String> getDistinctPositions(List<Welder> welders, Integer organizationId) {
        final java.util.Set<String> allowedDepts;
        if (organizationId != null) {
            allowedDepts = organizationUnitRepository.findByOrganizationId(organizationId).stream()
                    .map(u -> u.getName() == null ? "" : u.getName().trim().toLowerCase(Locale.ROOT))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } else {
            allowedDepts = null;
        }
        return welders.stream()
                .filter(w -> allowedDepts == null
                        || (w.getDepartment() != null
                        && allowedDepts.contains(w.getDepartment().trim().toLowerCase(Locale.ROOT))))
                .map(Welder::getPosition)
                .filter(p -> p != null && !p.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /** Маска 64 бит — типичный размер RFID в hex; снимаем расхождения ведущих нулей между аппаратом и БД. */
    private static final BigInteger RFID_HEX_MASK_64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    /**
     * Варианты строки RFID для поиска: исходная, регистр, канонический 16 hex (младшие 64 бит).
     */
    private static List<String> expandRfidCodeCandidates(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(trimmed);
        String compact = trimmed.replaceAll("\\s+", "");
        out.add(compact);
        String upper = compact.toUpperCase(Locale.ROOT);
        out.add(upper);
        out.add(upper.toLowerCase(Locale.ROOT));
        if (upper.matches("[0-9A-F]+")) {
            try {
                String hexForParse = upper.length() % 2 != 0 ? "0" + upper : upper;
                BigInteger bi = new BigInteger(hexForParse, 16);
                String stripped = bi.toString(16).toUpperCase(Locale.ROOT);
                out.add(stripped);
                out.add(stripped.toLowerCase(Locale.ROOT));
                BigInteger low64 = bi.and(RFID_HEX_MASK_64);
                String canon16 = String.format(Locale.ROOT, "%016X", low64);
                out.add(canon16);
                out.add(canon16.toLowerCase(Locale.ROOT));
                for (int hexDigits = 16; hexDigits <= 32; hexDigits += 8) {
                    if (bi.bitLength() <= hexDigits * 4) {
                        String padded = String.format(Locale.ROOT, "%0" + hexDigits + "X", bi);
                        out.add(padded);
                        out.add(padded.toLowerCase(Locale.ROOT));
                    }
                }
            } catch (Exception ignored) {
                // оставляем только строковые варианты
            }
        }
        return new ArrayList<>(out);
    }

    public List<Welder> getAllWelders() {
        return welderRepository.findAll();
    }

    public Optional<Welder> getWelderById(Long id) {
        Optional<Welder> welder = welderRepository.findById(id);
        if (welder.isPresent()) {
            Welder w = welder.get();
            // Загружаем RFID пропуска (не заменяем коллекцию — иначе Hibernate orphanRemoval)
            List<RfidPass> rfidPasses = rfidPassRepository.findByWelderId(id);
            if (w.getRfidPasses() == null) {
                w.setRfidPasses(new java.util.ArrayList<>());
            } else {
                w.getRfidPasses().clear();
            }
            if (rfidPasses != null && !rfidPasses.isEmpty()) {
                w.getRfidPasses().addAll(rfidPasses);
            }
            enrichLastPoweredOnForDisplay(w);
        }
        return welder;
    }

    private void enrichLastPoweredOnForDisplay(Welder welder) {
        if (welder.getWeldingMachines() == null || welder.getWeldingMachines().isEmpty()) {
            return;
        }
        welder.getWeldingMachines().size();
        for (WeldingMachine machine : welder.getWeldingMachines()) {
            if (machine == null || machine.getId() == null || machine.getLastPoweredOnAt() != null) {
                continue;
            }
            LocalDateTime fromHistory = weldingMachineLastPoweredOnService.resolveForDisplay(machine.getId());
            if (fromHistory != null) {
                machine.setLastPoweredOnAt(fromHistory);
            }
        }
    }

    public Welder createWelder(Welder welder) {
        Welder savedWelder = welderRepository.save(welder);
        return savedWelder;
    }

    public Welder createWelder(WelderDTO welderDTO) {
        Welder welder = new Welder();
        welder.setName(welderDTO.getName());
        welder.setStatus(welderDTO.getStatus());
        welder.setDepartment(welderDTO.getDepartment());
        welder.setPosition(welderDTO.getPosition());
        welder.setGrade(welderDTO.getGrade());
        welder.setEmployeeId(welderDTO.getEmployeeId());
        welder.setHireDate(welderDTO.getHireDate());
        welder.setBirthDate(welderDTO.getBirthDate());
        welder.setCertificationDate(welderDTO.getCertificationDate());
        welder.setNextCertificationDate(welderDTO.getNextCertificationDate());
        welder.setPhone(welderDTO.getPhone());
        welder.setAddress(welderDTO.getAddress());
        welder.setEducation(welderDTO.getEducation());
        welder.setEmail(welderDTO.getEmail());
        welder.setNotes(welderDTO.getNotes());
        welder.setPhoto(welderDTO.getPhoto());

        Welder savedWelder = welderRepository.save(welder);

        // Сохраняем RFID пропуска
        if (welderDTO.getRfidCodes() != null && !welderDTO.getRfidCodes().isEmpty()) {
            List<RfidPass> rfidPasses = new ArrayList<>();
            for (String code : welderDTO.getRfidCodes()) {
                if (code != null && !code.trim().isEmpty()) {
                    RfidPass rfidPass = new RfidPass();
                    rfidPass.setCode(code.trim());
                    rfidPass.setWelder(savedWelder);
                    rfidPasses.add(rfidPass);
                }
            }
            if (!rfidPasses.isEmpty()) {
                rfidPassRepository.saveAll(rfidPasses);
            }
        }

        // Сохраняем связанные аппараты
        if (welderDTO.getMachineIds() != null && !welderDTO.getMachineIds().isEmpty()) {
            List<WeldingMachine> machines = new ArrayList<>();
            for (Integer machineId : welderDTO.getMachineIds()) {
                Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                if (machineOpt.isPresent()) {
                    machines.add(machineOpt.get());
                }
            }
            savedWelder.setWeldingMachines(machines);
            savedWelder = welderRepository.save(savedWelder);
        }

        return savedWelder;
    }

    public Welder updateWelder(Long id, Welder welderDetails) {
        Optional<Welder> optionalWelder = welderRepository.findById(id);
        if (optionalWelder.isPresent()) {
            Welder welder = optionalWelder.get();
            welder.setName(welderDetails.getName());
            welder.setStatus(welderDetails.getStatus());
            welder.setDepartment(welderDetails.getDepartment());
            welder.setPosition(welderDetails.getPosition());
            welder.setGrade(welderDetails.getGrade());
            welder.setEmployeeId(welderDetails.getEmployeeId());
            welder.setHireDate(welderDetails.getHireDate());
            welder.setBirthDate(welderDetails.getBirthDate());
            welder.setCertificationDate(welderDetails.getCertificationDate());
            welder.setNextCertificationDate(welderDetails.getNextCertificationDate());
            welder.setPhone(welderDetails.getPhone());
            welder.setAddress(welderDetails.getAddress());
            welder.setRfidCode(welderDetails.getRfidCode());
            welder.setEducation(welderDetails.getEducation());
            welder.setEmail(welderDetails.getEmail());
            welder.setNotes(welderDetails.getNotes());
            if (welderDetails.getPhoto() != null) {
                welder.setPhoto(welderDetails.getPhoto());
            }
            return welderRepository.save(welder);
        }
        return null;
    }

    public Welder updateWelder(Long id, WelderDTO welderDTO) {
        Optional<Welder> optionalWelder = welderRepository.findById(id);
        if (optionalWelder.isPresent()) {
            Welder welder = optionalWelder.get();
            welder.setName(welderDTO.getName());
            welder.setStatus(welderDTO.getStatus());
            welder.setDepartment(welderDTO.getDepartment());
            welder.setPosition(welderDTO.getPosition());
            welder.setGrade(welderDTO.getGrade());
            welder.setEmployeeId(welderDTO.getEmployeeId());
            welder.setHireDate(welderDTO.getHireDate());
            welder.setBirthDate(welderDTO.getBirthDate());
            welder.setCertificationDate(welderDTO.getCertificationDate());
            welder.setNextCertificationDate(welderDTO.getNextCertificationDate());
            welder.setPhone(welderDTO.getPhone());
            welder.setAddress(welderDTO.getAddress());
            welder.setEducation(welderDTO.getEducation());
            welder.setEmail(welderDTO.getEmail());
            welder.setNotes(welderDTO.getNotes());
            if (welderDTO.getPhoto() != null) {
                welder.setPhoto(welderDTO.getPhoto());
            }

            Welder savedWelder = welderRepository.save(welder);

            // RFID обновляем только если поле передано в DTO (null — не трогать пропуска)
            if (welderDTO.getRfidCodes() != null) {
                List<RfidPass> existingPasses = rfidPassRepository.findByWelderId(id);
                if (!existingPasses.isEmpty()) {
                    rfidPassRepository.deleteAll(existingPasses);
                }
                if (!welderDTO.getRfidCodes().isEmpty()) {
                    List<RfidPass> rfidPasses = new ArrayList<>();
                    for (String code : welderDTO.getRfidCodes()) {
                        if (code != null && !code.trim().isEmpty()) {
                            RfidPass rfidPass = new RfidPass();
                            rfidPass.setCode(code.trim());
                            rfidPass.setWelder(savedWelder);
                            rfidPasses.add(rfidPass);
                        }
                    }
                    if (!rfidPasses.isEmpty()) {
                        rfidPassRepository.saveAll(rfidPasses);
                    }
                }
            }

            // Обновляем связанные аппараты
            if (welderDTO.getMachineIds() != null) {
                List<WeldingMachine> machines = new ArrayList<>();
                if (!welderDTO.getMachineIds().isEmpty()) {
                    for (Integer machineId : welderDTO.getMachineIds()) {
                        Optional<WeldingMachine> machineOpt = weldingMachineRepository.findById(machineId);
                        if (machineOpt.isPresent()) {
                            machines.add(machineOpt.get());
                        }
                    }
                }
                savedWelder.setWeldingMachines(machines);
                savedWelder = welderRepository.save(savedWelder);
            }

            return savedWelder;
        }
        return null;
    }

    public boolean isRfidCodeAvailable(String rfidCode, String department, Long excludeWelderId) {
        List<RfidPass> existingPasses;
        if (excludeWelderId != null) {
            existingPasses = rfidPassRepository.findByCodeAndDepartmentExcludingWelder(rfidCode, department, excludeWelderId);
        } else {
            existingPasses = rfidPassRepository.findByCodeAndDepartment(rfidCode, department);
        }
        return existingPasses.isEmpty();
    }

    public boolean deleteWelder(Long id) {
        Optional<Welder> optionalWelder = welderRepository.findById(id);
        if (optionalWelder.isPresent()) {
            // Сначала удаляем все аттестации сварщика
            certificationService.deleteCertificationsByWelderId(id);
            // Затем удаляем самого сварщика
            welderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Welder> getWeldersByStatus(Welder.WelderStatus status) {
        return welderRepository.findByStatus(status);
    }

    public List<Welder> getWeldersByDepartment(String department) {
        return welderRepository.findByDepartmentContainingIgnoreCase(department);
    }

    public List<Welder> getWeldersByName(String name) {
        return welderRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Welder> getWeldersByGrade(String grade) {
        return welderRepository.findByGrade(grade);
    }

    /**
     * Поиск сварщика по RFID: поле {@code Welders.rfid_code} (legacy) и таблица {@code rfid_passes} (актуальные пропуска).
     */
    @Transactional(readOnly = true)
    public Welder getWelderByRfidCode(String rfidCode) {
        if (rfidCode == null || rfidCode.trim().isEmpty()) {
            return null;
        }
        for (String candidate : expandRfidCodeCandidates(rfidCode)) {
            List<Welder> byJoin = welderRepository.findAllByRfidCodeOrPass(candidate);
            if (byJoin != null && !byJoin.isEmpty()) {
                return byJoin.get(0);
            }
            Welder byLegacy = welderRepository.findByRfidCode(candidate);
            if (byLegacy != null) {
                return byLegacy;
            }
            List<RfidPass> passes = rfidPassRepository.findAllByCode(candidate);
            if (passes != null && !passes.isEmpty()) {
                Welder w = passes.get(0).getWelder();
                if (w != null) {
                    return w;
                }
            }
        }
        return null;
    }

    public Welder getWelderByEmployeeId(String employeeId) {
        return welderRepository.findByEmployeeId(employeeId);
    }

    public List<Welder> getWeldersByFilters(String name, Welder.WelderStatus status, String department, String grade) {
        return welderRepository.findByFilters(name, status, department, grade);
    }

    public String uploadWelderPhoto(Long welderId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Optional<Welder> optionalWelder = welderRepository.findById(welderId);
        if (!optionalWelder.isPresent()) {
            throw new RuntimeException("Welder not found");
        }

        Welder welder = optionalWelder.get();

        // Создаем директорию, если она не существует
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename is null");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = "welder_" + welderId + "_" + UUID.randomUUID().toString() + extension;

        // Сохраняем файл
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Сохраняем путь к файлу в базе данных
        String photoPath = UPLOAD_DIR + "/" + filename;
        welder.setPhoto(photoPath);
        welderRepository.save(welder);

        return photoPath;
    }

    public byte[] getWelderPhoto(String photoPath) throws IOException {
        if (photoPath == null || photoPath.isEmpty()) {
            throw new IllegalArgumentException("Photo path is empty");
        }

        Path filePath = Paths.get(photoPath);
        if (!Files.exists(filePath)) {
            throw new IOException("Photo file not found: " + photoPath);
        }

        return Files.readAllBytes(filePath);
    }

    public void deleteWelderPhoto(Long welderId) throws IOException {
        Welder welder = welderRepository.findById(welderId)
                .orElseThrow(() -> new RuntimeException("Welder not found"));

        String photoPath = welder.getPhoto();
        if (photoPath != null && !photoPath.isEmpty()) {
            Path filePath = Paths.get(photoPath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }

        welder.setPhoto(null);
        welderRepository.save(welder);
    }
}
