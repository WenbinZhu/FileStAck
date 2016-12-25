package rmi;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class ProxyHandler<T> implements InvocationHandler, Serializable
{
    private Class<T> ci;
    private InetSocketAddress sockAddr;

    public ProxyHandler(Class<T> c, InetSocketAddress sockAddr)
    {
        if (c == null || sockAddr == null)
            throw new NullPointerException();

        this.ci = c;
        this.sockAddr = sockAddr;
    }

    public Class<T> getClassInterface() {
        return ci;
    }

    public InetSocketAddress getSockAddr() {
        return sockAddr;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        switch (method.getName()) {
            case "equals": {
                if (args.length == 1 && args[0] == null)
                    return false;

                ProxyHandler ph = (ProxyHandler) Proxy.getInvocationHandler(args[0]);
                return ci.equals(ph.getClassInterface()) && sockAddr.equals(ph.getSockAddr());
            }

            case "hashCode": {
                return (sockAddr.toString() + ci.toString()).hashCode();
            }

            case "toString": {
                return "Class: " + ci + ", Address: " + sockAddr;
            }

            default: {
                try {
                    Socket stubSocket = new Socket();
                    stubSocket.connect(sockAddr);
                    ObjectOutputStream output = new ObjectOutputStream(stubSocket.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(stubSocket.getInputStream());

                    output.writeObject(method.getName());
                    output.writeObject(method.getParameterTypes());
                    output.writeObject(args);
                    Object resultObj = input.readObject();

                    // Remote exceptions are transmitted back to the client
                    if (resultObj instanceof Exception)
                        throw ((Exception) resultObj).getCause();

                    output.close();
                    input.close();
                    stubSocket.close();

                    return resultObj;
                }
                catch (Exception e) {
                    // Method may throw their own exceptions
                    if (Arrays.asList(method.getExceptionTypes()).contains(e.getClass()))
                        throw e;
                    // e.printStackTrace();
                    return null;
                }
            }
        }
    }
}
