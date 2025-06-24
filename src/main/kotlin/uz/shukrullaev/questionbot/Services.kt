package uz.shukrullaev.questionbot

import org.khasanof.state.State
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*


/**
 * @see uz.shukrullaev.questionbot
 * @author Abdulloh
 * @since 18/06/2025 5:39 pm
 */

@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
) {


    fun login(loginDTO: LoginDTO): TokenDTO? {
        return listOfNotNull(
            checkByEmailAndPassword(loginDTO),
        ).firstOrNull() ?: throw UsernameAndPhoneNotFoundException(loginDTO)
    }

    fun setOperatorRole(setRoleDTO: SetRoleDTO): Boolean {

        val phone = setRoleDTO.phone



        return when {
            phone != null -> assignRoleIfExists(phone) { userRepository.findByPhoneAndDeletedFalse(it) }
            else -> false
        }
        return true
    }

    private fun <T> assignRoleIfExists(
        identifier: T,
        finder: (T) -> Optional<UserAccount>,
    ): Boolean {
        val user = finder(identifier).orElse(null) ?: return false
        user.role = UserRole.OPERATOR
        userRepository.save(user)
        return true
    }


//    fun setRole(loginDTO: LoginDTO): Boolean {
//        setRoleIfPhoneExist(loginDTO)
//        setRoleIfUserExist(loginDTO)
//        return true
//    }

//    private fun setRoleIfPhoneExist(loginDTO: LoginDTO) {
//        loginDTO.phone?.let {
//            userRepository.findByPhoneAndDeletedFalse(it)
//                .orElseThrow { throw PhoneNotFoundException(it) }
//                .apply {
//                    role = UserRole.OPERATOR
//                }.also { setUser ->
//                    userRepository.save(setUser)
//                }
//            true
//        }
//    }
//
//    private fun setRoleIfUserExist(loginDTO: LoginDTO) {
//        loginDTO.username?.let {
//            userRepository.findByUsernameAndDeletedFalse(it)
//                .orElseThrow { throw UsernameNotFoundException(it) }
//                .apply {
//                    role = UserRole.OPERATOR
//                }.also { setUser ->
//                    userRepository.save(setUser)
//                }
//            true
//        }
//    }



    private fun checkByEmailAndPassword(loginDTO: LoginDTO): TokenDTO? {
        return loginDTO.phone?.let {
            userRepository.findByPhoneAndDeletedFalse(it)
                .orElseThrow { UsernameNotFoundException(it) }
                .let { user ->
                    (passwordEncoder.matches(loginDTO.password, user.password)).runIfFalse {
                        throw PhoneOrPasswordIncorrect(loginDTO)
                    }
                    jwtUtil.generateToken(user)
                }

        }
    }
}
