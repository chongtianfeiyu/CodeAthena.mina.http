package CodeAthena.mina.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;

public class HttpResponseMessage {

	public static final String HTTP_MINA2_VERSION = "V1.00";

	/** HTTP response codes */
	public static final int HTTP_STATUS_SUCCESS = 200;

	public static final int HTTP_STATUS_NOT_FOUND = 404;

	/** Map<String, String> */
	private Map headers = new HashMap();

	/** Storage for body of HTTP response. */
	private ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

	private int responseCode = HTTP_STATUS_SUCCESS;

	public HttpResponseMessage() {
		headers.put("Server", "HttpServer (" + HTTP_MINA2_VERSION + ')');
	}

	public Map getHeaders() {
		return headers;
	}

	public void setContentType(String contentType) {
		headers.put("Content-Type", contentType);
	}

	public void setHeader(String key, String value) {
		headers.put(key, value);
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public int getResponseCode() {
		return this.responseCode;
	}

	public void appendBody(byte[] b) {
		try {
			body.write(b);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void appendBody(String s) {
		try {
			body.write(s.getBytes());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public IoBuffer getBody() {
		return IoBuffer.wrap(body.toByteArray());
	}

	public int getBodyLength() {
		return body.size();
	}
}
