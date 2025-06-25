package uz.shukrullaev.questionbot

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.extern.slf4j.Slf4j
import org.khasanof.constants.FluentConstants.BASE_PACKAGE
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter
import org.springframework.boot.context.TypeExcludeFilter
import org.springframework.context.MessageSource
import org.springframework.context.annotation.*
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.core.convert.converter.Converter
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.security.Key
import java.util.*
import javax.sql.DataSource


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:18 pm
 */

@Configuration
class WebMvcConfigure : WebMvcConfigurer {
    @Bean
    @Primary
    fun messageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setDefaultLocale(Locale("uz"))
        setBasename("errors")
    }

}

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val messageSource: MessageSource) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()


    private val secret = "superSecretKeyForJwtSigningChangeThisToStrongKey123!"

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/users/auth/**"
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(CustomAuthenticationEntryPoint(messageSource))
                it.accessDeniedHandler(CustomAccessDeniedHandler(messageSource))
            }
            .oauth2ResourceServer { configurer ->
                configurer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(CustomReactiveJwtAuthenticationConverter())
                }
            }

        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        return NimbusJwtDecoder.withSecretKey(key).build()
    }
}

class CustomReactiveJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val roles: List<String> = when (val rawRoles = jwt.claims["roles"]) {
            is String -> listOf(rawRoles)
            is Collection<*> -> rawRoles.filterIsInstance<String>()
            else -> emptyList()
        }

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        return JwtAuthenticationToken(jwt, authorities)
    }
}

class CustomAccessDeniedHandler(
    private val messageSource: MessageSource,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        accessDeniedException: org.springframework.security.access.AccessDeniedException?,
    ) {
        val locale = request?.getLocale() // foydalanuvchining tilini aniqlaydi
        val message = locale?.let { messageSource.getMessage("access.denied", null, it) }

        response?.status = HttpServletResponse.SC_FORBIDDEN
        response?.contentType = "application/json"
        response?.writer?.write("""{ "message": "$message" }""")
    }
}


class CustomAuthenticationEntryPoint(
    private val messageSource: MessageSource,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val locale = request.locale
        val message = messageSource.getMessage("auth.unauthorized", null, locale)

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{ "message": "$message" }""")
    }
}


@Component
class JwtUtil {

    private val secret = "superSecretKeyForJwtSigningChangeThisToStrongKey123!"
    private val expirationMillis = 60 * 15 * 1000L // 15 daqiqa
    private val key: Key = Keys.hmacShaKeyFor(secret.toByteArray())


    fun generateToken(user: UserAccount): TokenDTO {
        val now = Date()
        val expiry = Date(now.time + expirationMillis)
        val claims = Jwts.claims().setSubject(user.id.toString())
        claims["roles"] = user.role
        val token = generateToken(user.toDTO(), now, expiry, claims)
        return TokenDTO(token, now, expiry)
    }


    private fun generateToken(userDTO: UserAccountResponseDto, now: Date, expiry: Date, claims: Claims): String? =
        Jwts.builder()
            .setSubject(userDTO.phone)
            .setClaims(mutableMapOf<String, Any>("user" to userDTO))
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiry) // 15 daqiqa
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
}


@Slf4j
@ComponentScan(
    basePackages = [BASE_PACKAGE],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [TypeExcludeFilter::class]),
        ComponentScan.Filter(type = FilterType.CUSTOM, classes = [AutoConfigurationExcludeFilter::class])
    ]
)
class SpringBootFluentPostgresqlAutoConfiguration {

    private val log = LoggerFactory.getLogger(SpringBootFluentPostgresqlAutoConfiguration::class.java)

    @Bean
    fun commandLineRunner(): CommandLineRunner {
        return CommandLineRunner { log.info("Postgresql Strategy Initialize!!!") }
    }
}


