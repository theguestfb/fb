package fb.json;

import java.util.Date;

public class JsonCaptchaResponse {
	public boolean getSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public Date getChannelge_ts() {
		return channelge_ts;
	}
	public void setChannelge_ts(Date channelge_ts) {
		this.channelge_ts = channelge_ts;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	private boolean success;
	private Date channelge_ts;
	private String hostname;
}