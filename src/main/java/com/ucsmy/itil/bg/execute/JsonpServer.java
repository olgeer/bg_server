package com.ucsmy.itil.bg.execute;

import com.sword.nio.NIOProcess;
import com.ucsmy.itil.bg.api.APIRequest;
import com.ucsmy.itil.bg.api.APIResponse;
import com.ucsmy.itil.bg.api.VmwareApi;
import com.ucsmy.itil.bg.common.*;
import com.ucsmy.itil.bg.service.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.exit;

/**
 * @author Max
 */
public class JsonpServer {
    public static boolean LOG = false;//日志打印标志
    public static int PORT = 80;// 标准HTTP端口
    //public static int PORT = 8081;// 标准HTTP端口
    public static int SLEEP = 10;//循环休眠时间，单位ms
    public static int POOLSIZE = 15;//线程池大小
    public static int SO_TIMEOUT = 5 * 60 * 1000;//等待链接的timeout时长，单位毫秒
    private static UcsmyLog logger = new UcsmyLog(JsonpServer.class.getName());
    private static boolean KEEPRUNNING=true;

    ServerSocket serverSocket;// 服务器Socket
    ExecutorService pool = Executors.newFixedThreadPool(POOLSIZE, new MaxThreadFactory("JSONP-Service"));

    public JsonpServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setSoTimeout(SO_TIMEOUT);
        } catch (Exception e) {
            logger.error("Unable start the HTTP service cause:" + e.getMessage());
        }

        if (serverSocket == null) {
            exit(1);// 无法开始服务器
        }

        Socket client = null;
        int accept = 0;
        while (KEEPRUNNING) {
            try {
                client = serverSocket.accept();
                pool.execute(new JProcess(client, LOG));
                //logger.debug("Accept " + (++accept) + " times !");
                //sleep(SLEEP);
            } catch (SocketTimeoutException ste) {
                logger.debug("Timeout ,stop listening !", false);
            } catch (Exception e) {
                e.printStackTrace();
                exit(1);
            }
        }
    }

    public static void main(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                String cmd = args[i].substring(0, 2);
                switch (cmd) {
                    case "-p":
                        PORT = Integer.parseInt(args[i].substring(2));
                        break;
                    case "-l":
                        LOG = args[i].substring(2).compareToIgnoreCase("on") == 0;
                        break;
                    case "-t":
                        SO_TIMEOUT = Integer.parseInt(args[i].substring(2));
                        break;
                    case "-s":
                        POOLSIZE = Integer.parseInt(args[i].substring(2));
                        break;
                    case "-z":
                        SLEEP = Integer.parseInt(args[i].substring(2));
                        break;
                    case "-r":
                        KEEPRUNNING = args[i].substring(2).compareToIgnoreCase("true") == 0;
                        break;
                    case "-c":
                        Configure.setConfigPath(args[i].substring(2));
                        break;
                    case "-h":
                    case "-?":
                    default:
                        System.out.println("Jsonp service V1.5");
                        System.out.println("Usage command line:HttpServer [option]");
                        System.out.println("-------------------------------------------");
                        System.out.println("    -p{int}     :   listen [P]ort,default is 80.");
                        System.out.println("    -t{int}     :   so_[T]imeout(ms),default is 5 minute.");
                        System.out.println("    -s{int}     :   thread pool [S]ize,default is 15 threads.");
                        System.out.println("    -z{int}     :   la[Z]y sleep times(ms),default is 10ms.");
                        System.out.println("    -l{on/off}  :   [L]og on/off,default is off.");
                        System.out.println("    -c{path}    :   [C]onfigure file path.");
                        System.out.println("    -h/?        :   [H]elp message like this");
                        System.out.println("-------------------------------------------");
                        System.out.println("Now try again !");
                        exit(0);
                        break;
                }
            }

            System.out.println("Jsonp service V1.6 启动,listening port : [" + PORT + "] in " + Util.now());
            logger.info("Jsonp service V1.6 启动,listening port : [" + PORT + "] in " + Util.now());
            new JsonpServer();
        } catch (Exception e) {
            logger.error("Error cause:" + e.getMessage());
        }
    }
}

