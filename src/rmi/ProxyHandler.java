package rmi;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import static java.lang.reflect.Proxy.isProxyClass;

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
        if (method.equals(Object.class.getMethod("equals", Object.class))) {
            if (args[0] == null)
                return false;

            if (!isProxyClass(args[0].getClass()))
                return false;

            ProxyHandler ph = (ProxyHandler) Proxy.getInvocationHandler(args[0]);

            return ci.equals(ph.getClassInterface()) && sockAddr.equals(ph.getSockAddr());
        }

        if (method.equals(Object.class.getMethod("hashCode"))) {
            return (sockAddr.toString() + ci.toString()).hashCode();
        }

        if (method.equals(Object.class.getMethod("toString"))) {
            return "Class: " + ci + ", Address: " + sockAddr;
        }

        ObjectOutputStream output = null;
        ObjectInputStream input = null;
        Socket stubSocket = null;

        try {
            stubSocket = new Socket();
            stubSocket.connect(sockAddr);
            output = new ObjectOutputStream(stubSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(stubSocket.getInputStream());

            // Send serialized method name, parameter types and args to the client
            output.writeObject(method.getName());
            output.writeObject(method.getParameterTypes());
            output.writeObject(args);
            Object resultObj = input.readObject();

            // Remote exceptions are transmitted back to the client
            if (resultObj instanceof InvocationTargetException
                    || resultObj instanceof ClassNotFoundException || resultObj instanceof IllegalAccessException
                    || resultObj instanceof IllegalArgumentException || resultObj instanceof SecurityException)
                throw ((Exception) resultObj).getCause();

            if (resultObj instanceof NoSuchMethodException)
                throw new RMIException("No such method in interface");

            return resultObj;
        }
        catch (Exception e) {
            // Invoked methods may throw their own exceptions
            if (Arrays.asList(method.getExceptionTypes()).contains(e.getClass()))
                throw e;

            if (e instanceof IOException)
                throw new RMIException(e);

            throw e;
        }
        finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (stubSocket != null) stubSocket.close();
            }
            catch (IOException ioe) {
                // ioe.printStackTrace();
            }
        }
    }
}
