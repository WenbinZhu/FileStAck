package rmi;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

/** RMI Skeleton's Listening Thread

    <p>
    Upon receiving a client request, this thread creates multiple
    client threads to communicate with the client stubs.
*/
public class ListenThread<T> implements Runnable {
    private Class<T> ci;
    private T server;
    private ServerSocket serverSocket;      // the socket waiting for client request
    private volatile boolean isCancelled;   // make the flag volatile to ensure thread safety

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

    /** Stop the thread and close the listening socket
     */
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
        // The socket to communicate with client stubs
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

                // Get method name, parameter types and args from client stub
                String methodName = (String) input.readObject();
                Class<?>[] paramTypes = (Class<?>[]) input.readObject();
                Object[] args = (Object[]) input.readObject();

                try {
                    Method invokedMethod = ci.getMethod(methodName, paramTypes);
                    resultObj = invokedMethod.invoke(server, args);
                }
                catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                       SecurityException | IllegalArgumentException e) {
                    // Transmit remote exceptions back to the client
                    resultObj = e;
                }

                output.writeObject(resultObj);
            }
            // This exception is caused by readObject()
            catch (ClassNotFoundException e) {
                try {
                    // Send exceptions back to
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
