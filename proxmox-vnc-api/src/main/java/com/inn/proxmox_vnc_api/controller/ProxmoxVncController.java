package com.inn.proxmox_vnc_api.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inn.proxmox_vnc_api.service.ProxmoxVncService;

@RestController
@CrossOrigin(origins = "http://192.168.1.4:9090", allowCredentials = "true")
public class ProxmoxVncController {

    @Autowired
    private ProxmoxVncService proxmoxVncService;

    @GetMapping("/api/open-vnc")
    public ResponseEntity<?> openVnc(@RequestParam String vmid) {
        System.err.println("Inside Controller...");
        try {
            Map<String, Object> result = proxmoxVncService.openVnc(vmid);

            HttpHeaders headers = new HttpHeaders();
            // ✅ Set cookie properly
            headers.add(HttpHeaders.SET_COOKIE,
                "PVEAuthCookie=" + result.get("cookie") + 
                "; Path=/; Secure; HttpOnly; SameSite=None");

            // Return the result without the raw cookie value (optional security)
            result.remove("cookie");

            return ResponseEntity.ok().headers(headers).body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}