class JProcess extends Thread {
    private static long requestNumber = 0;
    private static int errorNumber = 0;
    private boolean DEBUG = false;//调试标志，为true时后台显示调试信息
    private static int SO_TIMEOUT = 5 * 60 * 1000;//socket timeout 时间，单位毫秒
    private Socket client;// 客户Socket
    private static long startTime = System.currentTimeMillis();
    private String hostAddress = null;
    private int port = 0;
    private String cacheData = null;
    private UcsmyLog logger = new UcsmyLog(Process.class.getName());

    JProcess(Socket client, boolean debug) {
        this.client = client;
        this.DEBUG = debug;
    }

    @Override
    public void run() {
        //Socket client = null;// 客户Socket
        try {
            String traceid = Util.creatUUID();
            requestNumber++;
            //client = serverSocket.accept();// 客户机(这里是 IE 等浏览器)已经连接到当前服务器
            client.setKeepAlive(true);
            client.setSoTimeout(SO_TIMEOUT);
            this.hostAddress = client.getInetAddress().getHostAddress();
            this.port = client.getPort();
            logger.setTrace(traceid);
            long requestTime = System.currentTimeMillis();
            long responseTime = 0;

            if (client != null) {
                if (DEBUG) {
                    logger.info("[" + hostAddress + ":" + port + "]--Begin---" + Thread.currentThread().getName() + "-|-" + requestTime + "-----requestNumber:" + requestNumber);
                }
                //logger.debug("连接到服务器的用户:" + client);
                try {
                    // 第一阶段: 打开输入流
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    //logger.debug("客户端发送的请求信息: ***************");
                    // 读取第一行, 请求地址
                    //logger.debug("http协议头部信息：");
                    String line = in.readLine();
                    if (DEBUG)logger.debug("[" + port + "]" + line);

                    if (line != null) {
                        String resource = line.substring(line.indexOf('/'), line.lastIndexOf('/') - 5);
                        // 获得请求的资源的地址，如"/get/js?kdk=4"
                        //resource = URLDecoder.decode(resource, "utf-8");// 反编码

                        String method = new StringTokenizer(line).nextElement().toString();// 获取请求方法, GET 或者 POST

                        int contentLength = 0;
                        // 读取浏览器发送过来的请求参数头部信息
                        while ((line = in.readLine()) != null) {
                            if (DEBUG)logger.debug(line);
                            if (line.contains("Content-Length:")) {
                                contentLength = Integer.parseInt(line.substring("Content-Length:".length() + 1));
                            }
                            if ("".equals(line)) {
                                break;
                            }
                        }
                        if (DEBUG) {
                            logger.debug("[" + port + "]Content-Length:" + contentLength);
                        }

                        String params = null;

                        if (resource.contains("?")) {
                            params = resource.substring(resource.indexOf("?") + 1);
                            resource = resource.substring(0, resource.indexOf("?"));
                        }
                        if (params != null) {
                            if (params.length() == 0) {
                                params = null;
                            }
                        }
                        resource = resource.toLowerCase();

                        char[] p = new char[contentLength];
                        // 显示 POST 表单提交的内容, 这个内容位于请求的主体部分
                        if ("POST".equalsIgnoreCase(method)) {
                            in.read(p);
                            String postData = new String(p);
                            if (params != null) {
                                params += "&" + postData;
                            } else {
                                params = postData;
                            }
                        }

                        if (params != null) {
                            params = params.trim();
//                            logger.debug("["+port+"]Before decode:[" + params+"] length="+params.length());
//                            params = params.replace('+', '＋');
                            params = URLDecoder.decode(params, "utf-8");
//                            params = params.replace('＋', '+');
                            //已转移到Util.toProperties(params)处执行
//                            logger.debug("[" + port + "]after decode: " + params + " length=" + params.getBytes().length);
                        }

                        logger.debug("[" + hostAddress + ":" + port + "]" + method + " " + resource + "?" + params);

                        //cacheResponse=getCache(resource,params);

                        String jsonp = null;
                        APIResponse response = new APIResponse();
                        APIRequest request = new APIRequest();
                        request.setParameters(Util.toProperties(params));
                        request.setIp(hostAddress);
                        request.setPort(port);
                        request.setResoure(resource.substring(1));
                        request.setTraceid(traceid);

                        if (WhiteList.isWhiteList(request)) {
                            switch (resource) {
                                case "/login":
                                    jsonp = login(request);
                                    break;
                                case "/logout":
                                    jsonp = logout(request);
                                    break;
                                case "/vmapi":
                                    jsonp = vmApiRequest(request);
                                    break;
                                case "/auto":
                                    jsonp = autoRequest(request);
                                    break;
                                case "/report":
                                    jsonp = reportRequest(request);
                                    break;
                                case "/itil":
                                case "/getitildata":
                                case "/cmdb":
                                case "/getcmdbdata":
                                    jsonp = itilRequest(request);
                                    break;
                                case "/interface":
                                    jsonp = interfaceRequest(request);
                                    break;
                                case "/authtokentest":
                                    //request.setUsername(request.getParameters().getProperty("username"));
                                    //request.setToken(request.getParameters().getProperty("token"));
                                    if (authToken(request)) {
                                        response.setReturnCode("200");
                                        response.setReturnData("{\"auth\"=true}");
                                    } else {
                                        response.setReturnCode("200");
                                        response.setReturnData("{\"auth\"=false}");
                                    }
                                    jsonp = response.toString();
                                    break;
                                case "/setting":
                                    jsonp = httpServerSetting(request);
                                    break;
                                case "/reloadwhitelist":
                                    WhiteList.loadWhiteList();
                                    jsonp = "{\"msg\":\"Reload white lisst OK !\"}";
                                    break;

                                default:
                                    response.setReturnCode("404");
                                    response.setReturnData("\"File not found !\"");
                                    jsonp = response.toString();
                            }
                        } else {
                            response.setReturnCode("301");
                            response.setReturnData("\"Not allow to access !\"");
                            jsonp = response.toString();
                        }

                        //根据参数返回json格式的结果
                        responseString(jsonp, client);

                        // 关闭客户端链接
                        client.close();

                    } else {
                        logger.debug("[" + port + "]IO Error !");
                        errorNumber++;
                    }
                    responseTime = System.currentTimeMillis();
                    if (DEBUG) {
                        logger.debug("[" + port + "]--Over--error:" + errorNumber + "--|" + responseTime + "----Cost:" + (responseTime - requestTime) + "ms----" + (responseTime - startTime));
                    }
                } catch (Exception e) {
                    logger.error("[" + port + "]HTTP服务器错误:" + e.getMessage());
                    e.printStackTrace();
                    errorNumber++;
                    APIResponse errResponse = new APIResponse();
                    errResponse.setReturnCode("500");
                    errResponse.setReturnData("\"Service internal ERROR !\"");
                    responseString(errResponse.toString(), client);
                }
            }
            client.close();
        } catch (Exception e) {
            logger.error("[" + port + "]HTTP服务器错误:" + e.getMessage());
            errorNumber++;
            APIResponse errResponse = new APIResponse();
            errResponse.setReturnCode("500");
            errResponse.setReturnData("\"Service internal ERROR !\"");
            responseString(errResponse.toString(), client);
        }
    }

