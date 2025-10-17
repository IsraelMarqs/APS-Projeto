package com.example.demo;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository userRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    private final Environment env;

    public AuthController(UserRepository userRepository, Environment env) {
        this.userRepository = userRepository;
        this.env = env;
    }

    private PasswordEncoder encoder() {
        return passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
    }

    @GetMapping("/login")
    public String login(Model model) {
        String clientId = env.getProperty("GOOGLE_CLIENT_ID");
        String propClientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
        boolean hasGoogle = (clientId != null && !clientId.isBlank()) || (propClientId != null && !propClientId.isBlank());
        model.addAttribute("hasGoogle", hasGoogle);
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        String clientId = env.getProperty("GOOGLE_CLIENT_ID");
        String propClientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
        boolean hasGoogle = (clientId != null && !clientId.isBlank()) || (propClientId != null && !propClientId.isBlank());
        model.addAttribute("hasGoogle", hasGoogle);

        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "register";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "As senhas não coincidem");
            return "register";
        }

        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            result.rejectValue("email", "email.exists", "Já existe uma conta com esse email");
            return "register";
        }

        User user = new User();
        user.setName(form.getName());
        user.setEmail(form.getEmail());
        user.setPassword(encoder().encode(form.getPassword()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Cadastro realizado com sucesso. Faça login.");
        return "redirect:/login";
    }
}
