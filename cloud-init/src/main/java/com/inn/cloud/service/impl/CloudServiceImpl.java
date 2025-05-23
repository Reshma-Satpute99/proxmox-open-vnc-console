package com.inn.cloud.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inn.cloud.entity.Cloud;
import com.inn.cloud.repository.CloudRepository;
import com.inn.cloud.service.CloudService;

@Service
public class CloudServiceImpl implements CloudService {

	@Value("${proxmox.api.url}")
	private String apiUrl;

	@Value("${proxmox.token.id}")
	private String tokenId;

	@Value("${proxmox.token.secret}")
	private String tokenSecret;

	private final RestTemplate restTemplate;
	private final CloudRepository cloudRepository;
	
	
	
	@Autowired
	public CloudServiceImpl(RestTemplate restTemplate, CloudRepository cloudRepository) {
		this.restTemplate = restTemplate;
		this.cloudRepository = cloudRepository;
	}

	@Override
	public ResponseEntity<String> updateCloudInitConfig(Long vmId, String node, Cloud cloud) {
		// 1. Call Proxmox API
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "PVEAPIToken=" + tokenId + "=" + tokenSecret);

		Map<String, Object> payload = new HashMap<>();

		payload.put("node", node);
		payload.put("ciuser", cloud.getCiuser());
		payload.put("cipassword", cloud.getCipassword());
		payload.put("scsihw", cloud.getScsihw());
		payload.put("ipconfig0", cloud.getIpconfig0());
		payload.put("sshkeys", cloud.getSshkeys());
        payload.put("onboot", cloud.getOnboot());
        
        
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonPayload;
		try {
			jsonPayload = objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error serializing request body: " + e.getMessage());
		}

		headers.setContentLength(jsonPayload.getBytes(StandardCharsets.UTF_8).length);

		String url = String.format("%s/nodes/%s/qemu/%d/config", apiUrl, node, vmId);

		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
				cloudRepository.save(cloud);
				return ResponseEntity.ok("Cloud created successfully: " + response.getBody());
			} else {
				return ResponseEntity.status(response.getStatusCode()).body("Failed to create container. Status: "
						+ response.getStatusCode() + ", Body: " + response.getBody());
			}

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " + e.getMessage());
		}

	}

}