    private String vmApiRequest(APIRequest request) {
        return VmwareApi.access(request);
    }

    private String autoRequest(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        String retString = new Automate(connection, request.getPort(), request.getTraceid()).allocate(request);
        Util.safeClose(connection);
        return retString;
    }

    private String interfaceRequest(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        String retString = new CommonObject(connection, request.getPort()).interfaceRequest(request);
        Util.safeClose(connection);
        return retString;
    }

    private String reportRequest(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        String retString = new Report(connection, request.getPort()).report(request);
        Util.safeClose(connection);
        return retString;
    }

    private boolean authToken(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        boolean auth = new Authentication(connection).tokenAvailable(request.getUsername(), request.getToken(), request.getIp());
        Util.safeClose(connection);
        return auth;
    }

    private String itilRequest(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        String retString = new CommonObject(connection, request.getPort()).httpRequest(request);
        Util.safeClose(connection);

        return retString;
    }

    private String login(APIRequest request) {
        String account = request.getUsername();
        String psw = request.getParameters().getProperty("_password");
        Connection connection = ItilDataSource.newInstance().getConn();
        String retString = new Authentication(connection).login(account, psw, hostAddress);
        Util.safeClose(connection);
        APIResponse response = new APIResponse();
        if (retString != null) {
            response.setReturnCode("200");
            response.setReturnData("{\"auth\":true,\"msg\":\"Login success !\"");
            response.setToken(retString);
        } else {
            response.setReturnCode("200");
            response.setReturnData("{\"auth\":false,\"msg\":\"Login failed !\"}");
        }
        retString = response.toString();

        return retString;
    }

