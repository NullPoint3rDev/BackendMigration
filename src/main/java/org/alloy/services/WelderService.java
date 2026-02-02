package org.alloy.services;

import org.alloy.models.dto.WelderDTO;
import org.alloy.models.entities.RfidPass;
import org.alloy.models.entities.Welder;
import org.alloy.models.entities.WeldingMachine;
import org.alloy.repositories.RfidPassRepository;
import org.alloy.repositories.WelderRepository;
import org.alloy.repositories.WeldingMachineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    private static final String UPLOAD_DIR = "uploads/welders";

    public List<Welder> getAllWelders() {
        return welderRepository.findAll();
    }

    public Optional<Welder> getWelderById(Long id) {
        Optional<Welder> welder = welderRepository.findById(id);
        if (welder.isPresent()) {
            Welder w = welder.get();
            // Загружаем RFID пропуска
            List<RfidPass> rfidPasses = rfidPassRepository.findByWelderId(id);
            w.setRfidPasses(rfidPasses);
            // Загружаем связанные аппараты (инициализируем коллекцию)
            if (w.getWeldingMachines() != null) {
                w.getWeldingMachines().size(); // Инициализация lazy коллекции
            }
        }
        return welder;
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

            // Удаляем старые RFID пропуска
            List<RfidPass> existingPasses = rfidPassRepository.findByWelderId(id);
            if (!existingPasses.isEmpty()) {
                rfidPassRepository.deleteAll(existingPasses);
            }

            // Сохраняем новые RFID пропуска
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

    public Welder getWelderByRfidCode(String rfidCode) {
        return welderRepository.findByRfidCode(rfidCode);
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
}
