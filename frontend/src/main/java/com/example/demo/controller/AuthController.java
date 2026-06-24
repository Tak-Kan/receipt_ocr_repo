package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private static final String SESSION_USER_KEY = "user";

    // show login form
    public String loginForm(HttpServletRequest req, Model model) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            // already logged in -> redirect to top
            return "redirect:/top";
        }
        return "hams_login";
    }

    // handle login submit
    public String doLogin(HttpServletRequest req, Model model) {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Simple auth: accept non-empty username/password OR match hardcoded credentials.
        // Replace with proper authentication (DB check or Spring Security) in real apps.
        boolean valid = false;
        if (username != null && password != null) {
            // example: allow 'admin' / 'password' OR any non-empty combination
            if ("admin".equals(username) && "password".equals(password)) {
                valid = true;
            } else if (!username.isBlank() && !password.isBlank()) {
                valid = true;
            }
        }

        if (!valid) {
            model.addAttribute("error", "Invalid username or password");
            return "hams_login";
        }

        // create session and store user info
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER_KEY, username);
        // redirect to top page
        return "redirect:/top";
    }

    // top page (requires login)
    public String top(HttpServletRequest req, Model model) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER_KEY) == null) {
            return "redirect:/login";
        }
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        model.addAttribute("username", username);
        return "hams_top";
    }

    // logout
    @GetMapping("/logout")
    public String logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}