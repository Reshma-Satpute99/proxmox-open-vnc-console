package com.inn.proxmox_vnc_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

	@GetMapping("/")
	public String root() {
		return "forward:/index.html";
	}

	@GetMapping("/don")
	public String don() {
		return "forward:/index.html";
	}
}
