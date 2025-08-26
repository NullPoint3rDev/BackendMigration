package org.alloy.services;

import org.alloy.models.entities.Welder;
import org.alloy.repositories.WelderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WelderService {
    
    @Autowired
    private WelderRepository welderRepository;
    
    public List<Welder> getAllWelders() {
        return welderRepository.findAll();
    }
    
    public Optional<Welder> getWelderById(Long id) {
        return welderRepository.findById(id);
    }
    
    public Welder createWelder(Welder welder) {
        return welderRepository.save(welder);
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
            return welderRepository.save(welder);
        }
        return null;
    }
    
    public boolean deleteWelder(Long id) {
        Optional<Welder> optionalWelder = welderRepository.findById(id);
        if (optionalWelder.isPresent()) {
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
}
