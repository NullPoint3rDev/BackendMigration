package org.alloy;

import org.alloy.repositories.*;
import org.alloy.security.JwtTokenProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.ArrayList;
import javax.sql.DataSource;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class TestConfig {
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // Mock all repositories
    @MockBean private AlertRepository alertRepository;
    @MockBean private DumpRepository dumpRepository;
    @MockBean private EmailTemplateRepository emailTemplateRepository;
    @MockBean private InboxMessageRepository inboxMessageRepository;
    @MockBean private InboxNotificationRepository inboxNotificationRepository;
    @MockBean private MaintenanceRepository maintenanceRepository;
    @MockBean private NotificationRepository notificationRepository;
    @MockBean private OrganizationRepository organizationRepository;
    @MockBean private OrganizationUnitRepository organizationUnitRepository;
    @MockBean private QueuePushEventRepository queuePushEventRepository;
    @MockBean private QueueTaskRepository queueTaskRepository;
    @MockBean private SurveyPassQuestionRepository surveyPassQuestionRepository;
    @MockBean private SurveyQuestionRepository surveyQuestionRepository;
    @MockBean private SurveyRepository surveyRepository;
    @MockBean private TranslationRepository translationRepository;
    @MockBean private UserAccountRepository userAccountRepository;
    @MockBean private UserActRepository userActRepository;
    @MockBean private UserPermissionRepository userPermissionRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private UserRolePermissionRepository userRolePermissionRepository;
    @MockBean private UserTokenRepository userTokenRepository;
    @MockBean private WeldingMachineParameterValueRepository weldingMachineParameterValueRepository;
    @MockBean private WeldingMachineRepository weldingMachineRepository;
    @MockBean private WeldingMachineStateRepository weldingMachineStateRepository;
    @MockBean private WeldingMachineTypeRepository weldingMachineTypeRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("test")
                .password("test")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(new ArrayList<>(java.util.Arrays.asList(user)));
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
} 