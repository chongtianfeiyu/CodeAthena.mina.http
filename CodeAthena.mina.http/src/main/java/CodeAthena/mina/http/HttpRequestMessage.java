package CodeAthena.mina.http;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class HttpRequestMessage {
	private Map<?, ?> headers = null;

	public void setHeaders(Map<?, ?> headers) {
		this.headers = headers;
	}

	public Map<?, ?> getHeaders() {
		return headers;
	}

	public String getContext() {
		String[] context = (String[]) headers.get("Context");
		return context == null ? "" : context[0];
	}

	public String getParameter(String name) {
		if (headers.containsKey("@".concat(name)) == false) {
			return "";
		}

		String[] param = (String[]) headers.get("@".concat(name));

		if (param == null || param.length == 0) {
			return "";
		}

		return param[0];
	}

	public String[] getParameters(String name) {
		String[] param = (String[]) headers.get("@".concat(name));
		return param == null ? new String[] {} : param;
	}

	public String[] getHeader(String name) {
		return (String[]) headers.get(name);
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		Iterator<?> it = headers.entrySet().iterator();
		while (it.hasNext()) {
			Entry<?, ?> e = (Entry<?, ?>) it.next();
			str.append(e.getKey() + " : " + arrayToString((String[]) e.getValue(), ',') + "\n");
		}
		return str.toString();
	}

	public static String arrayToString(String[] s, char sep) {
		if (s == null || s.length == 0) {
			return "";
		}
		StringBuffer buf = new StringBuffer();
		if (s != null) {
			for (int i = 0; i < s.length; i++) {
				if (i > 0)
					buf.append(sep);
				buf.append(s[i]);
			}
		}
		return buf.toString();
	}
}
