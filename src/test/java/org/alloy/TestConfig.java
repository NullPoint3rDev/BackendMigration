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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Properties;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class TestConfig {
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // Mock all repositories except those needed for WeldingMachineRepositoryTest
    @MockBean private AlertRepository alertRepository;
    @MockBean private DumpRepository dumpRepository;
    @MockBean private EmailTemplateRepository emailTemplateRepository;
    @MockBean private InboxMessageRepository inboxMessageRepository;
    @MockBean private InboxNotificationRepository inboxNotificationRepository;
    @MockBean private MaintenanceRepository maintenanceRepository;
    @MockBean private NotificationRepository notificationRepository;
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
    @MockBean private WeldingMachineStateRepository weldingMachineStateRepository;

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

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.alloy.models", "org.alloy.models.entities");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setShowSql(true);
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        em.setJpaProperties(properties);

        return em;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 