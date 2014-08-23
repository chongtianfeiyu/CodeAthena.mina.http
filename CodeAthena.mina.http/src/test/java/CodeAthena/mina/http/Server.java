package CodeAthena.mina.http;

import java.io.File;
import java.net.InetSocketAddress;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Server {
	public static final Logger log = LoggerFactory.getLogger(Server.class);

	private static int DEFAULT_PORT = 8080;
	public static final String AUTHOR_STRING = "www.codeAthena.net camus";
	public static final String VERSION_STRING = "V1.00.2014.0823";

	public static void printVersion() {
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("	                 version : " + VERSION_STRING);
		System.out.println("	                 author  : " + AUTHOR_STRING);
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}

	public static void logbackInit() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			String logbackXml = System.getProperty("user.dir") + File.separatorChar + "logback.xml";
			configurator.doConfigure(logbackXml);
			System.out.println(logbackXml + " load ok.");
		} catch (JoranException je) {
			log.error("Exception", je);
			if (context != null) {
				StatusPrinter.print(context);
			}
		}
	}

	public static void main(String[] args) {
		logbackInit();

		int port = DEFAULT_PORT;
		try {

			NioSocketAcceptor acceptor = new NioSocketAcceptor();
			acceptor.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(new HttpServerProtocolCodecFactory()));
			acceptor.setHandler(new ServerHandler());
			acceptor.setReuseAddress(true);
			acceptor.bind(new InetSocketAddress(port));

			printVersion();
			log.info("[Server] listening on port " + port);
		} catch (Exception ex) {
			log.error("Exception", ex);
		}
	}
}
