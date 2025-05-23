	package com.inn.cloud.service;
	
	
	import org.springframework.http.ResponseEntity;
	
	import com.inn.cloud.entity.Cloud;
	
	public interface CloudService {
	
		public ResponseEntity<String> updateCloudInitConfig(Long vmId, String node, Cloud cloud) ;
	
	}
