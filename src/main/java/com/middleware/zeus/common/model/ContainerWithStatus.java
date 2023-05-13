package com.middleware.zeus.common.model;

public class ContainerWithStatus {
	
	private String name;
	
	private Integer restartCount;
	
	private String state;
	
	private String startedAt;
	
	private Integer exitCode;
	
	private Integer signal;
	
	private String reason;
	
	private String message;
	
	private String finishedAt;

	private boolean isReady;

	private String images;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getRestartCount() {
		return restartCount;
	}

	public void setRestartCount(Integer restartCount) {
		this.restartCount = restartCount;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(String startedAt) {
		this.startedAt = startedAt;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public Integer getSignal() {
		return signal;
	}

	public void setSignal(Integer signal) {
		this.signal = signal;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(String finishedAt) {
		this.finishedAt = finishedAt;
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean ready) {
		isReady = ready;
	}

	public String getImages() {
		return images;
	}

	public void setImages(String images) {
		this.images = images;
	}
}
