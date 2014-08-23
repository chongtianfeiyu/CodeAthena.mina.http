package CodeAthena.mina.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestDecoder extends MessageDecoderAdapter {
	private static final byte[] CONTENT_LENGTH = new String("Content-Length:")
			.getBytes();
	public static final Logger log = LoggerFactory
			.getLogger(HttpRequestDecoder.class);
	private CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
	private CharsetDecoder gbkDecoder = Charset.forName("GBK").newDecoder();

	private HttpRequestMessage request = null;

	public HttpRequestDecoder() {

	}

	public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
		try {
			return messageComplete(in) ? MessageDecoderResult.OK
					: MessageDecoderResult.NEED_DATA;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return MessageDecoderResult.NOT_OK;
	}

	public MessageDecoderResult decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		HttpRequestMessage m = decodeBody(in);
		if (m == null) {
			return MessageDecoderResult.NEED_DATA;
		}
		out.write(m);
		return MessageDecoderResult.OK;
	}

	private boolean messageComplete(IoBuffer in) throws Exception {
		int last = in.remaining() - 1;
		if (in.remaining() < 4)
			return false;

		// to speed up things we check if the Http request is a GET or POST
		if (in.get(0) == (byte) 'G' && in.get(1) == (byte) 'E'
				&& in.get(2) == (byte) 'T') {
			// Http GET request therefore the last 4 bytes should be 0x0D 0x0A
			// 0x0D 0x0A
			return (in.get(last) == (byte) 0x0A
					&& in.get(last - 1) == (byte) 0x0D
					&& in.get(last - 2) == (byte) 0x0A && in.get(last - 3) == (byte) 0x0D);
		} else if (in.get(0) == (byte) 'P' && in.get(1) == (byte) 'O'
				&& in.get(2) == (byte) 'S' && in.get(3) == (byte) 'T') {
			// Http POST request
			// first the position of the 0x0D 0x0A 0x0D 0x0A bytes
			int eoh = -1;
			for (int i = last; i > 2; i--) {
				if (in.get(i) == (byte) 0x0A && in.get(i - 1) == (byte) 0x0D
						&& in.get(i - 2) == (byte) 0x0A
						&& in.get(i - 3) == (byte) 0x0D) {
					eoh = i + 1;
					break;
				}
			}
			if (eoh == -1)
				return false;
			for (int i = 0; i < last; i++) {
				boolean found = false;
				for (int j = 0; j < CONTENT_LENGTH.length; j++) {
					if (in.get(i + j) != CONTENT_LENGTH[j]) {
						found = false;
						break;
					}
					found = true;
				}
				if (found) {
					// retrieve value from this position till next 0x0D 0x0A
					StringBuilder contentLength = new StringBuilder();
					for (int j = i + CONTENT_LENGTH.length; j < last; j++) {
						if (in.get(j) == 0x0D)
							break;
						contentLength.append(new String(
								new byte[] { in.get(j) }));
					}
					// if content-length worth of data has been received then
					// the message is complete
					return (Integer.parseInt(contentLength.toString().trim())
							+ eoh == in.remaining());
				}
			}
		}

		// the message is not complete and we need more data
		return false;
	}

	public static String gbkToUtf8(String gbkText)
			throws UnsupportedEncodingException {
		String isoText = new String(gbkText.getBytes("UTF-8"), "ISO-8859-1");
		String utf8Text = new String(isoText.getBytes("ISO-8859-1"), "UTF-8");
		return utf8Text;
	}

	@SuppressWarnings("unused")
	private HttpRequestMessage decodeBody(IoBuffer in) {
		request = new HttpRequestMessage();

		try {
			StringReader sReader = null;
			try {
				String text = in.getString(decoder);
				sReader = new StringReader(text);
			} catch (Exception ex) {
				String text = in.getString(gbkDecoder);
				String utf8Text = gbkToUtf8(text);
				sReader = new StringReader(utf8Text);
			}

			if (sReader == null) {
				return null;
			}
			request.setHeaders(parseRequest(sReader));
			return request;
		} catch (Exception ex) {
			log.error("decodeBody", ex);
		}

		return null;
	}

	private Map<String, String[]> parseRequest(Reader is) {
		Map<String, String[]> map = new HashMap<String, String[]>();
		BufferedReader rdr = new BufferedReader(is);

		try {
			// Get request URL.
			String line = rdr.readLine();
			String[] url = line.split(" ");
			if (url.length < 3)
				return map;

			map.put("URI", new String[] { line });
			map.put("Method", new String[] { url[0].toUpperCase() });
			map.put("Context", new String[] { url[1].substring(1) });
			map.put("Protocol", new String[] { url[2] });
			// Read header
			while ((line = rdr.readLine()) != null && line.length() > 0) {
				String[] tokens = line.split(": ");
				map.put(tokens[0], new String[] { tokens[1] });
			}

			// If method 'POST' then read Content-Length worth of data
			if (url[0].equalsIgnoreCase("POST")) {
				int len = Integer.parseInt(((String[]) map
						.get("Content-Length"))[0]);
				char[] buf = new char[len];
				if (rdr.read(buf) == len) {
					line = String.copyValueOf(buf);
				}
			} else if (url[0].equalsIgnoreCase("GET")) {
				int idx = url[1].indexOf('?');
				if (idx != -1) {
					map.put("Context",
							new String[] { url[1].substring(1, idx) });
					line = url[1].substring(idx + 1);
				} else {
					line = null;
				}
			}
			if (line != null) {
				String[] match = line.split("\\&");
				for (int i = 0; i < match.length; i++) {
					String[] params = new String[1];
					String[] tokens = match[i].split("=");
					switch (tokens.length) {
					case 0:
						map.put("@".concat(match[i]), new String[] {});
						break;
					case 1:
						map.put("@".concat(tokens[0]), new String[] {});
						break;
					default:
						String name = "@".concat(tokens[0]);
						if (map.containsKey(name)) {
							params = (String[]) map.get(name);
							String[] tmp = new String[params.length + 1];
							for (int j = 0; j < params.length; j++)
								tmp[j] = params[j];
							params = null;
							params = tmp;
						}
						params[params.length - 1] = tokens[1].trim();
						map.put(name, params);
					}
				}
			}
		} catch (IOException ex) {
			log.error("parseRequest", ex);
		}
		return map;
	}
}
