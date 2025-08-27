package org.alloy.services;

import org.alloy.models.entities.Employee;
import org.alloy.models.entities.OrganizationUnit;
import org.alloy.models.entities.UserRole;
import org.alloy.models.entities.UserAccount;
import org.alloy.models.dto.EmployeeDTO;
import org.alloy.models.dto.mapper.EmployeeMapper;
import org.alloy.repositories.EmployeeRepository;
import org.alloy.repositories.OrganizationUnitRepository;
import org.alloy.repositories.UserRoleRepository;
import org.alloy.repositories.UserAccountRepository;
import org.alloy.models.GeneralStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                          OrganizationUnitRepository organizationUnitRepository,
                          UserRoleRepository userRoleRepository,
                          UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.organizationUnitRepository = organizationUnitRepository;
        this.userRoleRepository = userRoleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public List<Employee> getEmployeesByStatus(GeneralStatus status) {
        return employeeRepository.findByStatus(status);
    }

    public List<Employee> getEmployeesByOrganizationUnit(Long organizationUnitId) {
        return employeeRepository.findByOrganizationUnitId(organizationUnitId);
    }

    public List<Employee> getEmployeesByUserRole(Long userRoleId) {
        return employeeRepository.findByUserRoleId(userRoleId);
    }

    public Optional<Employee> getEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username);
    }

    public List<Employee> searchEmployeesByFullName(String fullName) {
        return employeeRepository.findByFullNameContainingIgnoreCase(fullName);
    }

    public List<Employee> searchEmployeesByEmail(String email) {
        return employeeRepository.findByEmailContainingIgnoreCase(email);
    }

    public List<Employee> searchEmployeesByPosition(String position) {
        return employeeRepository.findByPositionContainingIgnoreCase(position);
    }

    public List<Employee> searchEmployees(String fullName, String email, String position,
                                        Long organizationUnitId, Long userRoleId, GeneralStatus status) {
        return employeeRepository.findByFilters(fullName, email, position, organizationUnitId, userRoleId, status);
    }

    @Transactional
    public Employee createEmployee(EmployeeDTO employeeDTO) {
        // Проверяем, что логин уникален в обеих таблицах
        Optional<Employee> existingEmployee = employeeRepository.findByUsername(employeeDTO.getUsername());
        if (existingEmployee.isPresent()) {
            throw new IllegalArgumentException("Сотрудник с логином '" + employeeDTO.getUsername() + "' уже существует");
        }
        
        Optional<UserAccount> existingUserAccount = userAccountRepository.findByUserName(employeeDTO.getUsername());
        if (existingUserAccount.isPresent()) {
            throw new IllegalArgumentException("Пользователь с логином '" + employeeDTO.getUsername() + "' уже существует в системе");
        }

        Employee employee = EmployeeMapper.toEntity(employeeDTO, passwordEncoder);

        // Устанавливаем подразделение
        if (employeeDTO.getOrganizationUnit() != null && employeeDTO.getOrganizationUnit().getId() != null) {
            Optional<OrganizationUnit> organizationUnit = organizationUnitRepository.findById(employeeDTO.getOrganizationUnit().getId());
            if (organizationUnit.isPresent()) {
                employee.setOrganizationUnit(organizationUnit.get());
            } else {
                throw new IllegalArgumentException("Подразделение с ID " + employeeDTO.getOrganizationUnit().getId() + " не найдено");
            }
        }

        // Устанавливаем роль пользователя
        if (employeeDTO.getUserRoleId() != null) {
            Optional<UserRole> userRole = userRoleRepository.findById(employeeDTO.getUserRoleId());
            if (userRole.isPresent()) {
                employee.setUserRole(userRole.get());
            } else {
                throw new IllegalArgumentException("Роль пользователя с ID " + employeeDTO.getUserRoleId() + " не найдена");
            }
        }

        // Сохраняем сотрудника
        Employee savedEmployee = employeeRepository.save(employee);
        System.out.println("Employee создан: " + savedEmployee.getUsername());

        // Создаем соответствующую запись в UserAccount для входа в систему
        UserAccount userAccount = new UserAccount();
        userAccount.setUserName(employeeDTO.getUsername());
        // Используем уже хешированный пароль из Employee
        userAccount.setPasswordHash(employee.getPassword().getBytes());
        userAccount.setName(employeeDTO.getFullName());
        userAccount.setEmail(employeeDTO.getEmail());
        userAccount.setPhone(employeeDTO.getPhone());
        userAccount.setPosition(employeeDTO.getPosition());
        userAccount.setOrganizationUnitId(employee.getOrganizationUnit() != null ? employee.getOrganizationUnit().getId().intValue() : null);
        userAccount.setUserRoleId(employee.getUserRole() != null ? employee.getUserRole().getId() : null);
        userAccount.setStatus(employeeDTO.getStatus());
        userAccount.setFailedLoginsCount(0);
        // userAccount.setPhoto(employeeDTO.getPhoto()); // Пропускаем фото, так как типы не совпадают
        
        UserAccount savedUserAccount = userAccountRepository.save(userAccount);
        System.out.println("UserAccount создан: " + savedUserAccount.getUserName());

        return savedEmployee;
    }

    @Transactional
    public Employee updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Optional<Employee> existingEmployee = employeeRepository.findById(id);
        if (!existingEmployee.isPresent()) {
            throw new IllegalArgumentException("Сотрудник с ID " + id + " не найден");
        }

        Employee employee = existingEmployee.get();

        // Проверяем уникальность логина (если изменился)
        if (!employee.getUsername().equals(employeeDTO.getUsername())) {
            Optional<Employee> employeeWithSameUsername = employeeRepository.findByUsername(employeeDTO.getUsername());
            if (employeeWithSameUsername.isPresent()) {
                throw new IllegalArgumentException("Сотрудник с логином '" + employeeDTO.getUsername() + "' уже существует");
            }
        }

        // Обновляем поля
        employee.setUsername(employeeDTO.getUsername());
        employee.setFullName(employeeDTO.getFullName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setPosition(employeeDTO.getPosition());
        employee.setPhone(employeeDTO.getPhone());
        employee.setPhoto(employeeDTO.getPhoto());
        employee.setStatus(employeeDTO.getStatus());

        // Обновляем пароль только если он предоставлен
        if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        }

        // Обновляем подразделение
        if (employeeDTO.getOrganizationUnit() != null && employeeDTO.getOrganizationUnit().getId() != null) {
            Optional<OrganizationUnit> organizationUnit = organizationUnitRepository.findById(employeeDTO.getOrganizationUnit().getId());
            if (organizationUnit.isPresent()) {
                employee.setOrganizationUnit(organizationUnit.get());
            } else {
                throw new IllegalArgumentException("Подразделение с ID " + employeeDTO.getOrganizationUnit().getId() + " не найдено");
            }
        }

        // Обновляем роль пользователя
        if (employeeDTO.getUserRoleId() != null) {
            Optional<UserRole> userRole = userRoleRepository.findById(employeeDTO.getUserRoleId());
            if (userRole.isPresent()) {
                employee.setUserRole(userRole.get());
            } else {
                throw new IllegalArgumentException("Роль пользователя с ID " + employeeDTO.getUserRoleId() + " не найдена");
            }
        }

        return employeeRepository.save(employee);
    }

    @Transactional
    public boolean deleteEmployee(Long id) {
        Optional<Employee> employee = employeeRepository.findById(id);
        if (employee.isPresent()) {
            Employee emp = employee.get();
            System.out.println("Удаляем сотрудника: " + emp.getUsername());
            
            // Удаляем соответствующую запись из UserAccount (soft delete)
            Optional<UserAccount> userAccount = userAccountRepository.findByUserName(emp.getUsername());
            if (userAccount.isPresent()) {
                System.out.println("Найдена запись UserAccount для удаления: " + userAccount.get().getUserName());
                UserAccount ua = userAccount.get();
                ua.setStatus(GeneralStatus.Deleted);
                userAccountRepository.save(ua);
                System.out.println("UserAccount помечен как удаленный");
            } else {
                System.out.println("UserAccount не найден для пользователя: " + emp.getUsername());
            }
            
            // Удаляем сотрудника
            employeeRepository.delete(emp);
            System.out.println("Employee удален успешно");
            return true;
        }
        return false;
    }
}
