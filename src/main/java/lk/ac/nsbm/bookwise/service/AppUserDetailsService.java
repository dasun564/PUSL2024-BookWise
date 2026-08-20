package lk.ac.nsbm.bookwise.service;

import lk.ac.nsbm.bookwise.entity.AppUser;
import lk.ac.nsbm.bookwise.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges BookWise accounts into Spring Security.
 *
 * The polymorphic {@code AppUser.getRole()} means this class never tests which
 * subclass it loaded - each account type reports its own authority. Adding a
 * third kind of user later would require no change here.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account named " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())     // becomes ROLE_STUDENT or ROLE_ADMIN
                .build();
    }
}
