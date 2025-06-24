package uz.shukrullaev.questionbot

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:43 pm
 */
@RestController
@RequestMapping("api/users")
class UserController(
    private val userAccountService: UserAccountService,
) {
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody dto: LoginDTO): TokenDTO? =
        userAccountService.login(dto)

    @PostMapping("set-user-role")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun setOperatorRole(@RequestBody setRoleDTO: SetRoleDTO): Boolean =
        userAccountService.setOperatorRole(setRoleDTO)

}