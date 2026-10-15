package com.codesense.prototype.web;

import com.codesense.prototype.account.AccountService;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.web.dto.AuthRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> signup(@RequestBody AuthRequest request) {
        return pack(accounts.signup(request.getName(), request.getUsername(), request.getPassword()));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        return pack(accounts.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String header) {
        return pack(accounts.me(header));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        accounts.logout(header);
        return Map.of("ok", true);
    }

    private static Map<String, Object> pack(Account account) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", account.getId());
        body.put("name", account.getName());
        body.put("username", account.getUsername());
        body.put("token", account.getToken());
        body.put("created", account.getCreated());
        body.put("labs", account.getLabs());
        return body;
    }
}