    private String logout(APIRequest request) {
        Connection connection = ItilDataSource.newInstance().getConn();
        boolean retValue = new Authentication(connection).logout(request.getUsername(), request.getToken(), request.getIp());
        Util.safeClose(connection);
        APIResponse response = new APIResponse();

        if (retValue) {
            response.setReturnCode("200");
            response.setReturnData("{\"auth\":false,\"msg\":\"Logout success !\"");
        } else {
            response.setReturnCode("200");
            response.setReturnData("{\"auth\":false,\"msg\":\"Logout failed !\"}");
        }

        return response.toString();
    }

    private String httpServerSetting(APIRequest params) {
        APIResponse response = new APIResponse();
        String cmd = params.getParameters().getProperty("cmd");
        String psw = params.getParameters().getProperty("authword");
        String newDebug = params.getParameters().getProperty("debug");
        String newTimeout = params.getParameters().getProperty("timeout");

        if (newDebug != null) {
            DEBUG = new Boolean(newDebug).booleanValue();
        }
        if (newTimeout != null) {
            SO_TIMEOUT = new Integer(newTimeout).intValue();
        }
        if (cmd != null && psw != null) {
            if (cmd.compareTo("shutdown") == 0 && psw.compareTo("itil2016") == 0) {
                NIOProcess.shutdown();
                exit(0);
            }
        }
        response.setReturnCode("200");
        response.setReturnData("Update setting success !");
        return response.toString();
    }

    public void responseString(String returnContent, Socket socket) {
        try {
            returnContent = returnContent.replaceAll("\t", "");
            returnContent = returnContent.replaceAll("\n", "");
            logger.debug("[" + port + "]Response:" + returnContent);
            PrintStream out = new PrintStream(socket.getOutputStream(), true);

            String contentType = "text/html;charset=UTF8";
            // 设置返回的内容类型

            // http 协议返回头
            out.println("HTTP/1.0 200 OK");// 返回应答消息,并结束应答
            out.println("Content-Type:" + contentType);
            out.println("Content-Length:" + (returnContent.getBytes().length));// 返回内容字节数
            out.println();// 根据 HTTP 协议, 空行将结束头信息
            out.println(returnContent);

            out.close();
        } catch (IOException e) {
            logger.error("[" + this.port + "]ERROR:" + e.getMessage());
        }
    }

    /**
     * 根据输入参数获取对应文件内容并返回给浏览器端.
     *
     * @param fileName 文件名
     * @param socket   客户端 socket.
     * @throws IOException
     */
    void fileReaderAndReturn(String fileName, Socket socket) throws IOException {
        if ("/".equals(fileName)) {// 设置欢迎页面，呵呵！
            fileName = "/index.html";
        }
        fileName = fileName.substring(1);
        fileName = "D:\\dev\\bg_service\\web\\" + fileName;

        PrintStream out = new PrintStream(socket.getOutputStream(), true);
        File fileToSend = new File(fileName);
        //logger.debug("文件本地路径为" + fileToSend.getPath());

        String fileEx = fileName.substring(fileName.indexOf(".") + 1);
        String contentType = null;
        // 设置返回的内容类型
        // 此处的类型与tomcat/conf/web.xml中配置的mime-mapping类型是一致的。测试之用，就写这么几个。
        if ("htmlhtmxml".indexOf(fileEx) > -1) {
            contentType = "text/html;charset=UTF8";
        } else if ("jpegjpggifbmppng".indexOf(fileEx) > -1) {
            contentType = "application/binary";
        }

        if (fileToSend.exists() && !fileToSend.isDirectory()) {
            // http 协议返回头
            out.println("HTTP/1.0 200 OK");// 返回应答消息,并结束应答
            out.println("Content-Type:" + contentType);
            out.println("Content-Length:" + fileToSend.length());// 返回内容字节数
            out.println();// 根据 HTTP 协议, 空行将结束头信息

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileToSend);

                byte[] data;
                data = new byte[fis.available()];
                fis.read(data);
                out.write(data);

            } catch (FileNotFoundException e) {
                out.println("<h1>404错误！</h1>" + e.getMessage());
            } catch (IOException e) {
                out.println("<h1>500错误!</h1>" + e.getMessage());
                e.printStackTrace();
            } finally {
                out.close();
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            out.println("<h1>404错误！</h1>" + "文件没有找到");
            out.close();
        }
    }
}
