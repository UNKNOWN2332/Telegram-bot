package uz.shukrullaev.questionbot

import jakarta.persistence.EntityManagerFactory
import org.khasanof.StateConfigurerAdapter
import org.khasanof.state.configurer.StateConfigurer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import javax.sql.DataSource


@EnableJpaAuditing
@SpringBootApplication()
@EnableJpaRepositories(
    basePackages = ["uz.shukrullaev.questionbot"],
    repositoryBaseClass = BaseRepositoryImpl::class,
)
@Configuration
class TelegramQuestionBotApplication : StateConfigurerAdapter<StateCollection> {
    override fun configure(state: StateConfigurer<StateCollection>?) {
        state?.initial(StateCollection.START)
            ?.states(EnumSet.allOf(StateCollection::class.java))
    }
}

@Configuration
class JpaConfig {
    @Bean
    @Primary
    fun entityManagerFactory(
        dataSource: DataSource,
        jpaVendorAdapter: JpaVendorAdapter,
    ): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = dataSource
        factory.jpaVendorAdapter = jpaVendorAdapter
        factory.setPackagesToScan("uz.shukrullaev.questionbot")

        val jpaProperties = mutableMapOf<String, Any>()
        jpaProperties["hibernate.hbm2ddl.auto"] = "update"
        jpaProperties["hibernate.dialect"] = "org.hibernate.dialect.PostgreSQLDialect"
        jpaProperties["hibernate.show_sql"] = true

        factory.setJpaPropertyMap(jpaProperties)
        return factory
    }

    @Bean
    @Primary
    fun transactionManager(
        entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

fun main(args: Array<String>) {
    runApplication<TelegramQuestionBotApplication>(*args)
}

