package xlike.top.kn_ai_chat.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway数据库迁移配置
 * @author kn_ai_chat
 */
@Configuration
@ConditionalOnProperty(name = "flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig{

    private static final Logger logger=LoggerFactory.getLogger(FlywayConfig.class);

    @Autowired
    private DataSource dataSource;

    @Value("${flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    /**
     * 创建Flyway实例
     * @return Flyway实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "flyway")
    public Flyway flyway(){
        logger.info("Initializing Flyway...");

        logger.info("Using Flyway locations: {}", flywayLocations);

        Flyway flyway=Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .encoding("UTF-8")
                .load();

        logger.info("Flyway configured successfully with locations: {}", flywayLocations);
        return flyway;
    }

    /**
     * 执行数据库迁移
     */
    public void migrateDatabase(){
        try{
            logger.info("Starting database migration...");
            Flyway flywayInstance=flyway();
            logger.info(">>>>>> 开始执行Flyway迁移 <<<<<<");
            MigrateResult result=flywayInstance.migrate();
            logger.info("Database migration completed successfully. {} migrations applied.", result.migrationsExecuted);
            logger.info(">>>>>> Flyway迁移完成 <<<<<<");
        } catch(Exception e){
            logger.error("Database migration failed", e);
            throw new RuntimeException("Failed to migrate database", e);
        }
    }

    /**
     * 创建应用启动后执行的迁移任务
     */
    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway){
        return new FlywayMigrationInitializer(flyway, null);
    }
}