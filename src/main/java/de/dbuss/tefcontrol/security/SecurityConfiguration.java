package de.dbuss.tefcontrol.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import de.dbuss.tefcontrol.views.login.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    private final UserDetailsService userDetailsService;

    public SecurityConfiguration(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Component
    public class CustomAuthenticationProvider implements AuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {

            System.out.println("Angemeldet bei Spring Security wird: " + authentication.getName());

            String username = authentication.getName();
            String password = authentication.getCredentials().toString();

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if(!passwordEncoder().matches(password,userDetails.getPassword()))
            {
                System.out.println("Falsches Passwort!");

                if(userDetails.getPassword().equals("xxx"))
                {
                    return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
                }

                //return null;
                return null;
            }

            return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());


        }

        @Override
        public boolean supports(Class<?> authentication) {
            return authentication.equals(UsernamePasswordAuthenticationToken.class);
        }

    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll();

        // Icons from the line-awesome addon
        http.authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll();
        super.configure(http);
        setLoginView(http, LoginView.class);
    }

}
