package com.inn.proxmox_vnc_api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inn.proxmox_vnc_api.service.NoVncService;

@CrossOrigin(origins = "http://192.168.192.57:9090", allowCredentials = "true")
@RestController
public class LxcController {

	@Autowired
	private NoVncService novncService;

	@GetMapping("/api/open-vnc")
	public ResponseEntity<?> openVnc(@RequestParam String vmid) {
		System.err.println("Inside Controller...");

		try {
			System.err.println("Inside try block of LxcController...");
			Map<String, Object> result = novncService.openVnc(vmid);

			if (result.containsKey("cookie")) {
				String cookie = (String) result.get("cookie");

				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.SET_COOKIE,
						"PVEAuthCookie=" + cookie + "; Path=/; Secure; HttpOnly; SameSite=None");
				System.out.println("Headers : " + headers);

				result.remove("cookie");
				System.err.println("result :" +result);

				return ResponseEntity.ok().headers(headers).body(result);
			} else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("error", "Failed to retrieve cookie from Proxmox"));
			}

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid input: " + e.getMessage()));
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Internal server error: " + e.getMessage()));
		}
	}

}
