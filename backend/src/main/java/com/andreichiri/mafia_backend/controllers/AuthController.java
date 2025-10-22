package com.andreichiri.mafia_backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.andreichiri.mafia_backend.services.AuthService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("auth/login")
    public ResponseEntity<?> authLogin() {
        return authService.authLogin();
    }


    @PostMapping("auth/register")
    public ResponseEntity<?> authRegister() {
        return authService.authRegister();
    }
    
}
