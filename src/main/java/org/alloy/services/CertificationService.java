package org.alloy.services;

import org.alloy.models.entities.Certification;
import org.alloy.models.entities.Welder;
import org.alloy.repositories.CertificationRepository;
import org.alloy.repositories.WelderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CertificationService {

    @Autowired
    private CertificationRepository certificationRepository;

    @Autowired
    private WelderRepository welderRepository;

    public List<Certification> getAllCertifications() {
        return certificationRepository.findAll();
    }

    public Optional<Certification> getCertificationById(Long id) {
        return certificationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Certification> getCertificationsByWelderId(Long welderId) {
        List<Certification> certifications = certificationRepository.findByWelderId(welderId);
        // Принудительно инициализируем все коллекции в рамках транзакции
        // чтобы избежать проблем с ленивой загрузкой при сериализации
        for (Certification cert : certifications) {
            // Инициализируем коллекции, вызывая size() или другие методы
            cert.getWeldingMethods().size();
            cert.getTechGroups().size();
            cert.getPositions().size();
            cert.getConnections().size();
            cert.getMaterials().size();
            cert.getParts().size();
            cert.getWeldTypes().size();
        }
        return certifications;
    }

    public Certification createCertification(Long welderId, Certification certification) {
        Optional<Welder> welderOptional = welderRepository.findById(welderId);
        if (!welderOptional.isPresent()) {
            throw new RuntimeException("Welder not found with id: " + welderId);
        }

        Welder welder = welderOptional.get();
        certification.setWelder(welder);

        // Автоматически определяем статус на основе даты окончания
        if (certification.getExpiryDate() != null) {
            if (certification.getExpiryDate().isBefore(LocalDate.now())) {
                certification.setStatus(Certification.CertificationStatus.EXPIRED);
            } else {
                certification.setStatus(Certification.CertificationStatus.ACTIVE);
            }
        }

        return certificationRepository.save(certification);
    }

    public Certification updateCertification(Long id, Certification certificationDetails) {
        Optional<Certification> optionalCertification = certificationRepository.findById(id);
        if (optionalCertification.isPresent()) {
            Certification certification = optionalCertification.get();

            certification.setCertificateNumber(certificationDetails.getCertificateNumber());
            certification.setCertificationDate(certificationDetails.getCertificationDate());
            certification.setExpiryDate(certificationDetails.getExpiryDate());
            certification.setWeldingMethods(certificationDetails.getWeldingMethods());
            certification.setTechGroups(certificationDetails.getTechGroups());
            certification.setPositions(certificationDetails.getPositions());
            certification.setConnections(certificationDetails.getConnections());
            certification.setMaterials(certificationDetails.getMaterials());
            certification.setParts(certificationDetails.getParts());
            certification.setWeldTypes(certificationDetails.getWeldTypes());
            certification.setThicknessFrom(certificationDetails.getThicknessFrom());
            certification.setThicknessTo(certificationDetails.getThicknessTo());
            certification.setDiameterFrom(certificationDetails.getDiameterFrom());
            certification.setDiameterTo(certificationDetails.getDiameterTo());

            // Обновляем статус на основе даты окончания
            if (certification.getExpiryDate() != null) {
                if (certification.getExpiryDate().isBefore(LocalDate.now())) {
                    certification.setStatus(Certification.CertificationStatus.EXPIRED);
                } else {
                    certification.setStatus(Certification.CertificationStatus.ACTIVE);
                }
            }

            return certificationRepository.save(certification);
        }
        return null;
    }

    public boolean deleteCertification(Long id) {
        Optional<Certification> optionalCertification = certificationRepository.findById(id);
        if (optionalCertification.isPresent()) {
            certificationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void deleteCertificationsByWelderId(Long welderId) {
        certificationRepository.deleteByWelderId(welderId);
    }
}

