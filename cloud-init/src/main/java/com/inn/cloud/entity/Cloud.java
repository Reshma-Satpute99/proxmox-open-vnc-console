package com.inn.cloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cloud {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer cloudId;
	private String scsihw;
	private String ide2;
	private String ciuser;
	private String cipassword;
	private String ipconfig0;
	private int onboot;

	@Column(columnDefinition = "LONGTEXT")
	private String sshkeys;

	public Cloud() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Cloud(Integer cloudId, String scsihw, String ide2, String ciuser, String cipassword, String ipconfig0,
			int onboot, String sshkeys) {
		super();
		this.cloudId = cloudId;
		this.scsihw = scsihw;
		this.ide2 = ide2;
		this.ciuser = ciuser;
		this.cipassword = cipassword;
		this.ipconfig0 = ipconfig0;
		this.onboot = onboot;
		this.sshkeys = sshkeys;
	}

	public String getScsihw() {
		return scsihw;
	}

	public void setScsihw(String scsihw) {
		this.scsihw = scsihw;
	}

	public String getIde2() {
		return ide2;
	}

	public Integer getCloudId() {
		return cloudId;
	}

	public void setCloudId(Integer cloudId) {
		this.cloudId = cloudId;
	}

	public void setIde2(String ide2) {
		this.ide2 = ide2;
	}

	public String getCiuser() {
		return ciuser;
	}

	public void setCiuser(String ciuser) {
		this.ciuser = ciuser;
	}

	public String getCipassword() {
		return cipassword;
	}

	public void setCipassword(String cipassword) {
		this.cipassword = cipassword;
	}

	public String getIpconfig0() {
		return ipconfig0;
	}

	public void setIpconfig0(String ipconfig0) {
		this.ipconfig0 = ipconfig0;
	}

	public int getOnboot() {
		return onboot;
	}

	public void setOnboot(int onboot) {
		this.onboot = onboot;
	}

	public String getSshkeys() {
		return sshkeys;
	}

	public void setSshkeys(String sshkeys) {
		this.sshkeys = sshkeys;
	}

	@Override
	public String toString() {
		return "Cloud [cloudId=" + cloudId + ", scsihw=" + scsihw + ", ide2=" + ide2 + ", ciuser=" + ciuser
				+ ", cipassword=" + cipassword + ", ipconfig0=" + ipconfig0 + ", onboot=" + onboot + ", sshkeys="
				+ sshkeys + "]";
	}

}
