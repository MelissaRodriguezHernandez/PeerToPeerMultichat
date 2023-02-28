package view;

import java.io.Serializable;

public class Frame implements Serializable {

	public enum FrameType {
		PING,
		PING_ACK,
		MESSAGE
	}

	protected FrameType frameType;
	private Integer timeToLive;
	private String sourceIp;
	private String targetIp;
	private String payload;

	public final void setHeader(Integer timeToLive, String sourceIp, String targetIp) {
		this.timeToLive = timeToLive;
		this.sourceIp = sourceIp;
		this.targetIp = targetIp;
	}

	public final void setPayload(String payload) {
		this.payload = payload;
	}

	public String getPayload() {
		return payload;
	}

	public final boolean decrementTTL() {
		if(this.timeToLive > 0)
			return true;
		else
			--this.timeToLive;
		return false;
	}

	public final FrameType getFrameType() {
		return this.frameType;
	}

	public final String getSourceIP() {
		return this.sourceIp;
	}

	public final String getTargetIP() {
		return this.targetIp;
	}

	public void setFrameType(FrameType frameType) {
		this.frameType = frameType;
	}

}


