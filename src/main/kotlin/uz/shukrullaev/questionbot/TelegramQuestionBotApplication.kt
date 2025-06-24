package uz.shukrullaev.questionbot

import org.khasanof.StateConfigurerAdapter
import org.khasanof.state.configurer.StateConfigurer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.util.*


@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = ["uz.shukrullaev.questionbot"])
@EntityScan("uz.shukrullaev.questionbot")
@EnableJpaRepositories("uz.shukrullaev.questionbot", repositoryBaseClass = BaseRepositoryImpl::class)
@Configuration
class TelegramQuestionBotApplication {

    @Bean
    fun stateConfigurerAdapter(): StateConfigurerAdapter<StateCollection> {
        return StateConfigurerAdapter<StateCollection>() {
            fun configure(state: StateConfigurer<StateCollection>) {
                state.initial(StateCollection.START)
                    .states(EnumSet.allOf(StateCollection::class.java))
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<TelegramQuestionBotApplication>(*args)
}

