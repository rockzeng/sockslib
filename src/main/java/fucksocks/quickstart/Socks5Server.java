package fucksocks.quickstart;

import fucksocks.common.SSLConfiguration;
import fucksocks.common.methods.NoAuthenticationRequiredMethod;
import fucksocks.common.methods.UsernamePasswordMethod;
import fucksocks.server.SocksProxyServer;
import fucksocks.server.SocksServerBuilder;
import fucksocks.server.manager.MemoryBasedUserManager;
import fucksocks.server.manager.User;
import fucksocks.server.manager.UserManager;
import fucksocks.utils.ArgUtil;
import fucksocks.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * The class <code>Socks5Server</code> can create a simple Socks5 server.
 *
 * @author Youchao Feng
 * @version 1.0
 * @date Sep 24, 2015 11:25 AM
 */
public class Socks5Server {

  private static final Logger logger = LoggerFactory.getLogger(Socks5Server.class);
  private SocksProxyServer server;

  /**
   * Run a SOCKS5 server. Same as @{@link #start(String[])}.
   *
   * @param args Some arguments.
   * @throws IOException
   */
  public static void main(@Nullable String[] args) throws IOException, InterruptedException {
    Timer.open();
    Socks5Server socks5Server = new Socks5Server();
    socks5Server.start(args);
  }

  /**
   * Start a SOCKS5 server with some options.
   *
   * @param args Arguments.Support "--h --help --port --auth"
   * @throws IOException
   */
  public void start(@Nullable String[] args) throws IOException {
    ArgUtil argUtil = new ArgUtil(args);
    int port = 1080;
    String authValue = null;
    String sslConfigFilePath = null;

    if (argUtil.hasArgsIn("-h", "--help")) {
      showHelp();
      System.exit(0);
    }

    port = argUtil.getIntValue("-p", 1080);
    port = argUtil.getIntValueFromArg("--port=", "=", port);
    authValue = argUtil.getValue("-a", null);
    authValue = argUtil.getValueFromArg("--auth=", "=", authValue);
    sslConfigFilePath = argUtil.getValue("-c", null);
    sslConfigFilePath = argUtil.getValueFromArg("--ssl-config=", "=", sslConfigFilePath);


    SocksServerBuilder builder = null;
    if (authValue == null) {
      builder =
          SocksServerBuilder.newSocks5ServerBuilder().setSocksMethods(new
              NoAuthenticationRequiredMethod());
    } else {
      UserManager userManager = new MemoryBasedUserManager();
      for (String user : authValue.split(",")) {
        String[] userPassword = user.split(":");
        String username = userPassword[0];
        String password = userPassword[1];
        userManager.create(new User(username, password));
      }
      builder =
          SocksServerBuilder.newSocks5ServerBuilder().setSocksMethods(new UsernamePasswordMethod
              ()).setUserManager(userManager);

    }
    builder.setBindPort(port);
    if (sslConfigFilePath != null) {
      SSLConfiguration configuration = SSLConfiguration.load(sslConfigFilePath);
      builder.useSSL(configuration);
    }

    server = builder.build();

    final SocksProxyServer finalServer = server;
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        finalServer.shutdown();
        logger.info("SOCKS5 sever shutdown");
      }
    }));
    server.start();
  }

  /**
   * Print help information.
   */
  public void showHelp() {
    System.out.println("Usage: [Options]");
    System.out.println("    -p, --port=<val>             Server bind port");
    System.out.println("    -a, --auth=<val1:val2>       Use username/password authentication");
    System.out.println("                                 Example: --auth=admin:1234");
    System.out.println("                                          --auth=admin:1234,root:1234");
    System.out.println("    -c, --ssl-config=<val>       SSL configuration file");
    System.out.println("    -h, --help                   Show help");
  }

  /**
   * Shutdown SOCKS5 server.
   */
  public void shutdown() {
    if (server != null) {
      server.shutdown();
    }
  }

}
