package rmi;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *  RMI Skeleton's ListenThread, creates multiple ClientThreads
 *  to communicate with the client stubs
 */
public class ListenThread<T> implements Runnable {
    private Class<T> ci;
    private T server;
    private ServerSocket serverSocket;
    private volatile boolean isCancelled;

    public ListenThread(Class<T> c, T server, ServerSocket serverSocket)
    {
        if (c == null || server == null || serverSocket == null)
            throw new NullPointerException();

        this.isCancelled = false;
        this.ci = c;
        this.server = server;
        this.serverSocket = serverSocket;
    }

    @Override
    public synchronized void run()
    {
        // isCancelled = false;
        try {
            while (!isCancelled) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientThread(clientSocket)).start();
            }
        }
        catch (IOException ioe) {
            // ioe.printStackTrace();
        }
    }

    public void cancel()
    {
        isCancelled = true;
        if (serverSocket != null && !serverSocket.isClosed())
            try {
                serverSocket.close();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }

    }


    private class ClientThread implements Runnable
    {
        private Socket clientSocket;

        public ClientThread(Socket clientSocket)
        {
            if (clientSocket == null)
                throw new NullPointerException();

            this.clientSocket = clientSocket;
        }

        @Override
        public void run()
        {
            ObjectInputStream input = null;
            ObjectOutputStream output = null;
            Object resultObj;

            try {
                if (clientSocket == null || clientSocket.isClosed())
                    return;

                input = new ObjectInputStream(clientSocket.getInputStream());
                output = new ObjectOutputStream(clientSocket.getOutputStream());


                String methodName = (String) input.readObject();
                Class<?>[] paramTypes = (Class<?>[]) input.readObject();
                Object[] args = (Object[]) input.readObject();

                try {
                    Method invokedMethod = ci.getMethod(methodName, paramTypes);
                    resultObj = invokedMethod.invoke(server, args);
                }
                catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                       SecurityException | IllegalArgumentException e) {
                    resultObj = e;
                }

                output.writeObject(resultObj);
            }
            catch (ClassNotFoundException e) {
                try {
                    output = new ObjectOutputStream(clientSocket.getOutputStream());
                    output.writeObject(e);
                }
                catch (IOException ioe) {
                    // ioe.printStackTrace();
                }
            }
            catch (IOException ioe) {
                // ioe.printStackTrace();
            }
            finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (clientSocket != null) clientSocket.close();
                }
                catch (IOException ioe) {
                    // ioe.printStackTrace();
                }
            }
        }
    }

}
