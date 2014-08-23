package CodeAthena.mina.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerHandler extends IoHandlerAdapter {
	public static final Logger log = LoggerFactory.getLogger(ServerHandler.class);
	public static boolean debug = false;

	public ServerHandler() {
	}

	public void httpVersion(IoSession session, HttpRequestMessage request, HttpResponseMessage response) throws Exception {
		log.info("[httpVersion]:" + Server.VERSION_STRING);
		response.setContentType("text/plain");
		response.setResponseCode(HttpResponseMessage.HTTP_STATUS_SUCCESS);
		response.appendBody(Server.VERSION_STRING + " \n");
		response.appendBody(Server.AUTHOR_STRING);
		session.write(response).isWritten();
	}

	public static String decodeString(String text, String enc) {
		String decodeText = "";
		try {
			decodeText = URLDecoder.decode(text, enc);
		} catch (UnsupportedEncodingException e) {
			log.error("[decodeString error] " + text, e);
		}
		if (decodeText == null) {
			decodeText = "";
		}
		return decodeText;
	}

	public void httpAccess(IoSession session, HttpRequestMessage request, HttpResponseMessage response) {
		log.info(request.getHeader("Context")[0]);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String userName = decodeString(request.getParameter("userName"), "UTF-8");
		log.info("[userName]" + userName);

		response.setContentType("text/html");
		response.setResponseCode(HttpResponseMessage.HTTP_STATUS_SUCCESS);
		response.appendBody("[httpServer] you visit " + request.getHeader("Context")[0] + "<br/>\n");
		response.appendBody("[httpServer] server has  run " + (new Date()).toString());
		session.write(response).isWritten();
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		HttpRequestMessage request = (HttpRequestMessage) message;
		HttpResponseMessage response = new HttpResponseMessage();
		String context = request.getHeader("Context")[0];
		if (context.equalsIgnoreCase("version")) {
			httpVersion(session, request, response);
		} else {
			httpAccess(session, request, response);
		}
		session.close(true);
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
		log.error("exceptionCaught", cause);
		session.close(true);
	}
}